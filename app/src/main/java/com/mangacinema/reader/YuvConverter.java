package com.mangacinema.reader;

/** Pure Java converter for portable unit tests and MediaCodec input. */
public final class YuvConverter {
    private YuvConverter() { }

    /** Converts ARGB pixels to encoder input I420 or NV12 (studio-range BT.601). */
    public static void rgbToYuv420(int[] rgb, byte[] out, int w, int h, boolean nv12) {
        if ((w & 1) != 0 || (h & 1) != 0 || rgb.length < w * h ||
                out.length < w * h * 3 / 2) throw new IllegalArgumentException("Invalid YUV frame");
        final int luma = w * h;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = rgb[y * w + x];
                int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                out[y * w + x] = (byte) bound(((66 * r + 129 * g + 25 * b + 128) >> 8) + 16);
                if ((y & 1) == 0 && (x & 1) == 0) {
                    int u = bound(((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128);
                    int v = bound(((112 * r - 94 * g - 18 * b + 128) >> 8) + 128);
                    int cIndex = (y / 2) * (w / 2) + x / 2;
                    if (nv12) {
                        out[luma + cIndex * 2] = (byte) u;
                        out[luma + cIndex * 2 + 1] = (byte) v;
                    } else {
                        out[luma + cIndex] = (byte) u;
                        out[luma + luma / 4 + cIndex] = (byte) v;
                    }
                }
            }
        }
    }

    private static int bound(int n) { return Math.max(0, Math.min(255, n)); }
}
