package com.mangacinema.reader;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/** Single camera implementation used by the on-screen player AND MP4 export. */
public final class ScenePainter {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public void draw(Canvas canvas, Bitmap image, float alpha, float progress) {
        if (image == null || image.isRecycled() || alpha <= 0f) return;
        final float width = canvas.getWidth(), height = canvas.getHeight();
        alpha = clamp(alpha);
        progress = clamp(progress);
        float cover = Math.max(width / image.getWidth(), height / image.getHeight());
        float backWidth = image.getWidth() * cover;
        float backHeight = image.getHeight() * cover;
        paint.setColor(Color.WHITE);
        paint.setAlpha(Math.round(alpha * 70));
        canvas.drawBitmap(image, null, new RectF((width - backWidth) / 2f,
                (height - backHeight) / 2f, (width + backWidth) / 2f,
                (height + backHeight) / 2f), paint);
        float scale = width / image.getWidth() * (1f + 0.032f * progress);
        float drawWidth = image.getWidth() * scale;
        float drawHeight = image.getHeight() * scale;
        float x = (width - drawWidth) / 2f;
        float y = drawHeight > height ? -(drawHeight - height) * progress
                : (height - drawHeight) / 2f;
        paint.setAlpha(Math.round(alpha * 255));
        canvas.drawBitmap(image, null, new RectF(x, y, x + drawWidth, y + drawHeight), paint);
        paint.setAlpha(255);
    }

    public void frame(Canvas canvas, Bitmap previous, Bitmap current,
                      float dissolve, float motion) {
        canvas.drawColor(Color.rgb(7, 9, 15));
        if (previous != null && dissolve < 1f) draw(canvas, previous, 1f - dissolve, 1f);
        draw(canvas, current, dissolve, motion);
    }

    public static float clamp(float p) { return Math.max(0f, Math.min(1f, p)); }
}
