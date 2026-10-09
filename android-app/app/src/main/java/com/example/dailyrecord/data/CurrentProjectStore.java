package com.example.dailyrecord.data;

import android.content.Context;

import com.example.dailyrecord.model.Project;

public final class CurrentProjectStore {
    private static final String PREFS = "current_project";
    private static final String KEY_ID = "project_id";

    private CurrentProjectStore() {}

    public static Project load(Context context, DbHelper db) {
        long id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_ID, -1);
        Project selected = id < 0 ? null : db.getProject(id);
        if (selected != null && "ACTIVE".equals(selected.status)) return selected;
        selected = db.firstActiveProject();
        if (selected != null) select(context, selected.id);
        return selected;
    }

    public static void select(Context context, long id) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_ID, id).apply();
    }
}
