package com.example.dailyrecord.model;

public final class RecordItem {
    public final long id;
    public final String outcome;
    public final String planResult;
    public final String actualValue;
    public final int urge;
    public final String trigger;
    public final String mood;
    public final String note;
    public final long occurredAt;

    public RecordItem(long id, String outcome, String planResult, String actualValue,
                      int urge, String trigger, String mood, String note, long occurredAt) {
        this.id = id;
        this.outcome = outcome;
        this.planResult = planResult;
        this.actualValue = actualValue;
        this.urge = urge;
        this.trigger = trigger;
        this.mood = mood;
        this.note = note;
        this.occurredAt = occurredAt;
    }
}
