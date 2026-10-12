package com.mangacinema.reader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Displays exactly the same camera path used by the MP4 encoder. */
public final class SceneView extends View {
    private final ScenePainter painter = new ScenePainter();
    private Bitmap current;
    private Bitmap previous;
    private float motion = 0f;
    private float dissolve = 1f;

    public SceneView(Context context) { super(context); setBackgroundColor(Color.rgb(7, 9, 15)); }

    public void setScene(Bitmap bitmap) {
        if (previous != null && previous != current && !previous.isRecycled()) previous.recycle();
        previous = current;
        current = bitmap;
        motion = 0f;
        dissolve = previous == null ? 1f : 0f;
        invalidate();
    }

    public void setMotion(float f) { motion = ScenePainter.clamp(f); invalidate(); }
    public void setDissolve(float f) {
        dissolve = ScenePainter.clamp(f);
        if (dissolve >= 1f && previous != null) {
            if (previous != current && !previous.isRecycled()) previous.recycle();
            previous = null;
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        painter.frame(canvas, previous, current, dissolve, motion);
        if (current == null) {
            Paint message = new Paint(Paint.ANTI_ALIAS_FLAG);
            message.setColor(Color.rgb(112, 124, 146));
            message.setTextSize(getWidth() / 19f);
            message.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Importe seu PDF para começar", getWidth() / 2f,
                    getHeight() / 2f, message);
        }
    }

    public void release() {
        if (current != null && !current.isRecycled()) current.recycle();
        if (previous != null && previous != current && !previous.isRecycled()) previous.recycle();
        current = null;
        previous = null;
    }
}
