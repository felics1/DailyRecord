package com.example.dailyrecord.model;

public final class Stage {
    public final long id;
    public final int position;
    public final String title;
    public final String target;
    public final int durationDays;
    public final long startsAt;

    public Stage(long id, int position, String title, String target, int durationDays, long startsAt) {
        this.id = id;
        this.position = position;
        this.title = title;
        this.target = target;
        this.durationDays = durationDays;
        this.startsAt = startsAt;
    }
}
