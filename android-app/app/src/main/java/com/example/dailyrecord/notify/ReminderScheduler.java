package com.example.dailyrecord.notify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.dailyrecord.model.Project;

import java.util.Calendar;

public final class ReminderScheduler {
    private ReminderScheduler() {}

    public static void schedule(Context context, Project project) {
        if (!project.reminderEnabled || !"ACTIVE".equals(project.status)) return;
        int hour = 20, minute = 0;
        if (project.target.matches("(?:[01]?\\d|2[0-3]):[0-5]\\d")) {
            String[] parts = project.target.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]) - 30;
            if (minute < 0) { minute += 60; hour = (hour + 23) % 24; }
        }
        Calendar when = Calendar.getInstance();
        when.set(Calendar.HOUR_OF_DAY, hour);
        when.set(Calendar.MINUTE, minute);
        when.set(Calendar.SECOND, 0);
        when.set(Calendar.MILLISECOND, 0);
        if (when.getTimeInMillis() <= System.currentTimeMillis()) when.add(Calendar.DAY_OF_YEAR, 1);
        Intent intent = new Intent(context, ReminderReceiver.class)
                .putExtra("projectId", project.id)
                .putExtra("name", project.name);
        PendingIntent pending = PendingIntent.getBroadcast(context, (int) project.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pending);
    }

    public static void cancel(Context context, long projectId) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, (int) projectId, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pending != null) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(pending);
            pending.cancel();
        }
    }
}
