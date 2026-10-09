package com.example.dailyrecord.model;

public final class Project {
    public final long id;
    public final String name;
    public final String category;
    public final String mode;
    public final String unit;
    public final String status;
    public final String reason;
    public final String baseline;
    public final String target;
    public final int stageDays;
    public final boolean reminderEnabled;
    public final long createdAt;
    public final String groupName;

    public Project(long id, String name, String category, String mode, String unit,
                   String status, String reason, String baseline, String target,
                   int stageDays, boolean reminderEnabled, long createdAt, String groupName) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.mode = mode;
        this.unit = unit;
        this.status = status;
        this.reason = reason;
        this.baseline = baseline;
        this.target = target;
        this.stageDays = stageDays;
        this.reminderEnabled = reminderEnabled;
        this.createdAt = createdAt;
        this.groupName = groupName;
    }
}
