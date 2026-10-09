package com.example.dailyrecord.domain;

public final class NextStepAdvisor {
    private NextStepAdvisor() {}

    public static int phase(boolean hasProject, int recordCount) {
        if (!hasProject) return 1;
        if (recordCount < 3) return 2;
        if (recordCount < 7) return 3;
        return 4;
    }
}
