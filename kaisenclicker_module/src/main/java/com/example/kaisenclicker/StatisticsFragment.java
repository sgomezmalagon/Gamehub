package com.example.kaisenclicker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.kaisenclicker.databinding.FragmentStatisticsBinding;
import com.example.kaisenclicker.persistence.save.GameDataManager;

import java.util.Locale;

public class StatisticsFragment extends Fragment {

    private StatsDatabaseHelper dbHelper;
    private FragmentStatisticsBinding binding;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "KaisenClickerData";
    private long activeUserId = -1L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        dbHelper = new StatsDatabaseHelper(requireContext());
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Reset button uses view binding field
        binding.btnResetStats.setOnClickListener(v -> showResetConfirmation());

        // Ensure text colors and sizes are set defensively
        setInitialTextStyles();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // recover the current user id from SharedPreferences
        activeUserId = prefs.getLong("user_id_active", -1L);
        if (activeUserId <= 0) {
            // no active user, clear UI
            clearUiToZeros();
            return;
        }

        // Query database filtered by usuario_id and use try-with-resources to ensure cursor is closed.
        try (Cursor c = dbHelper.getStatsForUser(activeUserId)) { // implement this method with WHERE usuario_id = ? in helper
            if (c == null || !c.moveToFirst()) {
                // no rows or null cursor for this user
                clearUiToZeros();
                return;
            }

            long totalDamage = c.getLong(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_TOTAL_DAMAGE));
            int enemies = c.getInt(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_ENEMIES));
            int bosses = c.getInt(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_BOSSES));
            int clicks = c.getInt(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_CLICKS));
            int unlocked = c.getInt(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_UNLOCKED));
            int nextProgress = c.getInt(c.getColumnIndexOrThrow(StatsDatabaseHelper.COL_NEXT_PROGRESS));

            animateNumber(binding.tvTotalDamage, totalDamage, 900);
            // updated view binding fields (from fragment_statistics.xml ids: tv_enemies_count, tv_bosses_count)
            animateNumber(binding.tvEnemiesCount, enemies, 700);
            animateNumber(binding.tvBossesCount, bosses, 700);
            animateNumber(binding.tvClicks, clicks, 700);
            // unlocked count may be shown as "x/N"
            binding.tvUnlockedCount.setText(String.format(Locale.getDefault(), "%d/%d", unlocked, GameDataManager.TOTAL_CHARACTERS));

            setProgressAnimated(nextProgress);
        } catch (Exception e) {
            android.util.Log.w("StatisticsFragment", "Error reading stats for user " + activeUserId, e);
            clearUiToZeros();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Close dbHelper if possible and clear binding and references to avoid ghost data
        try {
            if (dbHelper != null) dbHelper.close();
        } catch (Exception ignored) {}
        binding = null;
        prefs = null;
        activeUserId = -1L;
    }

    private void clearUiToZeros() {
        if (binding == null) return;
        // Use string resources to avoid hardcoded, non-translatable literals
        binding.tvTotalDamage.setText(getString(com.example.kaisenclicker.R.string.zero));
        binding.tvDps.setText(getString(com.example.kaisenclicker.R.string.stats_dps_default));
        binding.tvTotalClicksSummary.setText(getString(com.example.kaisenclicker.R.string.stats_total_clicks_default));
        // use new binding fields for enemies/bosses
        binding.tvEnemiesCount.setText("0");
        binding.tvBossesCount.setText("0");
        binding.tvClicks.setText("0");
        binding.tvUnlockedCount.setText(String.format(Locale.getDefault(), "0/%d", GameDataManager.TOTAL_CHARACTERS));
        binding.progressUnlocked.setProgress(0);
        binding.tvUnlockedPercent.setText(String.format(Locale.getDefault(), getString(com.example.kaisenclicker.R.string.percent_format), 0));
    }

    private void setInitialTextStyles() {
        if (binding == null) return;
        // Ensure visibility: numbers in gold, labels in white
        int gold = ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light);
        binding.tvTotalDamage.setTextColor(gold);
        // apply to new binding fields
        binding.tvEnemiesCount.setTextColor(gold);
        binding.tvBossesCount.setTextColor(gold);
        binding.tvClicks.setTextColor(gold);

        binding.tvTotalDamage.setTextSize(28f);
        // make enemy/boss counters large as requested
        binding.tvEnemiesCount.setTextSize(28f);
        binding.tvBossesCount.setTextSize(28f);
        binding.tvClicks.setTextSize(24f);
    }

    private void animateNumber(final android.widget.TextView tv, long to, long duration) {
        if (tv == null) return;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, (float) to);
        animator.setDuration(duration);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            tv.setText(String.valueOf((long) val));
        });
        animator.start();
    }

    private void setProgressAnimated(int percent) {
        if (binding == null) return;
        ValueAnimator animator = ValueAnimator.ofInt(0, Math.max(0, Math.min(100, percent)));
        animator.setDuration(800);
        animator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            binding.progressUnlocked.setProgress(val);
            binding.tvUnlockedPercent.setText(String.format(Locale.getDefault(), "%d%%", val));
        });
        animator.start();
    }

    private void showResetConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Reset stats")
                .setMessage("¿Deseas reiniciar las estadísticas del usuario actual?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    if (activeUserId > 0) {
                        dbHelper.resetStatsForUser(activeUserId); // ensure helper deletes WHERE usuario_id = ?
                        clearUiToZeros();
                        Toast.makeText(requireContext(), "Estadísticas reiniciadas", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

}
