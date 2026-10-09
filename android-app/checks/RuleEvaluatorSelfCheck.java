package com.example.dailyrecord;

import com.example.dailyrecord.domain.PlanGenerator;
import com.example.dailyrecord.domain.NextStepAdvisor;
import com.example.dailyrecord.domain.RuleEvaluator;
import com.example.dailyrecord.domain.ReviewAdvisor;
import com.example.dailyrecord.model.StageDraft;
import com.example.dailyrecord.model.Stats;

import java.util.Arrays;
import java.util.Collections;

public final class RuleEvaluatorSelfCheck {
    public static void main(String[] args) {
        require(RuleEvaluator.evaluate(Collections.emptyList()) == RuleEvaluator.Result.NOT_APPLICABLE);
        require(RuleEvaluator.evaluate(Arrays.asList(true, true)) == RuleEvaluator.Result.WITHIN_PLAN);
        require(RuleEvaluator.evaluate(Arrays.asList(true, null)) == RuleEvaluator.Result.PENDING);
        require(RuleEvaluator.evaluate(Arrays.asList(true, null, false)) == RuleEvaluator.Result.EXCEEDED);
        java.util.List<StageDraft> sleep = PlanGenerator.generate("熬夜", "01:30", "23:30", 7);
        require(sleep.size() == 6);
        require(sleep.get(1).target.contains("01:00"));
        require(sleep.get(4).target.contains("23:30"));
        require(new Stats(10, 2, 2, 5, "压力").successPercent() == 50);
        require(new Stats(4, 0, 0, 5, "").successPercent() == 0);
        require(new Stats(7, 3, 1, 5, "").assessed() == 4);
        require(new Stats(7, 3, 1, 5, "").unassessed() == 3);
        Stats.Daily daily = new Stats.Daily("2026-10-09", "10/9");
        daily.add("WITHIN_PLAN", 4);
        daily.add("PENDING", 8);
        require(daily.total == 2 && daily.withinPlan == 1 && daily.unassessed() == 1 && daily.averageUrge() == 6);
        require(ReviewAdvisor.advice(new Stats(3, 0, 0, 5, "")).contains("观察或待确认"));
        require(NextStepAdvisor.phase(false, 0) == 1);
        require(NextStepAdvisor.phase(true, 0) == 2);
        require(NextStepAdvisor.phase(true, 3) == 3);
        require(NextStepAdvisor.phase(true, 7) == 4);
        java.util.List<StageDraft> longPlan = PlanGenerator.generate("熬夜", "03:30", "21:00", 7);
        require(longPlan.get(longPlan.size() - 2).target.contains("21:00"));
        System.out.println("Domain self-check passed");
    }

    private static void require(boolean condition) {
        if (!condition) throw new AssertionError("Domain self-check failed");
    }
}
