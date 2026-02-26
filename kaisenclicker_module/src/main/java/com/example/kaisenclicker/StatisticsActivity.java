package com.example.kaisenclicker;

import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;

public class StatisticsActivity extends AppCompatActivity {

    private StatsDatabaseHelper dbHelper;

    private TextView tvTotalDamage, tvDps, tvEnemies, tvBosses, tvClicks, tvUnlocked, tvProgressPercent;
    private ProgressBar progressNext;

    private MaterialButton btnReset;

    // Simulated active user id - in a real app obtain this from session/auth manager
    private final long activeUserId = 1L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        dbHelper = new StatsDatabaseHelper(this);

        tvTotalDamage = findViewById(R.id.tv_total_damage);
        tvDps = findViewById(R.id.tv_dps);
        tvEnemies = findViewById(R.id.tv_enemies);
        tvBosses = findViewById(R.id.tv_bosses);
        tvClicks = findViewById(R.id.tv_clicks);
        tvUnlocked = findViewById(R.id.tv_unlocked);
        tvProgressPercent = findViewById(R.id.tv_progress_percent);
        progressNext = findViewById(R.id.progress_next);
        btnReset = findViewById(R.id.btn_reset);

        // Ensure the user has a stats row
        dbHelper.ensureUserStats(activeUserId);

        loadAndAnimateStats();

        btnReset.setOnClickListener(v -> showResetConfirmation());
    }

    private void loadAndAnimateStats() {
        Cursor c = dbHelper.getStatsForUser(activeUserId);
        if (c != null && c.moveToFirst()) {
            long totalDamage = c.getLong(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_TOTAL_DAMAGE));
            double dps = c.getDouble(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_DPS));
            int enemies = c.getInt(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_ENEMIES));
            int bosses = c.getInt(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_BOSSES));
            int clicks = c.getInt(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_CLICKS));
            int unlocked = c.getInt(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_UNLOCKED));
            int nextProgress = c.getInt(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_NEXT_PROGRESS));

            animateNumber(tvTotalDamage, 0, totalDamage, 800);
            animateNumber(tvDps, 0, (long) dps, 700, true);
            animateNumber(tvEnemies, 0, enemies, 600);
            animateNumber(tvBosses, 0, bosses, 600);
            animateNumber(tvClicks, 0, clicks, 600);
            animateNumber(tvUnlocked, 0, unlocked, 600);
            setProgressAnimated(nextProgress);

            c.close();
        }
    }

    private void animateNumber(final TextView tv, long from, long to, long duration) {
        animateNumber(tv, from, to, duration, false);
    }

    private void animateNumber(final TextView tv, long from, long to, long duration, boolean isDecimal) {
        ValueAnimator animator = ValueAnimator.ofFloat((float) from, (float) to);
        animator.setDuration(duration);
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            if (isDecimal) {
                DecimalFormat df = new DecimalFormat("0.##");
                tv.setText(df.format(val));
            } else {
                tv.setText(String.valueOf((long) val));
            }
        });
        animator.start();
    }

    private void setProgressAnimated(int percent) {
        ValueAnimator animator = ValueAnimator.ofInt(0, percent);
        animator.setDuration(700);
        animator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            progressNext.setProgress(val);
            String formatted = getString(R.string.charge_percentage_local, val);
            tvProgressPercent.setText(formatted);
        });
        animator.start();
    }

    private void showResetConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_reset_stats)
                .setMessage("¿Estás seguro que deseas reiniciar las estadísticas para el usuario actual?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    dbHelper.resetStatsForUser(activeUserId);
                    loadAndAnimateStats();
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemNavigationBar();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemNavigationBar();
        }
    }

    private void hideSystemNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

}
