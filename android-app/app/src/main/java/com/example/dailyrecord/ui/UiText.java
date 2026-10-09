package com.example.dailyrecord.ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class UiText {
    private UiText() {}

    static String date(long time) {
        return new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date(time));
    }

    static String dateTime(long time) {
        return new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(new Date(time));
    }

    static String outcome(String value) {
        if ("RESISTED".equals(value)) return "控制住了";
        if ("PLANNED".equals(value)) return "计划内发生";
        if ("PARTIAL".equals(value)) return "部分发生";
        return "失控";
    }

    static String status(String value) {
        if ("ACTIVE".equals(value)) return "进行中";
        if ("PAUSED".equals(value)) return "已暂停";
        return "已归档";
    }
}
