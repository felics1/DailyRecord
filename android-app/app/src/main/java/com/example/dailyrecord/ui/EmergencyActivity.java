package com.example.dailyrecord.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dailyrecord.R;
import com.example.dailyrecord.data.DbHelper;
import com.example.dailyrecord.model.Project;

import java.util.Locale;

public final class EmergencyActivity extends BaseActivity {
    private static final String[] TRIGGERS = {"这次冲动来自……", "无聊", "压力", "疲惫", "情绪低落", "社交影响", "环境触发", "习惯性打开", "其他"};
    private DbHelper db;
    private long projectId;
    private long sessionId = -1;
    private int urge = 5;
    private int waitCount;
    private CountDownTimer timer;
    private boolean completing;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_emergency);
        db = new DbHelper(this);
        projectId = getIntent().getLongExtra("projectId", -1);
        Project project = db.getProject(projectId);
        if (project == null) { finish(); return; }
        sessionId = db.activeImpulse(projectId);
        if (sessionId >= 0) waitCount = db.impulseEventCount(sessionId, "WAIT");
        TextView reason = findViewById(R.id.reasonText);
        reason.setText(project.reason.isEmpty()
                ? "冲动会过去，你只需要处理眼前这一小段时间。"
                : "还记得你是为了什么吗？\n" + project.reason);
        Spinner trigger = findViewById(R.id.emergencyTriggerSpinner);
        trigger.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, TRIGGERS));
        TextView label = findViewById(R.id.intensityLabel);
        ((SeekBar) findViewById(R.id.intensitySeek)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                urge = value;
                label.setText("当前冲动：" + value + " / 10");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        updateWaitCount();
        findViewById(R.id.breatheButton).setOnClickListener(v -> startTimer(60, "BREATHE"));
        findViewById(R.id.waitButton).setOnClickListener(v -> {
            if (waitCount >= 3) {
                Toast.makeText(this, "已经多次延迟了，现在请记录真实状态", Toast.LENGTH_LONG).show();
                return;
            }
            waitCount++;
            updateWaitCount();
            startTimer(300, "WAIT");
        });
        findViewById(R.id.relievedButton).setOnClickListener(v -> finishSession(false, trigger.getSelectedItem().toString()));
        findViewById(R.id.happenedButton).setOnClickListener(v -> finishSession(true, trigger.getSelectedItem().toString()));
    }

    private void ensureSession() {
        if (sessionId < 0) sessionId = db.beginImpulse(projectId, urge);
    }

    private void startTimer(int seconds, String kind) {
        ensureSession();
        db.addImpulseEvent(sessionId, kind, Integer.toString(seconds));
        if (timer != null) timer.cancel();
        Button breathe = findViewById(R.id.breatheButton);
        Button wait = findViewById(R.id.waitButton);
        TextView timerText = findViewById(R.id.timerText);
        breathe.setEnabled(false);
        wait.setEnabled(false);
        timer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override public void onTick(long left) {
                long total = Math.max(0, left / 1000);
                timerText.setText(String.format(Locale.ROOT, "%02d:%02d", total / 60, total % 60));
            }
            @Override public void onFinish() {
                timerText.setText("现在感觉怎么样？");
                breathe.setEnabled(true);
                wait.setEnabled(waitCount < 3);
            }
        }.start();
    }

    private void updateWaitCount() {
        ((TextView) findViewById(R.id.waitCountText)).setText("已延迟 " + waitCount + "/3 次");
        findViewById(R.id.waitButton).setEnabled(waitCount < 3);
    }

    private void finishSession(boolean occurred, String trigger) {
        if (completing) return;
        completing = true;
        ensureSession();
        db.addImpulseEvent(sessionId, "FINAL_URGE", Integer.toString(urge));
        String outcome = occurred ? "LOST" : "RESISTED";
        long recordId = db.addRecord(projectId, outcome, db.planResultFor(projectId, outcome), "", urge,
                trigger.startsWith("这次") ? "" : trigger, "", "来自冲动急救");
        db.finishImpulse(sessionId, recordId, occurred ? "OCCURRED" : "RELIEVED");
        Toast.makeText(this, occurred ? "已如实记录，下一次仍可重新选择" : "你为自己争取了一次选择", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override protected void onDestroy() {
        if (timer != null) timer.cancel();
        db.close();
        super.onDestroy();
    }
}
