package com.mangacinema.reader;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Offline, silent H.264/MP4 exporter using Android's built-in hardware encoder.
 * A temporary MP4 is written in private cache; the caller copies it to the SAF URI.
 * No ffmpeg binary, internet access, or external storage permissions are needed.
 * Encoding is CPU-heavy: do not call this class on the UI thread.
 */
public final class Mp4Exporter {
    public interface Progress { void onProgress(int completedFrames, int estimatedFrames); }

    public static final class Preset {
        public final int width, height, fps, bitrate;
        private Preset(int width, int height, int fps, int bitrate) {
            this.width = width; this.height = height; this.fps = fps; this.bitrate = bitrate;
        }
        public static final Preset FAST = new Preset(480, 848, 12, 1_800_000);
        public static final Preset HD = new Preset(720, 1280, 15, 4_000_000);
    }

    private static final String MIME = MediaFormat.MIMETYPE_VIDEO_AVC;
    private final AtomicBoolean cancel;
    private final Progress progress;
    private MediaCodec encoder;
    private MediaMuxer muxer;
    private int track = -1;
    private boolean muxerStarted;
    private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    public Mp4Exporter(AtomicBoolean cancel, Progress progress) {
        this.cancel = cancel;
        this.progress = progress;
    }

    public void export(PdfRenderer pdf, List<SceneDetector.Scene> scenes, float speed,
                       Preset preset, File destination) throws Exception {
        if (pdf == null || scenes == null || scenes.isEmpty())
            throw new IllegalArgumentException("Nenhuma cena carregada");
        if (destination == null || preset == null) throw new IllegalArgumentException("Destino ausente");
        if (!Float.isFinite(speed) || speed <= 0) throw new IllegalArgumentException("Velocidade inválida");
        int completed = 0;
        int estimated = 0;
        for (int i = 0; i < scenes.size(); i++)
            estimated += SceneTiming.frames(SceneTiming.motionMillis(scenes.get(i), speed),
                    preset.fps, i > 0);

        Bitmap output = null, previous = null, current = null;
        try {
            configureEncoder(preset, destination);
            output = Bitmap.createBitmap(preset.width, preset.height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            ScenePainter painter = new ScenePainter();
            int[] rgb = new int[preset.width * preset.height];
            byte[] yuv = new byte[rgb.length * 3 / 2];
            final int total = estimated;
            for (int i = 0; i < scenes.size(); i++) {
                checkCancelled();
                // Previous scene is retained for a real dissolve in the first 440 ms.
                current = PdfSceneRenderer.render(pdf, scenes.get(i), preset.width);
                long motionMs = SceneTiming.motionMillis(scenes.get(i), speed);
                int count = SceneTiming.frames(motionMs, preset.fps, previous != null);
                double millisPerFrame = (double) (motionMs + (previous != null ? SceneTiming.FADE_MS : 0)) / count;
                for (int frame = 0; frame < count; frame++) {
                    checkCancelled();
                    double time = frame * millisPerFrame;
                    float dissolve = previous == null ? 1f :
                            ScenePainter.clamp((float) (time / SceneTiming.FADE_MS));
                    float motion = ScenePainter.clamp((float) ((time -
                            (previous == null ? 0 : SceneTiming.FADE_MS)) / motionMs));
                    painter.frame(canvas, previous, current, dissolve, motion);
                    output.getPixels(rgb, 0, preset.width, 0, 0, preset.width, preset.height);
                    YuvConverter.rgbToYuv420(rgb, yuv, preset.width, preset.height,
                            encoderSemiPlanar);
                    queueFrame(yuv, completed * 1_000_000L / preset.fps);
                    completed++;
                    drain(false);
                    if ((completed % 16) == 0 || completed == total)
                        progress.onProgress(completed, total);
                }
                if (previous != null) { previous.recycle(); previous = null; }
                previous = current;
                current = null;
            }
            // Send a proper end-of-stream marker. Do not cut off buffered frames.
            queueEos(completed * 1_000_000L / preset.fps);
            drain(true);
            if (!muxerStarted) throw new IOException("Codec não produziu vídeo MP4");
            progress.onProgress(total, total);
        } finally {
            if (current != null) current.recycle();
            if (previous != null) previous.recycle();
            if (output != null) output.recycle();
            releaseEncoder();
        }
    }

    private boolean encoderSemiPlanar;
    private void configureEncoder(Preset preset, File dest) throws IOException {
        MediaCodecInfo chosen = null;
        int chosenColor = -1;
        // Try standard NV12 first, then standard planar YUV420. Both are available
        // on many Android H.264 encoders, including Samsung; unsupported devices
        // get a clear error rather than an unplayable MP4.
        for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
            if (!info.isEncoder()) continue;
            for (String type : info.getSupportedTypes()) {
                if (!MIME.equalsIgnoreCase(type)) continue;
                for (int color : info.getCapabilitiesForType(type).colorFormats) {
                    if (color == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                            || color == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
                        if (chosen == null || (color ==
                                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar &&
                                chosenColor != color)) {
                            chosen = info;
                            chosenColor = color;
                        }
                    }
                }
            }
            if (chosen != null && chosenColor ==
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) break;
        }
        if (chosen == null) throw new IOException("Nenhum encoder H.264 YUV420 compatível neste aparelho");
        encoderSemiPlanar = chosenColor ==
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        MediaFormat format = MediaFormat.createVideoFormat(MIME, preset.width, preset.height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, chosenColor);
        format.setInteger(MediaFormat.KEY_BIT_RATE, preset.bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, preset.fps);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
        try {
            encoder = MediaCodec.createByCodecName(chosen.getName());
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
            muxer = new MediaMuxer(dest.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {
            releaseEncoder();
            throw new IOException("Falha ao configurar codificador H.264: " + e.getMessage(), e);
        }
    }

    private void queueFrame(byte[] yuv, long pts) throws Exception {
        int index = awaitInput();
        ByteBuffer input = encoder.getInputBuffer(index);
        if (input == null || input.capacity() < yuv.length)
            throw new IOException("Buffer YUV incompatível com o encoder");
        input.clear();
        input.put(yuv);
        encoder.queueInputBuffer(index, 0, yuv.length, pts, 0);
    }

    private void queueEos(long pts) throws Exception {
        int index = awaitInput();
        encoder.queueInputBuffer(index, 0, 0, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
    }

    private int awaitInput() throws Exception {
        for (int tries = 0; tries < 1000; tries++) {
            checkCancelled();
            int index = encoder.dequeueInputBuffer(10000);
            if (index >= 0) return index;
            drain(false);
        }
        throw new IOException("Encoder não liberou buffer de entrada");
    }

    /** @return true when encoder EOS is observed. */
    private boolean drain(boolean end) throws Exception {
        int attemptsWithoutOutput = 0;
        for (;;) {
            checkCancelled();
            int index = encoder.dequeueOutputBuffer(bufferInfo, end ? 15000 : 0);
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!end) return false;
                if (++attemptsWithoutOutput > 2000)
                    throw new IOException("Tempo limite aguardando encerramento do vídeo");
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxerStarted) throw new IOException("Formato do codec mudou duas vezes");
                track = muxer.addTrack(encoder.getOutputFormat());
                muxer.start();
                muxerStarted = true;
            } else if (index >= 0) {
                ByteBuffer encoded = encoder.getOutputBuffer(index);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)
                    bufferInfo.size = 0;
                if (bufferInfo.size > 0) {
                    if (!muxerStarted || encoded == null)
                        throw new IOException("Dados H.264 antes do cabeçalho MP4");
                    encoded.position(bufferInfo.offset);
                    encoded.limit(bufferInfo.offset + bufferInfo.size);
                    muxer.writeSampleData(track, encoded, bufferInfo);
                }
                boolean eos = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                encoder.releaseOutputBuffer(index, false);
                if (eos) return true;
                attemptsWithoutOutput = 0;
            }
        }
    }

    private void releaseEncoder() {
        if (encoder != null) {
            try { encoder.stop(); } catch (Exception ignored) { }
            try { encoder.release(); } catch (Exception ignored) { }
            encoder = null;
        }
        if (muxer != null) {
            if (muxerStarted) { try { muxer.stop(); } catch (Exception ignored) { } }
            try { muxer.release(); } catch (Exception ignored) { }
            muxer = null;
        }
        track = -1;
        muxerStarted = false;
    }

    private void checkCancelled() {
        if (cancel.get() || Thread.currentThread().isInterrupted())
            throw new CancellationException("Exportação cancelada");
    }


}
