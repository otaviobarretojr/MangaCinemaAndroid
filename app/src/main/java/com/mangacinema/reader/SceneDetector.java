package com.mangacinema.reader;

import java.util.ArrayList;
import java.util.List;

/** Scene proposals based on white horizontal gutters plus bounded vertical crops.
 * Pure Java, intentionally does not pretend to recognize narrative panels or dialogue.
 */
public final class SceneDetector {
    private SceneDetector() { }

    public static final class Scene {
        public final int page;
        public final float startY;
        public final float endY;
        Scene(int page, float startY, float endY) {
            this.page = page;
            this.startY = startY;
            this.endY = endY;
        }
        @Override public String toString() {
            return "page=" + (page + 1) + " y=" + (int) startY + ".." + (int) endY;
        }
    }

    /** argb pixels in row-major order, sampled from a low-resolution PdfRenderer bitmap. */
    public static List<Scene> detect(int page, float width, float height,
                                     int imageWidth, int imageHeight, int[] argb) {
        if (width <= 0 || height <= 0 || imageWidth < 1 || imageHeight < 1
                || argb == null || argb.length != imageWidth * imageHeight) {
            throw new IllegalArgumentException("Invalid page preview");
        }
        List<Scene> scenes = new ArrayList<>();
        if (page == 0 && height / width < 1.5f) {
            scenes.add(new Scene(page, 0, height));
            return scenes;
        }
        boolean[] gutter = new boolean[imageHeight];
        for (int y = 0; y < imageHeight; y++) {
            int hits = 0;
            int total = 0;
            for (int x = 0; x < imageWidth; x += 3) {
                int rgb = argb[y * imageWidth + x];
                // Treat only almost-white as a blank gutter; bright art is not empty.
                if (((rgb >> 16) & 255) >= 245 && ((rgb >> 8) & 255) >= 245
                        && (rgb & 255) >= 245) hits++;
                total++;
            }
            gutter[y] = total > 0 && hits >= Math.ceil(total * 0.965);
        }
        float pyToPdf = height / imageHeight;
        int minGap = Math.max(7, (int) Math.ceil(width * 0.07f / pyToPdf));
        float start = 0;
        for (int y = 0; y < imageHeight;) {
            if (!gutter[y]) { y++; continue; }
            int begin = y;
            while (y < imageHeight && gutter[y]) y++;
            int end = y;
            if (end - begin < minGap) continue;
            float before = begin * pyToPdf;
            float after = end * pyToPdf;
            if (before - start >= width * 0.24f) appendSplit(scenes, page, start, before, width);
            start = after; // Explicitly skip white gutter rather than treating it as a scene.
        }
        if (height - start >= width * 0.18f) appendSplit(scenes, page, start, height, width);
        if (scenes.isEmpty()) scenes.add(new Scene(page, 0, height));
        return scenes;
    }

    private static void appendSplit(List<Scene> out, int page, float start,
                                    float end, float pageWidth) {
        float max = pageWidth * 1.60f;
        float minimum = pageWidth * 0.25f;
        float cursor = start;
        int watchdog = 0;
        while (cursor < end - 1 && ++watchdog < 500) {
            float remaining = end - cursor;
            float stop = Math.min(end, cursor + max);
            // Don't create a tiny tail after a large crop; distribute height across crops.
            if (end - stop > 0 && end - stop < minimum) stop = (cursor + end) / 2f;
            if (stop - cursor < 3) break;
            out.add(new Scene(page, cursor, stop));
            // Keep a narrow overlap at arbitrary split points so a speech bubble is less
            // likely to disappear between two scenes (not a true balloon detector).
            cursor = stop >= end ? end : stop - pageWidth * 0.025f;
        }
    }
}
