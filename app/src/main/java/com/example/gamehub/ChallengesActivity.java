package com.example.gamehub;

import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamehub.adapter.ChallengeAdapter;
import com.example.gamehub.auth.SessionManager;
import com.example.gamehub.model.Challenge;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de desafíos/logros del GameHub.
 * Muestra retos que el usuario puede completar para ganar puntos.
 */
public class ChallengesActivity extends BaseActivity {

    private RecyclerView rvChallenges;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenges);

        sessionManager = new SessionManager(this);

        rvChallenges = findViewById(R.id.rvChallenges);
        rvChallenges.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadChallenges();
        animateEntrance();
    }

    private void loadChallenges() {
        List<Challenge> challenges = new ArrayList<>();
        int gamesPlayed = sessionManager.getGamesPlayed();
        int totalPoints = sessionManager.getTotalPoints();

        // Desafío 1: Primera partida
        challenges.add(new Challenge(
                "first_game",
                "Primer Paso",
                "Juega tu primera partida en el GameHub",
                R.drawable.ic_star,
                1,
                Math.min(gamesPlayed, 1),
                50
        ));

        // Desafío 2: Jugar 5 partidas
        challenges.add(new Challenge(
                "five_games",
                "Jugador Habitual",
                "Juega 5 partidas en total",
                R.drawable.ic_gamehub_logo,
                5,
                Math.min(gamesPlayed, 5),
                100
        ));

        // Desafío 3: Jugar 10 partidas
        challenges.add(new Challenge(
                "ten_games",
                "Veterano",
                "Juega 10 partidas en total",
                R.drawable.ic_trophy,
                10,
                Math.min(gamesPlayed, 10),
                200
        ));

        // Desafío 4: Conseguir 100 puntos
        challenges.add(new Challenge(
                "hundred_points",
                "Primeros Puntos",
                "Acumula 100 puntos en total",
                R.drawable.ic_points,
                100,
                Math.min(totalPoints, 100),
                50
        ));

        // Desafío 5: Conseguir 1000 puntos
        challenges.add(new Challenge(
                "thousand_points",
                "Mil Puntos",
                "Acumula 1.000 puntos en total",
                R.drawable.ic_points,
                1000,
                Math.min(totalPoints, 1000),
                150
        ));

        // Desafío 6: Conseguir 5000 puntos
        challenges.add(new Challenge(
                "five_thousand_points",
                "Maestro del GameHub",
                "Acumula 5.000 puntos en total",
                R.drawable.ic_trophy,
                5000,
                Math.min(totalPoints, 5000),
                500
        ));

        // Desafío 7: Jugar 25 partidas
        challenges.add(new Challenge(
                "twentyfive_games",
                "Adicto al Juego",
                "Juega 25 partidas en total",
                R.drawable.ic_challenge,
                25,
                Math.min(gamesPlayed, 25),
                300
        ));

        // Desafío 8: Probar todos los juegos
        challenges.add(new Challenge(
                "try_all_games",
                "Explorador",
                "Juega al menos 2 juegos diferentes",
                R.drawable.ic_gamehub_logo,
                2,
                Math.min(gamesPlayed, 2),
                100
        ));

        ChallengeAdapter adapter = new ChallengeAdapter(challenges);
        rvChallenges.setAdapter(adapter);
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

