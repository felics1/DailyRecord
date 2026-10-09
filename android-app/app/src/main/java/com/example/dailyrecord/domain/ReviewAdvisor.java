package com.example.dailyrecord.domain;

import com.example.dailyrecord.model.Stats;

public final class ReviewAdvisor {
    private ReviewAdvisor() {}

    public static String advice(Stats stats) {
        if (stats.total < 3) return "继续诚实记录几次，样本还不足，暂时不对你下结论。";
        if (stats.assessed() == 0) return "这些记录还处于观察或待确认状态。继续记录即可，暂时不判断达标与否。";
        if (stats.assessed() < 3) return "可判定记录还不足3次，先保持当前阶段，暂不根据达标率调整目标。";
        if (stats.successPercent() >= 80) return "当前阶段已经比较稳定，可以考虑进入下一阶段；不必追求每天完美。";
        if (stats.successPercent() >= 50) return "方向是对的。建议保持当前阶段，把最常见诱因提前处理。";
        return "当前目标可能偏难。建议把阶段放慢一些，先提高可执行性，再继续推进。";
    }
}
