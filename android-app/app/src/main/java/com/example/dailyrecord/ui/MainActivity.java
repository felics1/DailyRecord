package com.example.dailyrecord.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.dailyrecord.R;
import com.example.dailyrecord.data.CurrentProjectStore;
import com.example.dailyrecord.data.DbHelper;
import com.example.dailyrecord.domain.NextStepAdvisor;
import com.example.dailyrecord.model.Project;
import com.example.dailyrecord.model.Stage;
import com.example.dailyrecord.model.Stats;

public final class MainActivity extends BaseActivity {
    private static final long DAY = 86_400_000L;
    private DbHelper db;
    private Project project;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        db = new DbHelper(this);
        ((TextView) findViewById(R.id.dateText)).setText(UiText.date(System.currentTimeMillis()));
        findViewById(R.id.createProjectButton).setOnClickListener(v -> open(CreateProjectActivity.class));
        findViewById(R.id.historyButton).setOnClickListener(v -> openForProject(HistoryActivity.class));
        findViewById(R.id.checkInButton).setOnClickListener(v -> openForProject(CheckInActivity.class));
        findViewById(R.id.emergencyButton).setOnClickListener(v -> {
            if (project == null) open(CreateProjectActivity.class);
            else openForProject(EmergencyActivity.class);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        project = CurrentProjectStore.load(this, db);
        render();
    }

    private void render() {
        TextView category = findViewById(R.id.categoryText);
        TextView name = findViewById(R.id.projectName);
        TextView stageText = findViewById(R.id.stageText);
        TextView target = findViewById(R.id.targetText);
        TextView reason = findViewById(R.id.reasonText);
        TextView statsText = findViewById(R.id.statsText);
        ProgressBar progress = findViewById(R.id.stageProgress);
        View activeActions = findViewById(R.id.activeActions);
        View statsCard = findViewById(R.id.statsCard);
        Button emergency = findViewById(R.id.emergencyButton);

        if (project == null) {
            renderNextStep(null);
            category.setText("从一个计划开始");
            name.setText("还没有进行中的计划");
            stageText.setText("创建计划后，应用会帮你拆分阶段、记录诱因并进行复盘。");
            target.setText("不是打卡比赛，而是逐步建立可执行的改变。");
            reason.setText("");
            progress.setProgress(0);
            activeActions.setVisibility(View.GONE);
            statsCard.setVisibility(View.GONE);
            emergency.setText("创建我的第一个计划");
            return;
        }

        category.setText(project.category + " · " + project.mode);
        name.setText(project.name);
        Stage stage = db.currentStage(project.id);
        if (stage == null) {
            stageText.setText("计划已建立");
            target.setText("目标：" + project.target);
            progress.setProgress(0);
        } else {
            int day = Math.max(1, (int) ((System.currentTimeMillis() - stage.startsAt) / DAY) + 1);
            day = Math.min(day, stage.durationDays);
            stageText.setText(stage.title + " · 第 " + day + "/" + stage.durationDays + " 天");
            target.setText(stage.target);
            progress.setProgress(Math.round(day * 100f / stage.durationDays));
        }
        reason.setText(project.reason.isEmpty() ? "先把今天过好，不要求一次做到完美。" : "为了：" + project.reason);
        Stats stats = db.stats(project.id, 7);
        renderNextStep(stats);
        ((TextView) findViewById(R.id.recordCount)).setText(Integer.toString(stats.total));
        ((TextView) findViewById(R.id.successRate)).setText(stats.withinPlan + stats.exceeded == 0 ? "—" : stats.successPercent() + "%");
        statsText.setText(stats.total == 0
                ? "还没有记录。第一次诚实记录，就是计划真正开始。"
                : stats.total < 3 ? "记录还较少，继续观察，暂不作趋势判断。" : "常见诱因：" + stats.topTrigger + " · 每一次记录都帮助你看清规律");
        activeActions.setVisibility(View.VISIBLE);
        statsCard.setVisibility(View.VISIBLE);
        emergency.setText("我现在有冲动");
    }

    private void renderNextStep(Stats stats) {
        int total = stats == null ? 0 : stats.total;
        int phase = NextStepAdvisor.phase(project != null, total);
        TextView path = findViewById(R.id.journeyPathText);
        TextView title = findViewById(R.id.nextStepTitle);
        TextView body = findViewById(R.id.nextStepBody);
        Button action = findViewById(R.id.nextStepButton);
        path.setText("第 " + phase + " 步 / 4　·　计划 → 记录 → 执行 → 复盘");
        if (phase == 1) {
            title.setText("先制定一个可执行的计划");
            body.setText("选择想改变的行为、当前情况和最终目标，应用会自动拆成小阶段。");
            action.setText("开始制定计划");
            action.setOnClickListener(v -> open(CreateProjectActivity.class));
        } else if (phase == 2) {
            title.setText("先观察，不急着做到完美");
            body.setText("再完成 " + (3 - total) + " 次真实记录，先看清诱因和发生规律。");
            action.setText("记录现在的情况");
            action.setOnClickListener(v -> openForProject(CheckInActivity.class));
        } else if (phase == 3) {
            title.setText("按当前阶段稳定执行");
            body.setText("已经积累 " + total + " 次记录。继续按阶段目标行动，偏离也照常记录。");
            action.setText("记录今天");
            action.setOnClickListener(v -> openForProject(CheckInActivity.class));
        } else {
            title.setText("用记录调整下一阶段");
            body.setText("已经有 " + total + " 次记录，可以复盘规律，再决定是否推进阶段。");
            action.setText("查看本周期复盘");
            action.setOnClickListener(v -> openForProject(ReviewActivity.class));
        }
    }

    private void open(Class<?> screen) {
        startActivity(new Intent(this, screen));
    }

    private void openForProject(Class<?> screen) {
        if (project == null) {
            open(CreateProjectActivity.class);
            return;
        }
        startActivity(new Intent(this, screen).putExtra("projectId", project.id));
    }

    @Override protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
