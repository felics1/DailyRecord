package com.example.dailyrecord.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dailyrecord.R;
import com.example.dailyrecord.data.CurrentProjectStore;
import com.example.dailyrecord.data.DbHelper;
import com.example.dailyrecord.domain.PlanGenerator;
import com.example.dailyrecord.model.Project;
import com.example.dailyrecord.model.ProjectInput;
import com.example.dailyrecord.notify.ReminderScheduler;

import java.util.ArrayList;
import java.util.List;

public final class CreateProjectActivity extends BaseActivity {
    private static final String[] TEMPLATES = {"熬夜", "短视频", "游戏", "吸烟", "酒精", "购物", "自定义"};
    private static final String[] MODES = {"逐步调整", "完全戒断", "限量控制", "观察模式"};
    private EditText name;
    private EditText reason;
    private EditText baseline;
    private EditText target;
    private EditText unit;
    private EditText stageDays;
    private TextView preview;
    private Spinner template;
    private Spinner mode;
    private Spinner group;
    private CheckBox reminder;
    private DbHelper db;
    private long editingId = -1;
    private boolean editingLoaded;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_create_project);
        db = new DbHelper(this);
        editingId = getIntent().getLongExtra("editProjectId", -1);
        name = findViewById(R.id.nameInput);
        reason = findViewById(R.id.reasonInput);
        baseline = findViewById(R.id.baselineInput);
        target = findViewById(R.id.targetInput);
        unit = findViewById(R.id.unitInput);
        stageDays = findViewById(R.id.stageDaysInput);
        preview = findViewById(R.id.previewText);
        template = findViewById(R.id.templateSpinner);
        mode = findViewById(R.id.modeSpinner);
        group = findViewById(R.id.groupSpinner);
        reminder = findViewById(R.id.reminderCheck);
        List<String> groups = new ArrayList<>();
        groups.add("未分类");
        groups.addAll(db.listProjectGroups());
        group.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, groups));
        String initialGroup = getIntent().getStringExtra("groupName");
        if (initialGroup != null) select(group, initialGroup);
        template.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, TEMPLATES));
        mode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, MODES));
        template.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (editingId >= 0 && !editingLoaded) {
                    editingLoaded = true;
                    Project p = db.getProject(editingId);
                    if (p != null) {
                        for (int i = 0; i < TEMPLATES.length; i++) if (TEMPLATES[i].equals(p.category)) template.setSelection(i);
                        name.setText(p.name);
                        reason.setText(p.reason);
                        baseline.setText(p.baseline);
                        target.setText(p.target);
                        unit.setText(p.unit);
                        stageDays.setText(Integer.toString(p.stageDays));
                        for (int i = 0; i < MODES.length; i++) if (MODES[i].equals(p.mode)) mode.setSelection(i);
                        reminder.setChecked(p.reminderEnabled);
                        select(group, p.groupName);
                        ((TextView) findViewById(R.id.createTitle)).setText("调整你的计划");
                        ((TextView) findViewById(R.id.saveButton)).setText("保存调整");
                        updatePreview();
                    }
                    return;
                }
                if (editingId >= 0 && editingLoaded) { updatePreview(); return; }
                applyTemplate(TEMPLATES[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updatePreview(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        baseline.addTextChangedListener(watcher);
        target.addTextChangedListener(watcher);
        stageDays.addTextChangedListener(watcher);
        findViewById(R.id.saveButton).setOnClickListener(v -> save());
    }

    private void applyTemplate(String category) {
        if ("熬夜".equals(category)) set("改善熬夜", "01:30", "23:30", "时间", 7, 0);
        else if ("短视频".equals(category)) set("减少短视频", "180", "60", "分钟/天", 7, 2);
        else if ("游戏".equals(category)) set("控制游戏时间", "180", "60", "分钟/天", 7, 2);
        else if ("吸烟".equals(category)) set("减少吸烟", "10", "0", "支/天", 7, 0);
        else if ("酒精".equals(category)) {
            set("减少饮酒", "", "", "次/周", 7, 0);
            new AlertDialog.Builder(this).setTitle("先确认安全")
                    .setMessage("长期大量饮酒者突然停止可能出现危险的戒断反应。请优先咨询专业医疗人员，本应用不能替代诊断或治疗。")
                    .setPositiveButton("我知道了", null).show();
        } else if ("购物".equals(category)) set("减少冲动购物", "7", "1", "次/周", 7, 2);
        else set("", "", "", "", 7, 0);
        updatePreview();
    }

    private void set(String projectName, String from, String to, String projectUnit, int days, int modeIndex) {
        name.setText(projectName);
        baseline.setText(from);
        target.setText(to);
        unit.setText(projectUnit);
        stageDays.setText(Integer.toString(days));
        mode.setSelection(modeIndex);
    }

    private void updatePreview() {
        if (template == null || preview == null) return;
        preview.setText(PlanGenerator.preview(template.getSelectedItem().toString(), text(baseline), text(target), days()));
    }

    private void save() {
        if (text(name).isEmpty()) { name.setError("请输入计划名称"); return; }
        if (text(target).isEmpty()) { target.setError("请输入最终目标"); return; }
        if ("熬夜".equals(template.getSelectedItem().toString())) {
            if (!text(baseline).matches("(?:[01]?\\d|2[0-3]):[0-5]\\d")) { baseline.setError("请按 01:30 这样的格式输入"); return; }
            if (!text(target).matches("(?:[01]?\\d|2[0-3]):[0-5]\\d")) { target.setError("请按 23:30 这样的格式输入"); return; }
        }
        if (editingId >= 0) {
            new AlertDialog.Builder(this).setTitle("保存计划调整")
                .setMessage("历史记录会保留。若修改了目标或阶段长度，阶段计划将从观察期重新生成。")
                .setNegativeButton("再检查一下", null)
                .setPositiveButton("保存", (dialog, which) -> persist()).show();
        } else persist();
    }

    private void persist() {
        ProjectInput input = new ProjectInput(text(name), template.getSelectedItem().toString(),
                mode.getSelectedItem().toString(), text(unit), text(reason), text(baseline),
                text(target), days(), reminder.isChecked(), group.getSelectedItem().toString());
        long id;
        if (editingId >= 0) { db.updateProject(editingId, input); id = editingId; }
        else id = db.createProject(input);
        ReminderScheduler.cancel(this, id);
        CurrentProjectStore.select(this, id);
        Project project = db.getProject(id);
        if (project != null && project.reminderEnabled) {
            ReminderScheduler.schedule(this, project);
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
                Toast.makeText(this, "计划已保存，可选择是否允许提醒", Toast.LENGTH_LONG).show();
                return;
            }
        }
        Toast.makeText(this, "计划已生成，从观察期开始", Toast.LENGTH_LONG).show();
        finish();
    }

    private int days() {
        try { return Math.min(365, Math.max(3, Integer.parseInt(text(stageDays)))); }
        catch (NumberFormatException ignored) { return 7; }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) finish();
    }

    private static String text(EditText input) {
        return input.getText().toString().trim();
    }

    private static void select(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equals(spinner.getItemAtPosition(i))) { spinner.setSelection(i); return; }
        }
    }

    @Override protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
