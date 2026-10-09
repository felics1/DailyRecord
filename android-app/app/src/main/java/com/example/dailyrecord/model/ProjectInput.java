package com.example.dailyrecord.model;

public final class ProjectInput {
    public final String name;
    public final String category;
    public final String mode;
    public final String unit;
    public final String reason;
    public final String baseline;
    public final String target;
    public final int stageDays;
    public final boolean reminderEnabled;
    public final String groupName;

    public ProjectInput(String name, String category, String mode, String unit,
                        String reason, String baseline, String target,
                        int stageDays, boolean reminderEnabled, String groupName) {
        this.name = name;
        this.category = category;
        this.mode = mode;
        this.unit = unit;
        this.reason = reason;
        this.baseline = baseline;
        this.target = target;
        this.stageDays = stageDays;
        this.reminderEnabled = reminderEnabled;
        this.groupName = groupName;
    }
}
