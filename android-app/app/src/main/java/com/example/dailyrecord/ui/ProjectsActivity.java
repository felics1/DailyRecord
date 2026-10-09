package com.example.dailyrecord.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dailyrecord.R;
import com.example.dailyrecord.data.CurrentProjectStore;
import com.example.dailyrecord.data.DbHelper;
import com.example.dailyrecord.model.Project;

import java.util.List;
import java.util.ArrayList;

public final class ProjectsActivity extends BaseActivity {
    private DbHelper db;
    private List<Project> projects;
    private ListView list;
    private LinearLayout groupBar;
    private TextView empty;
    private String selectedGroup = "全部";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_projects);
        db = new DbHelper(this);
        list = findViewById(R.id.projectsList);
        groupBar = findViewById(R.id.groupBar);
        empty = findViewById(R.id.emptyProjectsText);
        findViewById(R.id.addGroupButton).setOnClickListener(v -> showCreateGroup());
        findViewById(R.id.addProjectButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateProjectActivity.class);
            if (!"全部".equals(selectedGroup)) intent.putExtra("groupName", selectedGroup);
            startActivity(intent);
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            Project project = projects.get(position);
            if (!"ACTIVE".equals(project.status)) {
                Toast.makeText(this, "请先长按并恢复这个计划", Toast.LENGTH_SHORT).show();
                return;
            }
            CurrentProjectStore.select(this, project.id);
            Toast.makeText(this, "已切换到“" + project.name + "”", Toast.LENGTH_SHORT).show();
            finish();
        });
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            manage(projects.get(position));
            return true;
        });
    }

    @Override protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        List<String> groups = groupsWithVirtualItems();
        if (!groups.contains(selectedGroup)) selectedGroup = "全部";
        renderGroups(groups);
        projects = db.listProjects(selectedGroup);
        list.setVisibility(projects.isEmpty() ? View.GONE : View.VISIBLE);
        empty.setVisibility(projects.isEmpty() ? View.VISIBLE : View.GONE);
        list.setAdapter(new ArrayAdapter<Project>(this, 0, projects) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                Project project = getItem(position);
                LinearLayout card = new LinearLayout(ProjectsActivity.this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(18), dp(14), dp(18), dp(14));
                card.setBackgroundResource(R.drawable.card_background);
                card.addView(text(project.name + "  ·  " + UiText.status(project.status), 17, true));
                TextView detail = text("分类：" + project.groupName + "\n" + project.category + " · " + project.mode + "\n目标：" + project.target, 14, false);
                detail.setPadding(0, dp(7), 0, 0);
                card.addView(detail);
                return card;
            }
        });
    }

    private List<String> groupsWithVirtualItems() {
        List<String> groups = new ArrayList<>();
        groups.add("全部");
        groups.addAll(db.listProjectGroups());
        groups.add("未分类");
        return groups;
    }

    private void renderGroups(List<String> groups) {
        groupBar.removeAllViews();
        for (String group : groups) {
            TextView chip = text(group, 14, selectedGroup.equals(group));
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(dp(16), dp(9), dp(16), dp(9));
            chip.setBackgroundResource(selectedGroup.equals(group) ? R.drawable.chip_background : R.drawable.folder_chip_unselected);
            chip.setContentDescription("查看" + group + "计划");
            chip.setOnClickListener(v -> { selectedGroup = group; load(); });
            if (!isProtectedGroup(group)) chip.setOnLongClickListener(v -> { manageGroup(group); return true; });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44));
            params.setMarginEnd(dp(8));
            groupBar.addView(chip, params);
        }
    }

    private void showCreateGroup() {
        EditText input = groupNameInput("");
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("新建计划分类")
                .setMessage("例如：健康、工作日、旅行期间")
                .setView(input).setNegativeButton("取消", null).setPositiveButton("创建", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (!db.createProjectGroup(name)) { input.setError("请输入1至12个字，且不要与现有分类重名"); return; }
            selectedGroup = name;
            dialog.dismiss();
            load();
        }));
        dialog.show();
    }

    private void manageGroup(String group) {
        new AlertDialog.Builder(this).setTitle(group).setItems(new String[]{"重命名", "删除分类"}, (dialog, which) -> {
            if (which == 0) renameGroup(group); else confirmDeleteGroup(group);
        }).show();
    }

    private void renameGroup(String oldName) {
        EditText input = groupNameInput(oldName);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("重命名分类")
                .setView(input).setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (!db.renameProjectGroup(oldName, name)) { input.setError("名称无效或已经存在"); return; }
            selectedGroup = name;
            dialog.dismiss();
            load();
        }));
        dialog.show();
    }

    private void confirmDeleteGroup(String group) {
        new AlertDialog.Builder(this).setTitle("删除“" + group + "”？")
                .setMessage("分类中的计划和历史记录不会删除，它们会移到“未分类”。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除分类", (dialog, which) -> {
                    db.deleteProjectGroup(group);
                    selectedGroup = "未分类";
                    load();
                }).show();
    }

    private EditText groupNameInput(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setHint("分类名称");
        input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        input.setSelectAllOnFocus(true);
        return input;
    }

    private static boolean isProtectedGroup(String group) {
        return "全部".equals(group) || "长期".equals(group) || "短期".equals(group)
                || "临时".equals(group) || "未分类".equals(group);
    }

    private void manage(Project project) {
        String[] actions = "ACTIVE".equals(project.status)
                ? new String[]{"设为当前计划", "编辑计划", "移动到分类", "暂停", "归档"}
                : new String[]{"恢复为进行中", "编辑计划", "移动到分类", "查看历史", "归档"};
        new AlertDialog.Builder(this).setTitle(project.name).setItems(actions, (dialog, which) -> {
            String action = actions[which];
            if ("编辑计划".equals(action)) {
                startActivity(new Intent(this, CreateProjectActivity.class).putExtra("editProjectId", project.id));
                return;
            }
            if ("查看历史".equals(action)) {
                startActivity(new Intent(this, HistoryActivity.class).putExtra("projectId", project.id));
                return;
            }
            if ("移动到分类".equals(action)) {
                moveProject(project);
                return;
            }
            if (action.startsWith("设为")) CurrentProjectStore.select(this, project.id);
            else if (action.startsWith("恢复")) { db.setProjectStatus(project.id, "ACTIVE"); CurrentProjectStore.select(this, project.id); }
            else if ("暂停".equals(action)) db.setProjectStatus(project.id, "PAUSED");
            else db.setProjectStatus(project.id, "ARCHIVED");
            com.example.dailyrecord.notify.ReminderScheduler.cancel(this, project.id);
            com.example.dailyrecord.notify.ReminderScheduler.schedule(this, db.getProject(project.id));
            load();
        }).show();
    }

    private void moveProject(Project project) {
        List<String> groups = groupsWithVirtualItems();
        groups.remove("全部");
        String[] names = groups.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("移动“" + project.name + "”")
                .setItems(names, (dialog, which) -> {
                    db.moveProjectToGroup(project.id, names[which]);
                    Toast.makeText(this, "已移到“" + names[which] + "”", Toast.LENGTH_SHORT).show();
                    load();
                }).show();
    }

    private TextView text(String value, int size, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(getColor(bold ? R.color.text_primary : R.color.text_secondary));
        if (bold) text.setTypeface(null, android.graphics.Typeface.BOLD);
        return text;
    }

    @Override protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
