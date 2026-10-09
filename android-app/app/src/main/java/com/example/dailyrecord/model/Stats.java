package com.example.dailyrecord.model;

import java.util.Locale;

public final class Stats {
    public final int total;
    public final int withinPlan;
    public final int exceeded;
    public final int averageUrge;
    public final String topTrigger;

    public Stats(int total, int withinPlan, int exceeded, int averageUrge, String topTrigger) {
        this.total = total;
        this.withinPlan = withinPlan;
        this.exceeded = exceeded;
        this.averageUrge = averageUrge;
        this.topTrigger = topTrigger;
    }

    public int successPercent() {
        int assessed = withinPlan + exceeded;
        return assessed == 0 ? 0 : Math.round(withinPlan * 100f / assessed);
    }

    public int assessed() {
        return withinPlan + exceeded;
    }

    public int unassessed() {
        return Math.max(0, total - assessed());
    }

    public static final class Daily {
        public final String key;
        public final String label;
        public int total;
        public int withinPlan;
        public int exceeded;
        private int urgeSum;

        public Daily(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public void add(String planResult, int urge) {
            total++;
            urgeSum += urge;
            if ("WITHIN_PLAN".equals(planResult)) withinPlan++;
            else if ("EXCEEDED".equals(planResult)) exceeded++;
        }

        public int unassessed() {
            return Math.max(0, total - withinPlan - exceeded);
        }

        public int averageUrge() {
            return total == 0 ? 0 : Math.round(urgeSum / (float) total);
        }

        public String accessibilityText() {
            return String.format(Locale.getDefault(), "%s，记录%d次，达标%d次，超出%d次，未判定%d次",
                    label, total, withinPlan, exceeded, unassessed());
        }
    }

    public static final class LabelCount {
        public final String label;
        public final int count;

        public LabelCount(String label, int count) {
            this.label = label;
            this.count = count;
        }
    }
}
