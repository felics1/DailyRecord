package com.example.dailyrecord.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.dailyrecord.domain.PlanGenerator;
import com.example.dailyrecord.model.Project;
import com.example.dailyrecord.model.ProjectInput;
import com.example.dailyrecord.model.RecordItem;
import com.example.dailyrecord.model.Stage;
import com.example.dailyrecord.model.StageDraft;
import com.example.dailyrecord.model.Stats;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "daily_record.db";
    private static final int DB_VERSION = 4;
    private static final long DAY = 86_400_000L;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE projects (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category TEXT NOT NULL DEFAULT '自定义', mode TEXT NOT NULL, unit TEXT NOT NULL DEFAULT '', status TEXT NOT NULL DEFAULT 'ACTIVE', reason TEXT NOT NULL DEFAULT '', baseline TEXT NOT NULL DEFAULT '', target TEXT NOT NULL DEFAULT '', stage_days INTEGER NOT NULL DEFAULT 7, reminder_enabled INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, group_name TEXT NOT NULL DEFAULT '未分类')");
        db.execSQL("CREATE TABLE rules (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, type TEXT NOT NULL, value TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, occurred INTEGER NOT NULL, outcome TEXT NOT NULL DEFAULT 'RESISTED', plan_result TEXT NOT NULL, amount REAL, actual_value TEXT NOT NULL DEFAULT '', urge INTEGER, trigger TEXT NOT NULL DEFAULT '', mood TEXT NOT NULL DEFAULT '', note TEXT NOT NULL DEFAULT '', occurred_at INTEGER NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE stages (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, target TEXT NOT NULL, duration_days INTEGER NOT NULL, starts_at INTEGER NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE impulse_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, started_at INTEGER NOT NULL, ended_at INTEGER, status TEXT NOT NULL, final_record_id INTEGER, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE impulse_events (id INTEGER PRIMARY KEY AUTOINCREMENT, session_id INTEGER NOT NULL, kind TEXT NOT NULL, value TEXT NOT NULL, created_at INTEGER NOT NULL, FOREIGN KEY(session_id) REFERENCES impulse_sessions(id) ON DELETE CASCADE)");
        db.execSQL("ALTER TABLE projects ADD COLUMN current_stage_position INTEGER NOT NULL DEFAULT 0");
        createProjectGroups(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE projects ADD COLUMN category TEXT NOT NULL DEFAULT '自定义'");
            db.execSQL("ALTER TABLE projects ADD COLUMN baseline TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE projects ADD COLUMN target TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE projects ADD COLUMN stage_days INTEGER NOT NULL DEFAULT 7");
            db.execSQL("ALTER TABLE projects ADD COLUMN reminder_enabled INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE records ADD COLUMN outcome TEXT NOT NULL DEFAULT 'RESISTED'");
            db.execSQL("ALTER TABLE records ADD COLUMN actual_value TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE records ADD COLUMN trigger TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE records ADD COLUMN mood TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE records ADD COLUMN note TEXT NOT NULL DEFAULT ''");
            db.execSQL("UPDATE records SET outcome=CASE WHEN occurred=0 THEN 'RESISTED' ELSE 'LOST' END");
            db.execSQL("CREATE TABLE stages (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, target TEXT NOT NULL, duration_days INTEGER NOT NULL, starts_at INTEGER NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
        }
        if (oldVersion < 3) db.execSQL("ALTER TABLE projects ADD COLUMN current_stage_position INTEGER NOT NULL DEFAULT 0");
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE projects ADD COLUMN group_name TEXT NOT NULL DEFAULT '未分类'");
            createProjectGroups(db);
        }
    }

    private static void createProjectGroups(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS project_groups (name TEXT PRIMARY KEY, built_in INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)");
        long now = System.currentTimeMillis();
        db.execSQL("INSERT OR IGNORE INTO project_groups(name,built_in,created_at) VALUES('长期',1,?)", new Object[]{now});
        db.execSQL("INSERT OR IGNORE INTO project_groups(name,built_in,created_at) VALUES('短期',1,?)", new Object[]{now + 1});
        db.execSQL("INSERT OR IGNORE INTO project_groups(name,built_in,created_at) VALUES('临时',1,?)", new Object[]{now + 2});
    }

    public long createProject(ProjectInput input) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            ContentValues values = new ContentValues();
            values.put("name", input.name.trim());
            values.put("category", input.category);
            values.put("mode", input.mode);
            values.put("unit", input.unit.trim());
            values.put("reason", input.reason.trim());
            values.put("baseline", input.baseline.trim());
            values.put("target", input.target.trim());
            values.put("stage_days", input.stageDays);
            values.put("reminder_enabled", input.reminderEnabled ? 1 : 0);
            values.put("group_name", input.groupName);
            values.put("created_at", now);
            long projectId = db.insertOrThrow("projects", null, values);
            long startsAt = now;
            List<StageDraft> stages = PlanGenerator.generate(input.category, input.baseline, input.target, input.stageDays);
            for (int i = 0; i < stages.size(); i++) {
                StageDraft stage = stages.get(i);
                ContentValues row = new ContentValues();
                row.put("project_id", projectId);
                row.put("position", i);
                row.put("title", stage.title);
                row.put("target", stage.target);
                row.put("duration_days", stage.durationDays);
                row.put("starts_at", startsAt);
                db.insertOrThrow("stages", null, row);
                startsAt += stage.durationDays * DAY;
            }
            db.setTransactionSuccessful();
            return projectId;
        } finally {
            db.endTransaction();
        }
    }

    public void updateProject(long projectId, ProjectInput input) {
        Project previous = getProject(projectId);
        if (previous == null) throw new IllegalArgumentException("Project no longer exists");
        boolean regenerate = !previous.category.equals(input.category)
                || !previous.mode.equals(input.mode) || !previous.baseline.equals(input.baseline.trim())
                || !previous.target.equals(input.target.trim()) || previous.stageDays != input.stageDays;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("name", input.name.trim());
            values.put("category", input.category);
            values.put("mode", input.mode);
            values.put("unit", input.unit.trim());
            values.put("reason", input.reason.trim());
            values.put("baseline", input.baseline.trim());
            values.put("target", input.target.trim());
            values.put("stage_days", input.stageDays);
            values.put("reminder_enabled", input.reminderEnabled ? 1 : 0);
            values.put("group_name", input.groupName);
            db.update("projects", values, "id=?", new String[]{Long.toString(projectId)});
            if (!regenerate) { db.setTransactionSuccessful(); return; }
            db.execSQL("UPDATE projects SET current_stage_position=0 WHERE id=?", new Object[]{projectId});
            db.delete("stages", "project_id=?", new String[]{Long.toString(projectId)});
            long startsAt = System.currentTimeMillis();
            List<StageDraft> stages = PlanGenerator.generate(input.category, input.baseline, input.target, input.stageDays);
            for (int i = 0; i < stages.size(); i++) {
                StageDraft stage = stages.get(i);
                ContentValues row = new ContentValues();
                row.put("project_id", projectId);
                row.put("position", i);
                row.put("title", stage.title);
                row.put("target", stage.target);
                row.put("duration_days", stage.durationDays);
                row.put("starts_at", startsAt);
                db.insertOrThrow("stages", null, row);
                startsAt += stage.durationDays * DAY;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Project getProject(long id) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,name,category,mode,unit,status,reason,baseline,target,stage_days,reminder_enabled,created_at,group_name FROM projects WHERE id=?",
                new String[]{Long.toString(id)})) {
            return cursor.moveToFirst() ? projectFrom(cursor) : null;
        }
    }

    public Project firstActiveProject() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,name,category,mode,unit,status,reason,baseline,target,stage_days,reminder_enabled,created_at,group_name FROM projects WHERE status='ACTIVE' ORDER BY id LIMIT 1", null)) {
            return cursor.moveToFirst() ? projectFrom(cursor) : null;
        }
    }

    public List<Project> listProjects() {
        List<Project> projects = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,name,category,mode,unit,status,reason,baseline,target,stage_days,reminder_enabled,created_at,group_name FROM projects ORDER BY CASE status WHEN 'ACTIVE' THEN 0 WHEN 'PAUSED' THEN 1 ELSE 2 END,id DESC", null)) {
            while (cursor.moveToNext()) projects.add(projectFrom(cursor));
        }
        return projects;
    }

    public List<Project> listProjects(String groupName) {
        if ("全部".equals(groupName)) return listProjects();
        List<Project> projects = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,name,category,mode,unit,status,reason,baseline,target,stage_days,reminder_enabled,created_at,group_name FROM projects WHERE group_name=? ORDER BY CASE status WHEN 'ACTIVE' THEN 0 WHEN 'PAUSED' THEN 1 ELSE 2 END,id DESC",
                new String[]{groupName})) {
            while (cursor.moveToNext()) projects.add(projectFrom(cursor));
        }
        return projects;
    }

    public List<String> listProjectGroups() {
        List<String> groups = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT name FROM project_groups ORDER BY built_in DESC,created_at,name", null)) {
            while (cursor.moveToNext()) groups.add(cursor.getString(0));
        }
        return groups;
    }

    public boolean createProjectGroup(String name) {
        String clean = cleanGroupName(name);
        if (clean == null) return false;
        ContentValues values = new ContentValues();
        values.put("name", clean);
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertWithOnConflict("project_groups", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }

    public boolean renameProjectGroup(String oldName, String newName) {
        String clean = cleanGroupName(newName);
        if (clean == null || isBuiltInGroup(oldName)) return false;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues group = new ContentValues();
            group.put("name", clean);
            group.put("created_at", System.currentTimeMillis());
            if (db.insertWithOnConflict("project_groups", null, group, SQLiteDatabase.CONFLICT_IGNORE) == -1) return false;
            ContentValues project = new ContentValues();
            project.put("group_name", clean);
            db.update("projects", project, "group_name=?", new String[]{oldName});
            db.delete("project_groups", "name=? AND built_in=0", new String[]{oldName});
            db.setTransactionSuccessful();
            return true;
        } finally { db.endTransaction(); }
    }

    public void deleteProjectGroup(String name) {
        if (isBuiltInGroup(name)) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues project = new ContentValues();
            project.put("group_name", "未分类");
            db.update("projects", project, "group_name=?", new String[]{name});
            db.delete("project_groups", "name=? AND built_in=0", new String[]{name});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public void moveProjectToGroup(long projectId, String groupName) {
        ContentValues values = new ContentValues();
        values.put("group_name", groupName);
        getWritableDatabase().update("projects", values, "id=?", new String[]{Long.toString(projectId)});
    }

    private static String cleanGroupName(String value) {
        if (value == null) return null;
        String clean = value.trim();
        if (clean.isEmpty() || clean.length() > 12 || "全部".equals(clean) || "未分类".equals(clean)) return null;
        return clean;
    }

    private static boolean isBuiltInGroup(String name) {
        return "长期".equals(name) || "短期".equals(name) || "临时".equals(name);
    }

    private Project projectFrom(Cursor c) {
        return new Project(c.getLong(0), c.getString(1), c.getString(2), c.getString(3),
                c.getString(4), c.getString(5), c.getString(6), c.getString(7),
                c.getString(8), c.getInt(9), c.getInt(10) == 1, c.getLong(11), c.getString(12));
    }

    public void setProjectStatus(long projectId, String status) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        getWritableDatabase().update("projects", values, "id=?", new String[]{Long.toString(projectId)});
    }

    public Stage currentStage(long projectId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,position,title,target,duration_days,starts_at FROM stages WHERE project_id=? AND position=(SELECT current_stage_position FROM projects WHERE id=?) LIMIT 1",
                new String[]{Long.toString(projectId), Long.toString(projectId)})) {
            if (cursor.moveToFirst()) return stageFrom(cursor);
        }
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,position,title,target,duration_days,starts_at FROM stages WHERE project_id=? ORDER BY position LIMIT 1",
                new String[]{Long.toString(projectId)})) {
            return cursor.moveToFirst() ? stageFrom(cursor) : null;
        }
    }

    public List<Stage> listStages(long projectId) {
        List<Stage> stages = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,position,title,target,duration_days,starts_at FROM stages WHERE project_id=? ORDER BY position",
                new String[]{Long.toString(projectId)})) {
            while (cursor.moveToNext()) stages.add(stageFrom(cursor));
        }
        return stages;
    }

    public boolean advanceStage(long projectId) {
        Stage current = currentStage(projectId);
        if (current == null) return false;
        List<Stage> stages = listStages(projectId);
        if (current.position + 1 >= stages.size()) return false;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("UPDATE projects SET current_stage_position=? WHERE id=?", new Object[]{current.position + 1, projectId});
            db.execSQL("UPDATE stages SET starts_at=? WHERE project_id=? AND position=?", new Object[]{System.currentTimeMillis(), projectId, current.position + 1});
            db.setTransactionSuccessful();
            return true;
        } finally { db.endTransaction(); }
    }

    private Stage stageFrom(Cursor c) {
        return new Stage(c.getLong(0), c.getInt(1), c.getString(2), c.getString(3), c.getInt(4), c.getLong(5));
    }

    public long addRecord(long projectId, String outcome, String planResult, String actualValue,
                          int urge, String trigger, String mood, String note) {
        ContentValues values = new ContentValues();
        values.put("project_id", projectId);
        values.put("occurred", "RESISTED".equals(outcome) ? 0 : 1);
        values.put("outcome", outcome);
        values.put("plan_result", planResult);
        values.put("actual_value", actualValue == null ? "" : actualValue.trim());
        values.put("urge", urge);
        values.put("trigger", trigger == null ? "" : trigger);
        values.put("mood", mood == null ? "" : mood);
        values.put("note", note == null ? "" : note.trim());
        values.put("occurred_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("records", null, values);
    }

    public List<RecordItem> listRecords(long projectId, int limit) {
        List<RecordItem> records = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,outcome,plan_result,actual_value,COALESCE(urge,0),trigger,mood,note,occurred_at FROM records WHERE project_id=? ORDER BY occurred_at DESC LIMIT ?",
                new String[]{Long.toString(projectId), Integer.toString(limit)})) {
            while (cursor.moveToNext()) {
                records.add(new RecordItem(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getInt(4), cursor.getString(5), cursor.getString(6),
                        cursor.getString(7), cursor.getLong(8)));
            }
        }
        return records;
    }

    public RecordItem getRecord(long recordId, long projectId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,outcome,plan_result,actual_value,COALESCE(urge,0),trigger,mood,note,occurred_at FROM records WHERE id=? AND project_id=?",
                new String[]{Long.toString(recordId), Long.toString(projectId)})) {
            return cursor.moveToFirst() ? new RecordItem(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                    cursor.getString(3), cursor.getInt(4), cursor.getString(5), cursor.getString(6),
                    cursor.getString(7), cursor.getLong(8)) : null;
        }
    }

    public void updateRecord(long recordId, long projectId, String outcome, String planResult,
                             String actualValue, int urge, String trigger, String mood, String note) {
        ContentValues values = new ContentValues();
        values.put("occurred", "RESISTED".equals(outcome) ? 0 : 1);
        values.put("outcome", outcome);
        values.put("plan_result", planResult);
        values.put("actual_value", actualValue == null ? "" : actualValue.trim());
        values.put("urge", urge);
        values.put("trigger", trigger == null ? "" : trigger);
        values.put("mood", mood == null ? "" : mood);
        values.put("note", note == null ? "" : note.trim());
        getWritableDatabase().update("records", values, "id=? AND project_id=?",
                new String[]{Long.toString(recordId), Long.toString(projectId)});
    }

    public void deleteRecord(long recordId) {
        getWritableDatabase().delete("records", "id=?", new String[]{Long.toString(recordId)});
    }

    public Stats stats(long projectId, int days) {
        long since = System.currentTimeMillis() - days * DAY;
        int total = 0, within = 0, exceeded = 0, urge = 0;
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*),SUM(CASE WHEN plan_result='WITHIN_PLAN' THEN 1 ELSE 0 END),SUM(CASE WHEN plan_result='EXCEEDED' THEN 1 ELSE 0 END),COALESCE(AVG(COALESCE(urge,0)),0) FROM records WHERE project_id=? AND occurred_at>=?",
                new String[]{Long.toString(projectId), Long.toString(since)})) {
            if (cursor.moveToFirst()) {
                total = cursor.getInt(0);
                within = cursor.getInt(1);
                exceeded = cursor.getInt(2);
                urge = cursor.getInt(3);
            }
        }
        String topTrigger = "暂无";
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT trigger,COUNT(*) count FROM records WHERE project_id=? AND occurred_at>=? AND trigger<>'' GROUP BY trigger ORDER BY count DESC LIMIT 1",
                new String[]{Long.toString(projectId), Long.toString(since)})) {
            if (cursor.moveToFirst()) topTrigger = cursor.getString(0);
        }
        return new Stats(total, within, exceeded, urge, topTrigger);
    }

    public List<Stats.Daily> dailyStats(long projectId, int days) {
        Calendar day = Calendar.getInstance();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        day.add(Calendar.DAY_OF_YEAR, -(days - 1));
        long since = day.getTimeInMillis();
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelFormat = new SimpleDateFormat("M/d", Locale.getDefault());
        Map<String, Stats.Daily> byDay = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            String key = keyFormat.format(day.getTime());
            byDay.put(key, new Stats.Daily(key, labelFormat.format(day.getTime())));
            day.add(Calendar.DAY_OF_YEAR, 1);
        }
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT occurred_at,plan_result,COALESCE(urge,0) FROM records WHERE project_id=? AND occurred_at>=? ORDER BY occurred_at",
                new String[]{Long.toString(projectId), Long.toString(since)})) {
            while (cursor.moveToNext()) {
                Stats.Daily value = byDay.get(keyFormat.format(new Date(cursor.getLong(0))));
                if (value != null) value.add(cursor.getString(1), cursor.getInt(2));
            }
        }
        return new ArrayList<>(byDay.values());
    }

    public List<Stats.LabelCount> topTriggers(long projectId, int days, int limit) {
        long since = System.currentTimeMillis() - days * DAY;
        List<Stats.LabelCount> values = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT trigger,COUNT(*) count FROM records WHERE project_id=? AND occurred_at>=? AND trigger<>'' GROUP BY trigger ORDER BY count DESC,trigger LIMIT ?",
                new String[]{Long.toString(projectId), Long.toString(since), Integer.toString(limit)})) {
            while (cursor.moveToNext()) values.add(new Stats.LabelCount(cursor.getString(0), cursor.getInt(1)));
        }
        return values;
    }

    public String planResultFor(long projectId, String outcome) {
        Project project = getProject(projectId);
        Stage stage = currentStage(projectId);
        if (project == null || "观察模式".equals(project.mode) || stage != null && stage.position == 0) return "NOT_APPLICABLE";
        if ("RESISTED".equals(outcome)) return "WITHIN_PLAN";
        if ("PLANNED".equals(outcome)) return project != null && "完全戒断".equals(project.mode) ? "EXCEEDED" : "WITHIN_PLAN";
        if ("PARTIAL".equals(outcome)) return "PENDING";
        return project != null && "观察模式".equals(project.mode) ? "NOT_APPLICABLE" : "EXCEEDED";
    }

    public long beginImpulse(long projectId, int urge) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues session = new ContentValues();
            session.put("project_id", projectId);
            session.put("started_at", System.currentTimeMillis());
            session.put("status", "ACTIVE");
            long id = db.insertOrThrow("impulse_sessions", null, session);
            addImpulseEvent(db, id, "URGE", Integer.toString(urge));
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public long activeImpulse(long projectId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id FROM impulse_sessions WHERE project_id=? AND status='ACTIVE' ORDER BY id DESC LIMIT 1",
                new String[]{Long.toString(projectId)})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1;
        }
    }

    public int impulseEventCount(long sessionId, String kind) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM impulse_events WHERE session_id=? AND kind=?",
                new String[]{Long.toString(sessionId), kind})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public void addImpulseEvent(long sessionId, String kind, String value) {
        addImpulseEvent(getWritableDatabase(), sessionId, kind, value);
    }

    private void addImpulseEvent(SQLiteDatabase db, long sessionId, String kind, String value) {
        ContentValues event = new ContentValues();
        event.put("session_id", sessionId);
        event.put("kind", kind);
        event.put("value", value);
        event.put("created_at", System.currentTimeMillis());
        db.insertOrThrow("impulse_events", null, event);
    }

    public void finishImpulse(long sessionId, long recordId, String status) {
        ContentValues values = new ContentValues();
        values.put("ended_at", System.currentTimeMillis());
        values.put("status", status);
        values.put("final_record_id", recordId);
        getWritableDatabase().update("impulse_sessions", values, "id=?", new String[]{Long.toString(sessionId)});
    }
}
