package com.example.dailyrecord.model;

public final class StageDraft {
    public final String title;
    public final String target;
    public final int durationDays;

    public StageDraft(String title, String target, int durationDays) {
        this.title = title;
        this.target = target;
        this.durationDays = durationDays;
    }
}
