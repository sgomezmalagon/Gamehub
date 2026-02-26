package com.example.gamehub.leaderboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.gamehub.BaseActivity;
import com.example.gamehub.R;
import com.example.gamehub.auth.SessionManager;
import com.example.kaisenclicker.persistence.save.GameDataManager;
import com.example.kaisenclicker.persistence.save.SqlRepository;

import java.util.Locale;

/**
 * Pantalla de puntuaciones con tabs para seleccionar entre Kaisen Clicker y 2048.
 * Kaisen Clicker: muestra nivel enemigo, clicks totales, daño, enemigos derrotados, etc.
 * 2048: muestra puntuación actual, mejor puntuación, movimientos, tiempo.
 */
public class LeaderboardActivity extends BaseActivity {

    private TextView tabKaisen, tab2048;
    private ScrollView layoutKaisen, layout2048;
    private boolean showingKaisen = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // Botón atrás
        ImageView btnBack = findViewById(R.id.btnBackLeaderboard);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Tabs
        tabKaisen = findViewById(R.id.tabKaisenClicker);
        tab2048 = findViewById(R.id.tab2048);
        layoutKaisen = findViewById(R.id.layoutKaisen);
        layout2048 = findViewById(R.id.layout2048);

        tabKaisen.setOnClickListener(v -> showTab(true));
        tab2048.setOnClickListener(v -> showTab(false));

        // Botón "Ver historial de puntuaciones"
        TextView btnViewAll = findViewById(R.id.btnViewAllScores);
        if (btnViewAll != null) {
            btnViewAll.setOnClickListener(v ->
                    startActivity(new Intent(this, ScoresListActivity.class)));
        }

        // Cargar datos
        loadKaisenData();
        load2048Data();
        showTab(true);

        animateEntrance();
    }

    private void showTab(boolean kaisen) {
        showingKaisen = kaisen;

        if (kaisen) {
            layoutKaisen.setVisibility(View.VISIBLE);
            layout2048.setVisibility(View.GONE);
            tabKaisen.setTextColor(getColor(R.color.white));
            tabKaisen.setBackgroundResource(R.drawable.bg_tab_active);
            tab2048.setTextColor(getColor(R.color.hub_text_hint));
            tab2048.setBackgroundResource(R.drawable.bg_menu_option);
        } else {
            layoutKaisen.setVisibility(View.GONE);
            layout2048.setVisibility(View.VISIBLE);
            tab2048.setTextColor(getColor(R.color.white));
            tab2048.setBackgroundResource(R.drawable.bg_tab_active);
            tabKaisen.setTextColor(getColor(R.color.hub_text_hint));
            tabKaisen.setBackgroundResource(R.drawable.bg_menu_option);
        }
    }

    private void loadKaisenData() {
        SessionManager session = new SessionManager(this);
        String username = session.getUsername();

        try {
            GameDataManager gdm = new GameDataManager(this, username);

            TextView tvEnemyLevel = findViewById(R.id.tvKaisenEnemyLevel);
            TextView tvTotalClicks = findViewById(R.id.tvKaisenTotalClicks);
            TextView tvTotalDamage = findViewById(R.id.tvKaisenTotalDamage);
            TextView tvEnemiesDefeated = findViewById(R.id.tvKaisenEnemiesDefeated);
            TextView tvBossesDefeated = findViewById(R.id.tvKaisenBossesDefeated);
            TextView tvCursedEnergy = findViewById(R.id.tvKaisenCursedEnergy);
            TextView tvCharLevel = findViewById(R.id.tvKaisenCharLevel);
            TextView tvTimePlayed = findViewById(R.id.tvKaisenTimePlayed);

            tvEnemyLevel.setText(String.valueOf(gdm.getEnemyLevel()));
            tvTotalClicks.setText(formatNumber(gdm.getTotalClicks()));
            tvTotalDamage.setText(formatNumber(gdm.getTotalDamage()));
            tvEnemiesDefeated.setText(formatNumber(gdm.getEnemiesDefeated()));
            tvBossesDefeated.setText(formatNumber(gdm.getBossesDefeated()));
            tvCursedEnergy.setText(formatNumber(gdm.getCursedEnergy()));
            tvCharLevel.setText(String.valueOf(gdm.getCharacterLevel()));
            tvTimePlayed.setText(formatTime(gdm.getTotalPlaySeconds()));

        } catch (Exception e) {
            // Si no hay datos, mostrar valores por defecto
            setDefaultKaisenValues();
        }
    }

    private void setDefaultKaisenValues() {
        TextView tvEnemyLevel = findViewById(R.id.tvKaisenEnemyLevel);
        TextView tvTotalClicks = findViewById(R.id.tvKaisenTotalClicks);
        TextView tvTotalDamage = findViewById(R.id.tvKaisenTotalDamage);
        TextView tvEnemiesDefeated = findViewById(R.id.tvKaisenEnemiesDefeated);
        TextView tvBossesDefeated = findViewById(R.id.tvKaisenBossesDefeated);
        TextView tvCursedEnergy = findViewById(R.id.tvKaisenCursedEnergy);
        TextView tvCharLevel = findViewById(R.id.tvKaisenCharLevel);
        TextView tvTimePlayed = findViewById(R.id.tvKaisenTimePlayed);

        String zero = "0";
        tvEnemyLevel.setText("1");
        tvTotalClicks.setText(zero);
        tvTotalDamage.setText(zero);
        tvEnemiesDefeated.setText(zero);
        tvBossesDefeated.setText(zero);
        tvCursedEnergy.setText(zero);
        tvCharLevel.setText("1");
        tvTimePlayed.setText("00:00:00");
    }

    private void load2048Data() {
        SessionManager session = new SessionManager(this);
        String username = session.getUsername();
        if (username == null) username = "player1";

        // Leer datos del 2048 desde la BD global (kaisen_clicker_<user>.db)
        String dbName = "kaisen_clicker_" + username + ".db";

        try {
            SqlRepository repo = new SqlRepository(this, dbName);

            int score = repo.getInt("2048_score", 0);
            int bestScore = repo.getInt("2048_best_score", 0);
            int moves = repo.getInt("2048_moves", 0);
            int seconds = repo.getInt("2048_seconds", 0);

            TextView tv2048Score = findViewById(R.id.tv2048Score);
            TextView tv2048BestScore = findViewById(R.id.tv2048BestScore);
            TextView tv2048Moves = findViewById(R.id.tv2048Moves);
            TextView tv2048Time = findViewById(R.id.tv2048Time);

            tv2048Score.setText(formatNumber(score));
            tv2048BestScore.setText(formatNumber(bestScore));
            tv2048Moves.setText(formatNumber(moves));
            tv2048Time.setText(formatTime(seconds));

        } catch (Exception e) {
            setDefault2048Values();
        }
    }

    private void setDefault2048Values() {
        TextView tv2048Score = findViewById(R.id.tv2048Score);
        TextView tv2048BestScore = findViewById(R.id.tv2048BestScore);
        TextView tv2048Moves = findViewById(R.id.tv2048Moves);
        TextView tv2048Time = findViewById(R.id.tv2048Time);

        String zero = "0";
        tv2048Score.setText(zero);
        tv2048BestScore.setText(zero);
        tv2048Moves.setText(zero);
        tv2048Time.setText("00:00");
    }

    // ── Formateadores ──

    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format(Locale.getDefault(), "%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format(Locale.getDefault(), "%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    private String formatNumber(int number) {
        return formatNumber((long) number);
    }

    private String formatTime(long totalSeconds) {
        if (totalSeconds <= 0) return "00:00";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
    }

    private void animateEntrance() {
        View root = findViewById(android.R.id.content);
        root.setAlpha(0f);
        root.animate()
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }
}

