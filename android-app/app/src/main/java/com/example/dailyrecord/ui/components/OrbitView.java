package com.example.dailyrecord.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Original vector-like ornament, rendered locally without bitmap assets. */
public final class OrbitView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public OrbitView(Context context, AttributeSet attrs) { super(context, attrs); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale = Math.min(getWidth(), getHeight()) / 142f;
        canvas.save();
        canvas.scale(scale, scale);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(0x35D5C5FF);
        canvas.drawCircle(92, 56, 45, paint);
        canvas.drawCircle(92, 56, 62, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFF0DEBA);
        canvas.drawCircle(95, 56, 19, paint);
        paint.setColor(0xFF6C55A8);
        canvas.drawCircle(104, 48, 17, paint);
        paint.setColor(0xFFD8C7FB);
        canvas.drawCircle(36, 72, 3, paint);
        canvas.drawCircle(79, 117, 2, paint);
        canvas.drawCircle(125, 24, 2, paint);
        canvas.restore();
    }
}
