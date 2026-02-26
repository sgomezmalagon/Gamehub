package com.example.kaisenclicker.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.kaisenclicker.R;
import com.example.kaisenclicker.databinding.FragmentStatisticsBinding;
import com.example.kaisenclicker.persistence.save.GameDataManager;
import com.example.kaisenclicker.ui.activities.MainActivity;

import java.text.NumberFormat;
import java.util.Locale;

public class StatisticsFragment extends Fragment {

    private static final String TAG = "StatisticsFragment";

    private FragmentStatisticsBinding binding;
    private GameDataManager gameDataManager;

    // Views (looked up from binding.getRoot())
    private TextView tvDpsView;
    private TextView tvTotalDamageView;
    private TextView tvTotalClicksSmallView;
    private TextView tvEnemiesDefeatedView;
    private TextView tvBossesDefeatedView;
    private TextView tvTotalClicksBigView;
    private TextView tvCharactersUnlockedView;
    private ProgressBar progressUnlockedView;
    private TextView tvUnlockedPercentView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        } catch (Exception e) {
            Log.e(TAG, "Error inflating fragment_statistics layout", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error al abrir Estadísticas (ver logs)", Toast.LENGTH_LONG).show();
            }
            return new View(requireContext());
        }

        if (getActivity() instanceof MainActivity) {
            gameDataManager = ((MainActivity) getActivity()).getGameDataManager();
        }

        // Ensure there is an active user id in SharedPreferences. If none, default to 1 so per-user keys work.
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("KaisenClickerData", Context.MODE_PRIVATE);
            long uid = prefs.getLong("user_id_active", -1L);
            if (uid <= 0) {
                prefs.edit().putLong("user_id_active", 1L).apply();
                Log.i(TAG, "No user_id_active found, defaulting to user 1 for persistence tests");
            } else {
                Log.i(TAG, "Active user id (prefs) = " + uid);
            }
        } catch (Exception ignored) {}

        // lookup views from the inflated root instead of relying on generated binding fields
        // prefer binding fields generated from fragment_statistics.xml
        View root = binding.getRoot();
        ImageButton btnResetLocal = binding.btnResetStats;
        tvDpsView = binding.tvDps;
        tvTotalDamageView = binding.tvTotalDamage;
        tvTotalClicksSmallView = binding.tvTotalClicksSummary;
        // use the new IDs (tv_enemies_count / tv_bosses_count) present in fragment_statistics.xml
        tvEnemiesDefeatedView = binding.tvEnemiesCount;
        tvBossesDefeatedView = binding.tvBossesCount;
        tvTotalClicksBigView = binding.tvClicks;
        tvCharactersUnlockedView = binding.tvUnlockedCount;
        progressUnlockedView = binding.progressUnlocked;
        tvUnlockedPercentView = binding.tvUnlockedPercent;
        // Note: enemy-level controls (tv_enemy_level, btn_enemy_inc/dec) were removed from layout
        // so we intentionally do not reference them here.

        // Reset button
        if (btnResetLocal != null) {
            btnResetLocal.setOnClickListener(v -> {
                if (gameDataManager != null) {
                    // resetAllData() resetea los datos (incluye estadísticas).
                    gameDataManager.resetAllData();
                    refreshStats();
                }
            });
        }

        // Add press effects to available views (safely)
        addPressEffect(btnResetLocal);
        addPressEffect(tvEnemiesDefeatedView);
        addPressEffect(tvBossesDefeatedView);
        addPressEffect(tvTotalClicksBigView);
        addPressEffect(tvCharactersUnlockedView);

        refreshStats();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void refreshStats() {
        if (gameDataManager == null || binding == null) return;

        long totalDamage = gameDataManager.getTotalDamage();
        int totalClicks = gameDataManager.getTotalClicks();
        double dps = gameDataManager.getAverageDps();
        double peakDps = gameDataManager.getPeakDps();
        int enemies = gameDataManager.getEnemiesDefeated();
        int bosses = gameDataManager.getBossesDefeated();
        int chars = gameDataManager.getCharactersUnlockedCount();

        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
        String totalDamageStr = nf.format(totalDamage);
        String totalClicksStr = nf.format(totalClicks);
        String enemiesStr = nf.format(enemies);
        String bossesStr = nf.format(bosses);
        String charsStr = nf.format(chars);

        try {
            if (tvDpsView != null) {
                // Mostrar DPS promedio y el pico más alto registrado
                String dpsText = String.format(Locale.getDefault(), "DPS: %.1f  |  🏆 Pico: %.1f", dps, peakDps);
                tvDpsView.setText(dpsText);
            }
            if (tvTotalDamageView != null) {
                tvTotalDamageView.setText(totalDamageStr);
                tvTotalDamageView.setTextColor(0xFFFFD700);
            }
            // small label summary (keep as label with value)
            if (tvTotalClicksSmallView != null) tvTotalClicksSmallView.setText(getString(R.string.stats_total_clicks, totalClicksStr));

            // Numbers only for the cards — ensure we set only the numeric values and log them
            if (tvEnemiesDefeatedView != null) {
                tvEnemiesDefeatedView.setText(enemiesStr);
                tvEnemiesDefeatedView.setTextColor(0xFFFFD700);
                Log.d("STATS_CHECK", "Enemigos: " + enemies);
            }
            if (tvBossesDefeatedView != null) {
                tvBossesDefeatedView.setText(bossesStr);
                tvBossesDefeatedView.setTextColor(0xFFFFD700);
                Log.d("STATS_CHECK", "Bosses: " + bosses);
            }
            if (tvTotalClicksBigView != null) {
                tvTotalClicksBigView.setText(totalClicksStr);
                tvTotalClicksBigView.setTextColor(0xFFFFD700);
                Log.d("STATS_CHECK", "Clicks: " + totalClicks);
            }
            if (tvCharactersUnlockedView != null) {
                // show as "unlocked/total"
                String unlockedText = String.format(Locale.getDefault(), "%s/%d", charsStr, GameDataManager.TOTAL_CHARACTERS);
                tvCharactersUnlockedView.setText(unlockedText);
                tvCharactersUnlockedView.setTextColor(0xFFFFD700);
                Log.d("STATS_CHECK", "Unlocked: " + unlockedText);
            }

            // progress and percent
            if (progressUnlockedView != null) {
                int totalChars = Math.max(1, GameDataManager.TOTAL_CHARACTERS);
                int percent = Math.min(100, (gameDataManager.getCharactersUnlockedCount() * 100) / totalChars);
                progressUnlockedView.setProgress(percent);
            }
            if (tvUnlockedPercentView != null) tvUnlockedPercentView.setText(getString(R.string.percent_format,  (progressUnlockedView != null ? progressUnlockedView.getProgress() : 0)));

        } catch (Exception e) {
            Log.e(TAG, "Error updating stats UI", e);
        }
    }

    private void addPressEffect(View v) {
        if (v == null) return;
        final float scaleDown = 0.97f;
        final long duration = 90;
        v.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(scaleDown).scaleY(scaleDown).setDuration(duration).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(duration).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        // Llamar performClick inmediatamente para cumplir con accesibilidad/lint
                        view.performClick();
                    }
                    break;
            }
            return false; // allow ripple and click events to continue
        });
    }
}
