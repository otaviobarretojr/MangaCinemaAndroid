package com.mangacinema.reader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.provider.DocumentsContract;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Offline cinematic manga player with on-device MP4 export. */
public final class MainActivity extends Activity {
    private static final int OPEN_PDF = 100;
    private static final int SAVE_VIDEO = 101;
    private final ExecutorService pdfThread = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final ArrayList<SceneDetector.Scene> scenes = new ArrayList<>();
    private final float[] speeds = {0.75f, 1.0f, 1.25f, 1.5f};
    private int speedIndex = 1;
    private volatile int pdfRequest = 0;
    private volatile int sceneRequest = 0;
    private int currentIndex = -1;
    private float fraction = 0f;
    private boolean playing = true;
    private volatile boolean closing = false;
    private volatile boolean exporting = false;
    private final AtomicBoolean cancelExport = new AtomicBoolean(false);
    private boolean highQuality = false;
    private PdfRenderer renderer;
    private ParcelFileDescriptor descriptor;
    private Uri currentUri;
    private ValueAnimator progressAnim;
    private ValueAnimator fadeAnim;
    private SceneView viewer;
    private TextView headline;
    private TextView info;
    private Button playButton;
    private Button speedButton;
    private Button exportButton;
    private Button qualityButton;
    private Button importButton;
    private Button previousButton;
    private Button nextButton;
    private ProgressBar progressBar;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(7, 9, 15));
        getWindow().setNavigationBarColor(Color.rgb(7, 9, 15));
        buildUi();
        String saved = getPreferences(MODE_PRIVATE).getString("uri", null);
        if (saved != null) openPdf(Uri.parse(saved));
    }

    private void buildUi() {
        final int background = Color.rgb(7, 9, 15);
        final int accent = Color.rgb(89, 136, 241);
        final int soft = Color.rgb(21, 27, 40);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(background);
        root.setPadding(dp(9), dp(6), dp(9), dp(11));
        setContentView(root);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        headline = new TextView(this);
        headline.setText("MANGÁ CINEMA");
        headline.setTextSize(19);
        headline.setTypeface(null, 1);
        headline.setTextColor(Color.WHITE);
        bar.addView(headline, new LinearLayout.LayoutParams(0, dp(56), 1));
        headline.setGravity(Gravity.CENTER_VERTICAL);
        importButton = button("Importar PDF", accent);
        importButton.setOnClickListener(v -> choosePdf());
        bar.addView(importButton, new LinearLayout.LayoutParams(dp(140), dp(52)));
        root.addView(bar);

        viewer = new SceneView(this);
        root.addView(viewer, new LinearLayout.LayoutParams(-1, 0, 1));
        viewer.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
            if (scenes.isEmpty() || exporting) return true;
            float x = event.getX() / Math.max(1, v.getWidth());
            if (x < .23f) jump(-1);
            else if (x > .77f) jump(1);
            else togglePlay();
            v.performClick();
            return true;
        });

        info = new TextView(this);
        info.setText("Abra um mangá em PDF para começar");
        info.setTextColor(Color.rgb(183, 193, 215));
        info.setTextSize(13);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, dp(12), 0, dp(10));
        root.addView(info, new LinearLayout.LayoutParams(-1, -2));
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(1000);
        progressBar.setProgressTintList(ColorStateList.valueOf(accent));
        progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(soft));
        root.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(4)));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(0, dp(9), 0, 0);
        previousButton = button("◀", soft);
        previousButton.setOnClickListener(v -> jump(-1));
        controls.addView(previousButton, new LinearLayout.LayoutParams(0, dp(52), 0.8f));
        playButton = button("Pausar", accent);
        playButton.setOnClickListener(v -> togglePlay());
        controls.addView(playButton, new LinearLayout.LayoutParams(0, dp(52), 1.35f));
        nextButton = button("▶", soft);
        nextButton.setOnClickListener(v -> jump(1));
        controls.addView(nextButton, new LinearLayout.LayoutParams(0, dp(52), .8f));
        speedButton = button("1×", soft);
        speedButton.setOnClickListener(v -> changeSpeed());
        controls.addView(speedButton, new LinearLayout.LayoutParams(0, dp(52), 1f));
        root.addView(controls);

        LinearLayout videoTools = new LinearLayout(this);
        videoTools.setGravity(Gravity.CENTER_VERTICAL);
        videoTools.setPadding(0, dp(8), 0, 0);
        exportButton = button("Exportar MP4", accent);
        exportButton.setOnClickListener(v -> {
            if (exporting) cancelVideo();
            else chooseVideoDestination();
        });
        videoTools.addView(exportButton, new LinearLayout.LayoutParams(0, dp(48), 1.7f));
        qualityButton = button("Rápido 480p", soft);
        qualityButton.setOnClickListener(v -> {
            if (exporting) return;
            highQuality = !highQuality;
            qualityButton.setText(highQuality ? "HD 720p" : "Rápido 480p");
        });
        videoTools.addView(qualityButton, new LinearLayout.LayoutParams(0, dp(48), 1.2f));
        root.addView(videoTools);
        updatePlayButton();
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }

    private Button button(String text, int color) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13f);
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setBackgroundTintList(ColorStateList.valueOf(color));
        return b;
    }

    private void choosePdf() {
        if (exporting) return;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("application/pdf");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, OPEN_PDF);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (request == SAVE_VIDEO) {
            beginVideoExport(data.getData());
            return;
        }
        if (request != OPEN_PDF || exporting) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) { }
        getPreferences(MODE_PRIVATE).edit().putString("uri", uri.toString()).apply();
        playing = true;
        updatePlayButton();
        openPdf(uri);
    }

    private void openPdf(Uri uri) {
        if (exporting) return;
        currentUri = uri;
        stopTimers();
        ++sceneRequest;
        int request = ++pdfRequest;
        currentIndex = -1;
        scenes.clear();
        info.setText("Analisando cenas do PDF…");
        progressBar.setProgress(0);
        viewer.setMotion(0);
        pdfThread.execute(() -> {
            try {
                closeReader();
                descriptor = getContentResolver().openFileDescriptor(uri, "r");
                if (descriptor == null) throw new IllegalStateException("Não foi possível abrir o arquivo");
                renderer = new PdfRenderer(descriptor);
                ArrayList<SceneDetector.Scene> detected = new ArrayList<>();
                int pages = renderer.getPageCount();
                for (int p = 0; p < pages; p++) {
                    if (request != pdfRequest) return;
                    try (PdfRenderer.Page page = renderer.openPage(p)) {
                        float width = page.getWidth(), height = page.getHeight();
                        float scale = Math.min(0.26f, 190f / width);
                        int w = Math.max(1, Math.round(width * scale));
                        int h = Math.max(1, Math.round(height * scale));
                        Bitmap preview = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                        preview.eraseColor(Color.WHITE);
                        Matrix matrix = new Matrix();
                        matrix.setScale(w / width, h / height);
                        page.render(preview, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        int[] pixels = new int[w * h];
                        preview.getPixels(pixels, 0, w, 0, 0, w, h);
                        preview.recycle();
                        detected.addAll(SceneDetector.detect(p, width, height, w, h, pixels));
                    }
                }
                mainThread.post(() -> {
                    if (request != pdfRequest || closing) return;
                    scenes.addAll(detected);
                    if (scenes.isEmpty()) { info.setText("Não foi possível detectar cenas"); return; }
                    headline.setText("MANGÁ CINEMA · " + pages + " pág.");
                    currentIndex = 0;
                    displayScene(0);
                });
            } catch (Exception e) {
                mainThread.post(() -> {
                    if (request != pdfRequest || closing) return;
                    info.setText("Falha na importação: " + e.getMessage());
                    Toast.makeText(this, "Verifique se o PDF não está protegido ou corrompido", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void closeReader() {
        if (renderer != null) { renderer.close(); renderer = null; }
        if (descriptor != null) {
            try { descriptor.close(); } catch (Exception ignored) { }
            descriptor = null;
        }
    }

    private void displayScene(int index) {
        if (exporting) return;
        if (index < 0 || index >= scenes.size()) {
            if (index >= scenes.size()) {
                pausePlayback();
                info.setText("Fim do capítulo • " + scenes.size() + " cenas");
            }
            return;
        }
        stopTimers();
        currentIndex = index;
        fraction = 0f;
        final SceneDetector.Scene scene = scenes.get(index);
        info.setText("Carregando cena " + (index + 1) + " de " + scenes.size() + "…");
        progressBar.setProgress(0);
        int request = ++sceneRequest;
        pdfThread.execute(() -> {
            Bitmap bitmap = null;
            try {
                if (renderer != null) {
                    int width = Math.max(720, Math.min(1100, getResources().getDisplayMetrics().widthPixels * 2));
                    bitmap = PdfSceneRenderer.render(renderer, scene, width);
                }
                Bitmap result = bitmap;
                mainThread.post(() -> {
                    if (request != sceneRequest || closing) {
                        if (result != null) result.recycle();
                        return;
                    }
                    if (result == null) { info.setText("Cena indisponível"); return; }
                    viewer.setScene(result);
                    info.setText(String.format(Locale.getDefault(), "Cena %d / %d  •  Página %d",
                            index + 1, scenes.size(), scene.page + 1));
                    fadeAnim = ValueAnimator.ofFloat(0, 1);
                    fadeAnim.setDuration(SceneTiming.FADE_MS);
                    fadeAnim.addUpdateListener(a -> viewer.setDissolve((float) a.getAnimatedValue()));
                    fadeAnim.addListener(new AnimatorListenerAdapter() {
                        private boolean cancelled = false;
                        @Override public void onAnimationCancel(Animator a) { cancelled = true; }
                        @Override public void onAnimationEnd(Animator a) {
                            if (!cancelled && request == sceneRequest && playing && !closing) startProgress();
                        }
                    });
                    fadeAnim.start();
                });
            } catch (Exception e) {
                if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
                mainThread.post(() -> { if (request == sceneRequest) info.setText("Erro na cena: " + e.getMessage()); });
            }
        });
    }

    private void startProgress() {
        if (!playing || currentIndex < 0 || currentIndex >= scenes.size()) return;
        if (progressAnim != null) progressAnim.cancel();
        SceneDetector.Scene scene = scenes.get(currentIndex);
        long duration = Math.max(220L, Math.round(SceneTiming.motionMillis(scene,
                speeds[speedIndex]) * (1f - fraction)));
        final int expected = sceneRequest;
        progressAnim = ValueAnimator.ofFloat(fraction, 1f);
        progressAnim.setDuration(duration);
        progressAnim.addUpdateListener(a -> {
            fraction = (float) a.getAnimatedValue();
            viewer.setMotion(fraction);
            progressBar.setProgress(Math.round(fraction * 1000));
        });
        progressAnim.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled = false;
            @Override public void onAnimationCancel(Animator a) { cancelled = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancelled && playing && expected == sceneRequest) displayScene(currentIndex + 1);
            }
        });
        progressAnim.start();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void stopTimers() {
        if (progressAnim != null) { progressAnim.cancel(); progressAnim = null; }
        if (fadeAnim != null) { fadeAnim.cancel(); fadeAnim = null; }
    }

    private void jump(int offset) {
        if (exporting) return;
        if (!scenes.isEmpty()) displayScene(Math.max(0, Math.min(scenes.size() - 1, currentIndex + offset)));
    }

    private void togglePlay() {
        if (exporting) return;
        if (playing) pausePlayback();
        else {
            playing = true;
            updatePlayButton();
            if (currentIndex >= 0) startProgress();
        }
    }

    private void pausePlayback() {
        playing = false;
        if (progressAnim != null) { progressAnim.cancel(); progressAnim = null; }
        updatePlayButton();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void updatePlayButton() {
        if (playButton != null) playButton.setText(playing ? "Pausar" : "Reproduzir");
    }

    private void changeSpeed() {
        if (exporting) return;
        speedIndex = (speedIndex + 1) % speeds.length;
        speedButton.setText(String.format(Locale.US, "%.2g×", speeds[speedIndex]));
        if (playing && currentIndex >= 0) startProgress();
    }

    private void chooseVideoDestination() {
        if (exporting || scenes.isEmpty()) {
            Toast.makeText(this, "Importe e analise um PDF primeiro", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/mp4");
        intent.putExtra(Intent.EXTRA_TITLE,
                "MangaCinema_" + System.currentTimeMillis() + ".mp4");
        startActivityForResult(intent, SAVE_VIDEO);
    }

    private void beginVideoExport(Uri destination) {
        if (exporting || scenes.isEmpty() || renderer == null) return;
        pausePlayback();
        stopTimers();
        ++sceneRequest;
        exporting = true;
        cancelExport.set(false);
        final ArrayList<SceneDetector.Scene> chosenScenes = new ArrayList<>(scenes);
        final Mp4Exporter.Preset preset = highQuality ? Mp4Exporter.Preset.HD : Mp4Exporter.Preset.FAST;
        final float speed = speeds[speedIndex];
        setControlsEnabled(false);
        exportButton.setEnabled(true);
        exportButton.setText("Cancelar MP4");
        progressBar.setProgress(0);
        info.setText("Preparando vídeo — mantenha o app aberto…");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        pdfThread.execute(() -> {
            File temp = null;
            String result = null;
            boolean success = false;
            try {
                if (renderer == null) throw new IOException("PDF não está disponível");
                temp = File.createTempFile("mangacinema_", ".mp4", getCacheDir());
                new Mp4Exporter(cancelExport, (done, total) -> mainThread.post(() -> {
                    if (exporting && !cancelExport.get() && !closing) {
                        progressBar.setProgress(Math.round(done * 1000f / Math.max(1, total)));
                        info.setText(String.format(Locale.getDefault(),
                                "Exportando MP4 • %d%% • mantenha o app aberto",
                                Math.round(done * 100f / Math.max(1, total))));
                    }
                })).export(renderer, chosenScenes, speed, preset, temp);
                if (cancelExport.get()) throw new CancellationException();
                try (InputStream input = new FileInputStream(temp);
                     OutputStream output = getContentResolver().openOutputStream(destination, "w")) {
                    if (output == null) throw new IOException("Não foi possível salvar o MP4");
                    byte[] bytes = new byte[128 * 1024];
                    int n;
                    while ((n = input.read(bytes)) != -1) {
                        if (cancelExport.get()) throw new CancellationException();
                        output.write(bytes, 0, n);
                    }
                    output.flush();
                }
                if (cancelExport.get()) throw new CancellationException();
                result = "MP4 salvo com sucesso!";
                success = true;
            } catch (CancellationException ex) {
                result = "Exportação cancelada";
            } catch (Exception ex) {
                result = "Erro ao exportar MP4: " + ex.getMessage();
            } finally {
                if (temp != null && temp.exists()) temp.delete();
                if (!success) {
                    // This document was created by our save request. Best-effort cleanup
                    // avoids leaving empty or incomplete videos in the user's folder.
                    try { DocumentsContract.deleteDocument(getContentResolver(), destination); }
                    catch (Exception ignored) { }
                }
                final String message = result;
                final boolean ok = success;
                mainThread.post(() -> {
                    if (closing) return;
                    exporting = false;
                    setControlsEnabled(true);
                    exportButton.setText("Exportar MP4");
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    info.setText(message);
                    progressBar.setProgress(ok ? 1000 : 0);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void cancelVideo() {
        if (!exporting) return;
        cancelExport.set(true);
        exportButton.setEnabled(false);
        info.setText("Cancelando exportação…");
    }

    private void setControlsEnabled(boolean enabled) {
        importButton.setEnabled(enabled);
        previousButton.setEnabled(enabled);
        nextButton.setEnabled(enabled);
        playButton.setEnabled(enabled);
        speedButton.setEnabled(enabled);
        qualityButton.setEnabled(enabled);
        exportButton.setEnabled(enabled);
    }

    @Override protected void onPause() {
        super.onPause();
        if (!exporting) pausePlayback();
    }

    @Override protected void onDestroy() {
        closing = true;
        cancelExport.set(true);
        ++pdfRequest;
        ++sceneRequest;
        stopTimers();
        viewer.release();
        pdfThread.execute(this::closeReader);
        pdfThread.shutdown();
        super.onDestroy();
    }
}
