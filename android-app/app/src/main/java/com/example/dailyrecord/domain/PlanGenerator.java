package com.example.dailyrecord.domain;

import com.example.dailyrecord.model.StageDraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlanGenerator {
    private PlanGenerator() {}

    public static List<StageDraft> generate(String category, String baseline, String target, int stageDays) {
        int days = Math.max(3, stageDays);
        List<StageDraft> result = new ArrayList<>();
        result.add(new StageDraft("观察期", "只记录真实情况，不要求立刻改变", 3));
        if ("熬夜".equals(category)) {
            Integer from = parseTime(baseline);
            Integer to = parseTime(target);
            if (from != null && to != null) {
                if (from < 6 * 60) from += 24 * 60;
                if (to < 6 * 60) to += 24 * 60;
                int direction = Integer.compare(to, from);
                int current = from;
                int index = 1;
                while (current != to) {
                    current += direction * Math.min(30, Math.abs(to - current));
                    result.add(new StageDraft("第" + index + "阶段", "最晚 " + formatTime(current) + " 停止主动娱乐", days));
                    index++;
                }
                if (result.size() == 1) result.add(new StageDraft("稳定期", "保持 " + formatTime(to) + " 前停止主动娱乐", days * 2));
                else result.add(new StageDraft("稳定期", "连续保持目标作息", days * 2));
                return result;
            }
        }
        result.add(new StageDraft("起步阶段", displayTarget(baseline, target, 0.25f), days));
        result.add(new StageDraft("适应阶段", displayTarget(baseline, target, 0.60f), days));
        result.add(new StageDraft("目标阶段", "达到目标：" + target, days * 2));
        return result;
    }

    public static String preview(String category, String baseline, String target, int stageDays) {
        List<StageDraft> stages = generate(category, baseline, target, stageDays);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < stages.size(); i++) {
            StageDraft stage = stages.get(i);
            if (i > 0) text.append('\n');
            text.append(i + 1).append(". ").append(stage.title).append(" · ")
                    .append(stage.durationDays).append("天\n   ").append(stage.target);
        }
        return text.toString();
    }

    private static String displayTarget(String baseline, String target, float progress) {
        try {
            double from = Double.parseDouble(baseline);
            double to = Double.parseDouble(target);
            double value = from + (to - from) * progress;
            return "阶段目标：" + String.format(Locale.ROOT, "%.1f", value);
        } catch (NumberFormatException ignored) {
            return progress < 0.5f ? "先减少约四分之一" : "逐步接近目标：" + target;
        }
    }

    static Integer parseTime(String value) {
        if (value == null || !value.matches("(?:[01]?\\d|2[0-3]):[0-5]\\d")) return null;
        String[] parts = value.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private static String formatTime(int minutes) {
        int normalized = ((minutes % (24 * 60)) + 24 * 60) % (24 * 60);
        return String.format(Locale.ROOT, "%02d:%02d", normalized / 60, normalized % 60);
    }
}
