package com.example.dailyrecord.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.example.dailyrecord.R;
import com.example.dailyrecord.model.Stats;

import java.util.ArrayList;
import java.util.List;

public final class TrendChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<Stats.Daily> days = new ArrayList<>();

    public TrendChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setData(List<Stats.Daily> values) {
        days = values == null ? new ArrayList<>() : values;
        StringBuilder description = new StringBuilder("近七天记录趋势。");
        for (Stats.Daily day : days) description.append(day.accessibilityText()).append('。');
        setContentDescription(description.toString());
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (days.isEmpty()) return;
        float density = getResources().getDisplayMetrics().density;
        float scaled = getResources().getDisplayMetrics().scaledDensity;
        float left = 8 * density;
        float right = getWidth() - 8 * density;
        float top = 12 * density;
        float bottom = getHeight() - 30 * density;
        int max = 1;
        for (Stats.Daily day : days) max = Math.max(max, day.total);
        float step = (right - left) / days.size();
        float width = Math.min(28 * density, step * 0.58f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(11 * scaled);
        paint.setColor(getResources().getColor(R.color.text_secondary, getContext().getTheme()));
        for (int i = 0; i < days.size(); i++) {
            Stats.Daily day = days.get(i);
            float center = left + step * (i + .5f);
            float y = bottom;
            y = drawSegment(canvas, center, y, day.withinPlan, max, bottom - top, width, R.color.stats_within);
            y = drawSegment(canvas, center, y, day.exceeded, max, bottom - top, width, R.color.stats_exceeded);
            drawSegment(canvas, center, y, day.unassessed(), max, bottom - top, width, R.color.stats_unassessed);
            paint.setColor(getResources().getColor(R.color.text_secondary, getContext().getTheme()));
            canvas.drawText(day.label, center, getHeight() - 8 * density, paint);
        }
    }

    private float drawSegment(Canvas canvas, float center, float bottom, int count, int max,
                              float available, float width, int color) {
        if (count <= 0) return bottom;
        float height = available * count / max;
        paint.setColor(getResources().getColor(color, getContext().getTheme()));
        canvas.drawRect(center - width / 2, bottom - height, center + width / 2, bottom, paint);
        return bottom - height;
    }
}
