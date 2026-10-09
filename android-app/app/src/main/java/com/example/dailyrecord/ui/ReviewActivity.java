package com.example.dailyrecord.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.dailyrecord.R;
import com.example.dailyrecord.data.DbHelper;
import com.example.dailyrecord.domain.ReviewAdvisor;
import com.example.dailyrecord.model.Project;
import com.example.dailyrecord.model.Stage;
import com.example.dailyrecord.model.Stats;
import com.example.dailyrecord.ui.components.TrendChartView;

import java.util.List;

public final class ReviewActivity extends BaseActivity {
    private DbHelper db;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_review);
        db = new DbHelper(this);
        long projectId = getIntent().getLongExtra("projectId", -1);
        Project project = db.getProject(projectId);
        if (project == null) { finish(); return; }
        ((TextView) findViewById(R.id.reviewSubtitle)).setText(project.name + " · 复盘是为了调整计划，不是给自己打分");
        Stats stats = db.stats(projectId, 30);
        String summary = "记录总数：" + stats.total + " 次\n"
                + "已判定：" + stats.assessed() + " 次（达标 " + stats.withinPlan + "，超出 " + stats.exceeded + "）\n"
                + "未判定：" + stats.unassessed() + " 次\n"
                + (stats.assessed() == 0 ? "达标率：暂无可判定记录" : "已判定记录达标率：" + stats.successPercent() + "%")
                + (stats.total == 0 ? "" : "\n平均冲动：" + stats.averageUrge + "/10");
        ((TextView) findViewById(R.id.reviewStatsText)).setText(summary);
        ((TextView) findViewById(R.id.adviceText)).setText("下一步建议\n" + ReviewAdvisor.advice(stats));
        renderTrend(projectId);
        renderTriggers(projectId, stats);
        List<Stage> stages = db.listStages(projectId);
        Stage current = db.currentStage(projectId);
        StringBuilder plan = new StringBuilder();
        for (Stage stage : stages) {
            if (plan.length() > 0) plan.append("\n\n");
            plan.append(current != null && current.id == stage.id ? "● " : "○ ")
                    .append(stage.title).append(" · ").append(stage.durationDays).append("天\n")
                    .append(stage.target);
        }
        ((TextView) findViewById(R.id.stagesText)).setText(plan.toString());
        findViewById(R.id.advanceStageButton).setOnClickListener(v -> {
            new AlertDialog.Builder(this).setTitle("进入下一阶段？")
                .setMessage("由你决定进度。觉得还没准备好，可以继续保持当前阶段。")
                .setNegativeButton("继续当前阶段", null)
                .setPositiveButton("进入下一阶段", (dialog, which) -> {
                    if (db.advanceStage(projectId)) recreate();
                    else android.widget.Toast.makeText(this, "已经到达最后一个阶段", android.widget.Toast.LENGTH_SHORT).show();
                }).show();
        });
    }

    private void renderTrend(long projectId) {
        Stats recent = db.stats(projectId, 7);
        TextView notice = findViewById(R.id.trendSampleNotice);
        View legend = findViewById(R.id.chartLegend);
        TrendChartView chart = findViewById(R.id.trendChart);
        TableLayout table = findViewById(R.id.trendTable);
        if (recent.total < 3) {
            notice.setText("近7天记录 " + recent.total + " 次。至少记录3次后展示趋势，避免少量数据造成误导。");
            legend.setVisibility(View.GONE);
            chart.setVisibility(View.GONE);
            table.setVisibility(View.GONE);
            return;
        }
        notice.setText("柱高代表当天记录数；准确数值见下表。达标率不包含未判定记录。");
        List<Stats.Daily> days = db.dailyStats(projectId, 7);
        chart.setData(days);
        chart.setVisibility(View.VISIBLE);
        legend.setVisibility(View.VISIBLE);
        colorLegend((TextView) legend);
        table.setVisibility(View.VISIBLE);
        table.removeAllViews();
        table.addView(row(true, "日期", "记录", "达标", "超出", "未判定"));
        for (Stats.Daily day : days) {
            table.addView(row(false, day.label, Integer.toString(day.total), Integer.toString(day.withinPlan),
                    Integer.toString(day.exceeded), Integer.toString(day.unassessed())));
        }
    }

    private void colorLegend(TextView legend) {
        String value = "■ 达标　■ 超出　■ 未判定";
        SpannableString colored = new SpannableString(value);
        int second = value.indexOf('■', 1);
        int third = value.indexOf('■', second + 1);
        colored.setSpan(new ForegroundColorSpan(getColor(R.color.stats_within)), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        colored.setSpan(new ForegroundColorSpan(getColor(R.color.stats_exceeded)), second, second + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        colored.setSpan(new ForegroundColorSpan(getColor(R.color.stats_unassessed)), third, third + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        legend.setText(colored);
    }

    private void renderTriggers(long projectId, Stats stats) {
        View card = findViewById(R.id.triggerCard);
        if (stats.total < 3) { card.setVisibility(View.GONE); return; }
        List<Stats.LabelCount> triggers = db.topTriggers(projectId, 30, 3);
        card.setVisibility(View.VISIBLE);
        TextView text = findViewById(R.id.triggerStatsText);
        if (triggers.isEmpty()) { text.setText("暂无可统计的诱因标签。"); return; }
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < triggers.size(); i++) {
            if (i > 0) value.append('\n');
            value.append(i + 1).append(". ").append(triggers.get(i).label)
                    .append("　").append(triggers.get(i).count).append("次");
        }
        text.setText(value.toString());
    }

    private TableRow row(boolean bold, String... values) {
        TableRow row = new TableRow(this);
        for (int i = 0; i < values.length; i++) {
            TextView cell = new TextView(this);
            cell.setText(values[i]);
            cell.setTextColor(getColor(i == 0 ? R.color.text_primary : R.color.text_secondary));
            cell.setTextSize(12);
            cell.setGravity(i == 0 ? Gravity.START : Gravity.CENTER);
            cell.setPadding(0, dp(7), 0, dp(7));
            if (bold) cell.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.addView(cell, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
        }
        return row;
    }

    @Override protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
