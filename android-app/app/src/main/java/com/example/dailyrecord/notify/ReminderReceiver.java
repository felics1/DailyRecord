package com.example.dailyrecord.notify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.dailyrecord.R;
import com.example.dailyrecord.data.DbHelper;
import com.example.dailyrecord.model.Project;
import com.example.dailyrecord.ui.MainActivity;

public final class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "daily_plan";

    @Override public void onReceive(Context context, Intent intent) {
        long projectId = intent.getLongExtra("projectId", -1);
        try (DbHelper db = new DbHelper(context)) {
            Project project = db.getProject(projectId);
            if (project == null || !project.reminderEnabled || !"ACTIVE".equals(project.status)) return;
        }
        String name = intent.getStringExtra("name");
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL, "计划提醒", NotificationManager.IMPORTANCE_DEFAULT));
        }
        PendingIntent open = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(name == null ? "今天的计划" : name)
                .setContentText("先做一个小小的收尾动作，为目标留出空间。")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        try { manager.notify((int) projectId, notification); }
        catch (SecurityException ignored) { }
        try (DbHelper db = new DbHelper(context)) {
            Project project = db.getProject(projectId);
            if (project != null) ReminderScheduler.schedule(context, project);
        }
    }
}
