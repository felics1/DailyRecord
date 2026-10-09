package com.example.dailyrecord.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.dailyrecord.R;
import com.example.dailyrecord.data.DbHelper;
import com.example.dailyrecord.model.Project;
import com.example.dailyrecord.model.RecordItem;

public final class CheckInActivity extends BaseActivity {
    private static final String[] OUTCOME_LABELS = {"控制住了", "计划内发生", "部分发生", "失控"};
    private static final String[] OUTCOMES = {"RESISTED", "PLANNED", "PARTIAL", "LOST"};
    private static final String[] TRIGGERS = {"没有明显诱因", "无聊", "压力", "疲惫", "情绪低落", "社交影响", "环境触发", "习惯性打开", "其他"};
    private static final String[] MOODS = {"平静", "开心", "焦虑", "烦躁", "难过", "疲惫", "空虚", "其他"};
    private DbHelper db;
    private long projectId;
    private long editingRecordId = -1;
    private int urge = 5;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_check_in);
        db = new DbHelper(this);
        projectId = getIntent().getLongExtra("projectId", -1);
        editingRecordId = getIntent().getLongExtra("editRecordId", -1);
        Project project = db.getProject(projectId);
        if (project == null) { finish(); return; }
        ((TextView) findViewById(R.id.checkInSubtitle)).setText(project.name + " · 记录用于看清规律，不用于责备自己");
        EditText actual = findViewById(R.id.actualInput);
        actual.setHint("熬夜".equals(project.category) ? "实际停止娱乐时间，如 23:40" : "实际情况或用量（" + project.unit + "）");
        Spinner outcome = findViewById(R.id.outcomeSpinner);
        Spinner trigger = findViewById(R.id.triggerSpinner);
        Spinner mood = findViewById(R.id.moodSpinner);
        outcome.setAdapter(adapter(OUTCOME_LABELS));
        trigger.setAdapter(adapter(TRIGGERS));
        mood.setAdapter(adapter(MOODS));
        TextView urgeLabel = findViewById(R.id.urgeLabel);
        SeekBar urgeSeek = findViewById(R.id.urgeSeek);
        urgeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                urge = value;
                urgeLabel.setText("冲动强度：" + value + " / 10");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        if (editingRecordId >= 0) {
            RecordItem record = db.getRecord(editingRecordId, projectId);
            if (record == null) { finish(); return; }
            ((TextView) findViewById(R.id.checkInTitle)).setText("修改记录");
            ((TextView) findViewById(R.id.saveRecordButton)).setText("保存修改");
            select(outcome, OUTCOMES, record.outcome);
            actual.setText(record.actualValue);
            urge = record.urge;
            urgeSeek.setProgress(record.urge);
            select(trigger, TRIGGERS, record.trigger);
            select(mood, MOODS, record.mood);
            ((EditText) findViewById(R.id.noteInput)).setText(record.note);
        }
        findViewById(R.id.saveRecordButton).setOnClickListener(v -> {
            int index = outcome.getSelectedItemPosition();
            String result = OUTCOMES[index];
            String note = ((EditText) findViewById(R.id.noteInput)).getText().toString();
            if (editingRecordId >= 0) {
                db.updateRecord(editingRecordId, projectId, result, db.planResultFor(projectId, result),
                        actual.getText().toString(), urge, trigger.getSelectedItem().toString(),
                        mood.getSelectedItem().toString(), note);
                android.widget.Toast.makeText(this, "记录已更新", android.widget.Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            long recordId = db.addRecord(projectId, result, db.planResultFor(projectId, result), actual.getText().toString(),
                    urge, trigger.getSelectedItem().toString(), mood.getSelectedItem().toString(), note);
            new AlertDialog.Builder(this).setTitle("已经记下来了")
                    .setMessage(index == 0 ? "你为自己争取了一次选择。" : "记录事实就够了，下一次仍然可以重新选择。")
                    .setNegativeButton("撤销这条", (dialog, which) -> { db.deleteRecord(recordId); finish(); })
                    .setPositiveButton("完成", (dialog, which) -> finish())
                    .setCancelable(false).show();
        });
    }

    private ArrayAdapter<String> adapter(String[] values) {
        return new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
    }

    private static void select(Spinner spinner, String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) { spinner.setSelection(i); return; }
        }
    }

    @Override protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
