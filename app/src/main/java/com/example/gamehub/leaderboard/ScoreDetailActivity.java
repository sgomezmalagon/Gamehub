package com.example.gamehub.leaderboard;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.gamehub.BaseActivity;
import com.example.gamehub.R;
import com.example.gamehub.auth.SessionManager;
import com.example.kaisenclicker.persistence.save.SqlRepository;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScoreDetailActivity extends BaseActivity {
    public static final String EXTRA_SCORE_ID = "extra_score_id";
    private SqlRepository repo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_detail);

        // Botón atrás
        ImageView btnBack = findViewById(R.id.btnBackDetail);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Usar BD per-user
        SessionManager session = new SessionManager(this);
        String username = session.getUsername();
        String dbName = (username != null && !username.isEmpty())
                ? "kaisen_clicker_" + username + ".db"
                : "kaisen_clicker.db";
        repo = new SqlRepository(getApplicationContext(), dbName);

        long id = getIntent().getLongExtra(EXTRA_SCORE_ID, -1);
        if (id <= 0) {
            finish();
            return;
        }

        Cursor c = repo.getScoreById(id);
        if (c == null) {
            finish();
            return;
        }

        if (c.moveToFirst()) {
            String player = c.getString(c.getColumnIndexOrThrow("player_name"));
            long score = c.getLong(c.getColumnIndexOrThrow("score_value"));
            long created = c.getLong(c.getColumnIndexOrThrow("created_at"));

            // game_name column (may not exist in old DBs)
            String gameName = null;
            int gameNameIdx = c.getColumnIndex("game_name");
            if (gameNameIdx >= 0 && !c.isNull(gameNameIdx)) {
                gameName = c.getString(gameNameIdx);
            }

            // Fallback: try to parse game from extra JSON
            if (gameName == null || gameName.isEmpty()) {
                int extraIdx = c.getColumnIndex("extra");
                if (extraIdx >= 0 && !c.isNull(extraIdx)) {
                    String extra = c.getString(extraIdx);
                    try {
                        JSONObject json = new JSONObject(extra);
                        gameName = json.optString("game", null);
                    } catch (Exception ignored) {}
                }
            }

            if (gameName == null || gameName.isEmpty()) {
                gameName = getString(R.string.game_unknown);
            }

            // Formatear fecha
            String formattedDate;
            try {
                formattedDate = new SimpleDateFormat("dd/MM/yyyy HH:mm",
                        Locale.getDefault()).format(new Date(created * 1000L));
            } catch (Exception e) {
                formattedDate = String.valueOf(created);
            }

            // Mostrar datos
            TextView tvPlayer = findViewById(R.id.tvDetailPlayer);
            TextView tvScore = findViewById(R.id.tvDetailScore);
            TextView tvDate = findViewById(R.id.tvDetailDate);
            TextView tvGame = findViewById(R.id.tvDetailGame);

            tvPlayer.setText(player);
            tvScore.setText(String.valueOf(score));
            tvDate.setText(formattedDate);
            if (tvGame != null) {
                tvGame.setText(gameName);
            }
        }
        c.close();

        animateEntrance();
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

