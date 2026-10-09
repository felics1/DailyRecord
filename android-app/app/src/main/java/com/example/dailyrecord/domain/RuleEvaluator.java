package com.example.dailyrecord.domain;

import java.util.List;

public final class RuleEvaluator {
    public enum Result { WITHIN_PLAN, EXCEEDED, PENDING, NOT_APPLICABLE }

    private RuleEvaluator() {}

    public static Result evaluate(List<Boolean> applicableRules) {
        if (applicableRules.isEmpty()) return Result.NOT_APPLICABLE;
        boolean pending = false;
        for (Boolean passed : applicableRules) {
            if (Boolean.FALSE.equals(passed)) return Result.EXCEEDED;
            if (passed == null) pending = true;
        }
        return pending ? Result.PENDING : Result.WITHIN_PLAN;
    }
}
