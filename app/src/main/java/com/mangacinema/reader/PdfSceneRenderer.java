package com.mangacinema.reader;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;

/** Crops in PDF coordinates; keeps the high-resolution original outside the APK. */
public final class PdfSceneRenderer {
    private PdfSceneRenderer() { }

    public static Bitmap render(PdfRenderer renderer, SceneDetector.Scene scene, int targetWidth) {
        if (renderer == null || scene == null) throw new IllegalArgumentException("Missing PDF or scene");
        try (PdfRenderer.Page page = renderer.openPage(scene.page)) {
            float width = page.getWidth();
            float span = scene.endY - scene.startY;
            if (width <= 0 || span <= 0) throw new IllegalArgumentException("Invalid scene coordinates");
            int bitmapWidth = Math.max(240, targetWidth);
            float scale = bitmapWidth / width;
            int bitmapHeight = Math.max(1, Math.round(span * scale));
            // Do not allocate gigantic bitmaps from unexpectedly oversized pages.
            if (bitmapHeight > 2400) {
                bitmapWidth = Math.max(240, Math.round(bitmapWidth * 2400f / bitmapHeight));
                scale = bitmapWidth / width;
                bitmapHeight = Math.max(1, Math.round(span * scale));
            }
            Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
            try {
                bitmap.eraseColor(Color.WHITE);
                Matrix matrix = new Matrix();
                matrix.setValues(new float[] {scale, 0f, 0f, 0f, scale,
                        -scene.startY * scale, 0f, 0f, 1f});
                page.render(bitmap, new Rect(0, 0, bitmapWidth, bitmapHeight), matrix,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                return bitmap;
            } catch (RuntimeException e) {
                bitmap.recycle();
                throw e;
            }
        }
    }
}
