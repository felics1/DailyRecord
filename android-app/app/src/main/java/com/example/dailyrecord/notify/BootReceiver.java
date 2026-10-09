package com.example.dailyrecord.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.dailyrecord.data.DbHelper;
import com.example.dailyrecord.model.Project;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        try (DbHelper db = new DbHelper(context)) {
            for (Project project : db.listProjects()) ReminderScheduler.schedule(context, project);
        }
    }
}
