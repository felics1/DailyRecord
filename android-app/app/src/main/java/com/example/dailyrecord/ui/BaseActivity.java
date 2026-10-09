package com.example.dailyrecord.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import com.example.dailyrecord.R;
import com.example.dailyrecord.data.CurrentProjectStore;
import com.example.dailyrecord.data.DbHelper;
import com.example.dailyrecord.model.Project;

/** Common navigation and window insets; screen logic stays in each Activity. */
public abstract class BaseActivity extends Activity {
    @Override public void setContentView(int layoutResId) {
        View screen = getLayoutInflater().inflate(layoutResId, null);
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(getColor(R.color.background));
        if (!isTopLevel()) {
            LinearLayout toolbar = new LinearLayout(this);
            toolbar.setPadding(dp(20), dp(8), dp(20), 0);
            Button back = new Button(this);
            back.setText("返回");
            back.setTextSize(13);
            back.setTextColor(getColor(R.color.text_secondary));
            back.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            back.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_back, 0, 0, 0);
            back.setCompoundDrawablePadding(dp(4));
            back.setContentDescription("返回上一页");
            back.setOnClickListener(v -> finish());
            toolbar.addView(back, new LinearLayout.LayoutParams(dp(96), dp(48)));
            Button rescue = new Button(this);
            rescue.setText("急救");
            rescue.setTextSize(13);
            rescue.setTextColor(getColor(R.color.primary));
            rescue.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            rescue.setOnClickListener(v -> {
                try (com.example.dailyrecord.data.DbHelper db = new com.example.dailyrecord.data.DbHelper(this)) {
                    com.example.dailyrecord.model.Project p = com.example.dailyrecord.data.CurrentProjectStore.load(this, db);
                    if (p != null) startActivity(new Intent(this, EmergencyActivity.class).putExtra("projectId", p.id));
                    else android.widget.Toast.makeText(this, "请先创建一个计划", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
            View spacer = new View(this);
            toolbar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
            toolbar.addView(rescue, new LinearLayout.LayoutParams(dp(72), dp(48)));
            shell.addView(toolbar);
        }
        shell.addView(screen, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        if (isTopLevel()) addBottomNavigation(shell);
        super.setContentView(shell);
        if (Build.VERSION.SDK_INT >= 27) getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (Build.VERSION.SDK_INT >= 35) {
            shell.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
            shell.requestApplyInsets();
        }
    }

    private boolean isTopLevel() {
        return this instanceof MainActivity || this instanceof ProjectsActivity
                || this instanceof HistoryActivity || this instanceof ReviewActivity;
    }

    private void addBottomNavigation(LinearLayout shell) {
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(5), dp(8), dp(7));
        nav.setBackgroundColor(getColor(R.color.surface));
        nav.setElevation(dp(8));
        nav.addView(navButton(R.string.home_button_text, R.drawable.ic_home, MainActivity.class));
        nav.addView(navButton(R.string.projects_button_text, R.drawable.ic_plan, ProjectsActivity.class));
        nav.addView(navButton(R.string.history_nav_button_text, R.drawable.ic_history, HistoryActivity.class));
        nav.addView(navButton(R.string.review_button_text, R.drawable.ic_review, ReviewActivity.class));
        nav.addView(navButton(R.string.rescue_bottom_button_text, R.drawable.ic_spark, EmergencyActivity.class));
        shell.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(74)));
    }

    private Button navButton(int text, int icon, Class<?> target) {
        boolean selected = target.isInstance(this);
        boolean rescue = target == EmergencyActivity.class;
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(10);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(2), dp(4), dp(2), dp(2));
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setCompoundDrawablesRelativeWithIntrinsicBounds(0, icon, 0, 0);
        button.setCompoundDrawablePadding(dp(2));
        int color = getColor(rescue ? R.color.rescue : selected ? R.color.primary : R.color.text_secondary);
        button.setTextColor(color);
        button.setCompoundDrawableTintList(ColorStateList.valueOf(color));
        button.setBackgroundResource(selected ? R.drawable.nav_item_selected : android.R.color.transparent);
        button.setStateListAnimator(null);
        button.setContentDescription(getString(text));
        button.setOnClickListener(v -> openTab(target));
        button.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return button;
    }

    private void openTab(Class<?> target) {
        if (target.isInstance(this)) return;
        Intent intent = new Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (target == HistoryActivity.class || target == ReviewActivity.class || target == EmergencyActivity.class) {
            try (DbHelper db = new DbHelper(this)) {
                Project project = CurrentProjectStore.load(this, db);
                if (project == null) {
                    startActivity(new Intent(this, CreateProjectActivity.class));
                    return;
                }
                intent.putExtra("projectId", project.id);
            }
        }
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    protected int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
