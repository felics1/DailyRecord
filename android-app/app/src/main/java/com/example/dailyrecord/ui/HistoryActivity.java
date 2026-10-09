package com.example.dailyrecord.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dailyrecord.R;
import com.example.dailyrecord.data.DbHelper;
import com.example.dailyrecord.model.Project;
import com.example.dailyrecord.model.RecordItem;

import java.util.List;

public final class HistoryActivity extends BaseActivity {
    private DbHelper db;
    private long projectId;
    private List<RecordItem> records;
    private ListView list;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_history);
        db = new DbHelper(this);
        projectId = getIntent().getLongExtra("projectId", -1);
        Project project = db.getProject(projectId);
        if (project == null) { finish(); return; }
        ((TextView) findViewById(R.id.historySubtitle)).setText(project.name + " · 点击修改，长按删除误操作");
        list = findViewById(R.id.historyList);
        list.setOnItemClickListener((parent, view, position, id) -> startActivity(
                new Intent(this, CheckInActivity.class).putExtra("projectId", projectId)
                        .putExtra("editRecordId", records.get(position).id)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            RecordItem item = records.get(position);
            new AlertDialog.Builder(this).setTitle("删除这条记录？")
                    .setMessage("删除后统计会同步更新。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除", (dialog, which) -> { db.deleteRecord(item.id); load(); }).show();
            return true;
        });
        load();
    }

    private void load() {
        records = db.listRecords(projectId, 100);
        list.setAdapter(new ArrayAdapter<RecordItem>(this, 0, records) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                RecordItem item = getItem(position);
                LinearLayout card = new LinearLayout(HistoryActivity.this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(18), dp(14), dp(18), dp(14));
                card.setBackgroundResource(R.drawable.card_background);
                TextView title = text(UiText.outcome(item.outcome) + "  ·  " + UiText.dateTime(item.occurredAt), 16, true);
                String detail = "冲动 " + item.urge + "/10";
                if (!item.actualValue.isEmpty()) detail += "  ·  实际 " + item.actualValue;
                if (!item.trigger.isEmpty()) detail += "\n诱因：" + item.trigger;
                if (!item.mood.isEmpty()) detail += "  ·  情绪：" + item.mood;
                if (!item.note.isEmpty()) detail += "\n" + item.note;
                TextView body = text(detail, 14, false);
                body.setPadding(0, dp(7), 0, 0);
                card.addView(title);
                card.addView(body);
                return card;
            }
        });
        if (records.isEmpty()) Toast.makeText(this, "还没有记录", Toast.LENGTH_SHORT).show();
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
