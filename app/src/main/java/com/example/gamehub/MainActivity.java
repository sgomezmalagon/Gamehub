package com.example.gamehub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamehub.adapter.GameAdapter;
import com.example.gamehub.auth.SessionManager;
import com.example.gamehub.data.GameRepository;
import com.example.gamehub.leaderboard.LeaderboardActivity;
import com.example.gamehub.model.Game;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla principal del GameHub.
 * Muestra un grid con los juegos disponibles o un estado vacío profesional.
 */
public class MainActivity extends BaseActivity {

    private RecyclerView rvGames;
    private LinearLayout emptyStateContainer;
    private LinearLayout headerContainer;
    private TextView tvGameCount;
    private TextView tvSubtitle;
    private GameAdapter adapter;
    private SessionManager sessionManager;

    // Profile card
    private ShapeableImageView ivProfilePhoto;
    private TextView tvProfileName;
    private View viewStatusIndicator;
    private TextView tvStatusLabel;
    private TextView tvUserPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Si no hay sesión activa, volver al login
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);


        registerGames();
        initViews();
        setupUserInfo();
        setupProfileCard();
        setupMenuOptions();
        setupRecyclerView();
        animateHeader();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Acumula tiempo jugado si se vuelve de un juego
        sessionManager.markGameEnded();
        // Refresca la lista por si se registraron juegos nuevos
        loadGames();
        // Refresca datos del perfil
        refreshProfileCard();
    }

    // ═══════════════════════════════════════════════════

    /**
     * Registra todos los juegos disponibles en el hub.
     * Cada vez que se importe un nuevo módulo, se añade aquí.
     */
    private void registerGames() {
        GameRepository repo = GameRepository.getInstance();

        // ── Kaisen Clicker ──
        repo.registerGame(new Game(
                "kaisen_clicker",
                "Kaisen Clicker",
                com.example.kaisenclicker.R.drawable.kaisen_icon,
                "¡Haz clic para derrotar maldiciones!",
                com.example.kaisenclicker.ui.activities.MainActivity.class
        ));

        // ── 2048 ──
        repo.registerGame(new Game(
                "2048",
                "2048",
                com.example.a2048.R.drawable.icon,
                "¡Desliza y combina números!",
                com.example.a2048.MainActivity.class
        ));

        // ── Añadir más juegos aquí cuando se importen ──
    }

    private void initViews() {
        rvGames = findViewById(R.id.rvGames);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        headerContainer = findViewById(R.id.headerContainer);
        tvGameCount = findViewById(R.id.tvGameCount);
        tvSubtitle = findViewById(R.id.tvSubtitle);

        // Botón cerrar sesión
        ImageView btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
    }

    private void setupUserInfo() {
        String username = sessionManager.getUsername();
        if (username != null && tvSubtitle != null) {
            tvSubtitle.setText(getString(R.string.welcome_user, username));
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> performLogout())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void performLogout() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupRecyclerView() {
        // Grid de 2 columnas
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        rvGames.setLayoutManager(layoutManager);
        rvGames.setHasFixedSize(true);

        String username = sessionManager.getUsername();
        adapter = new GameAdapter(new ArrayList<>(), username);
        rvGames.setAdapter(adapter);
    }

    private void loadGames() {
        List<Game> games = GameRepository.getInstance().getGames();
        int count = games.size();

        tvGameCount.setText(getString(R.string.game_count, count));

        if (count > 0) {
            rvGames.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
            adapter.updateGames(games);
        } else {
            rvGames.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
            animateEmptyState();
        }
    }

    // ═══════════════════════════════════════════════════
    //                PERFIL Y MENÚ
    // ═══════════════════════════════════════════════════

    private void setupProfileCard() {
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvProfileName = findViewById(R.id.tvProfileName);
        viewStatusIndicator = findViewById(R.id.viewStatusIndicator);
        tvStatusLabel = findViewById(R.id.tvStatusLabel);
        tvUserPoints = findViewById(R.id.tvUserPoints);

        // Click en la tarjeta de perfil abre ProfileActivity
        LinearLayout profileCard = findViewById(R.id.profileCard);
        if (profileCard != null) {
            profileCard.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));
        }

        refreshProfileCard();
    }

    private void refreshProfileCard() {
        if (sessionManager == null) return;

        // Nombre
        String username = sessionManager.getUsername();
        if (tvProfileName != null) {
            tvProfileName.setText(username != null ? username : "Usuario");
        }

        // Foto
        if (ivProfilePhoto != null) {
            String photoUri = sessionManager.getPhotoUri();
            if (photoUri != null) {
                try {
                    ivProfilePhoto.setImageURI(null); // reset para forzar recarga
                    ivProfilePhoto.setImageURI(Uri.parse(photoUri));
                    ivProfilePhoto.setColorFilter(null);
                    ivProfilePhoto.setImageTintList(null);
                } catch (Exception e) {
                    ivProfilePhoto.setImageResource(R.drawable.ic_profile);
                }
            }
        }

        // Estado
        if (viewStatusIndicator != null && tvStatusLabel != null) {
            String status = sessionManager.getStatus();
            int colorRes;
            int textRes;
            switch (status) {
                case "playing":
                    colorRes = R.color.status_playing;
                    textRes = R.string.status_playing;
                    break;
                case "away":
                    colorRes = R.color.status_away;
                    textRes = R.string.status_away;
                    break;
                case "online":
                default:
                    colorRes = R.color.status_online;
                    textRes = R.string.status_online;
                    break;
            }
            viewStatusIndicator.getBackground().setTint(getColor(colorRes));
            tvStatusLabel.setText(textRes);
        }

        // Puntos
        if (tvUserPoints != null) {
            int points = sessionManager.getTotalPoints();
            tvUserPoints.setText(String.valueOf(points));
        }
    }

    private void setupMenuOptions() {
        // Botón Juegos — scroll al grid
        LinearLayout btnGames = findViewById(R.id.btnMenuGames);
        if (btnGames != null) {
            btnGames.setOnClickListener(v -> {
                if (rvGames != null) rvGames.smoothScrollToPosition(0);
            });
        }

        // Botón Puntuaciones — abre LeaderboardActivity
        LinearLayout btnScores = findViewById(R.id.btnMenuScores);
        if (btnScores != null) {
            btnScores.setOnClickListener(v ->
                    startActivity(new Intent(this, LeaderboardActivity.class)));
        }

        // Botón Desafíos — abre ChallengesActivity
        LinearLayout btnChallenges = findViewById(R.id.btnMenuChallenges);
        if (btnChallenges != null) {
            btnChallenges.setOnClickListener(v ->
                    startActivity(new Intent(this, ChallengesActivity.class)));
        }

        // Botón Perfil — abre ProfileActivity
        LinearLayout btnProfile = findViewById(R.id.btnMenuProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));
        }
    }

    // ═══════════════════════════════════════════════════
    //                    ANIMACIONES
    // ═══════════════════════════════════════════════════

    private void animateHeader() {
        headerContainer.setAlpha(0f);
        headerContainer.setTranslationY(-50f);
        headerContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setInterpolator(new DecelerateInterpolator(2f))
                .start();
    }

    private void animateEmptyState() {
        ImageView ivEmptyIcon = findViewById(R.id.ivEmptyIcon);
        if (ivEmptyIcon == null) return;

        // Animación de fade-in + slide-up
        emptyStateContainer.setAlpha(0f);
        emptyStateContainer.setTranslationY(40f);
        emptyStateContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(300)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();

        // Animación de "respiración" en el icono
        ivEmptyIcon.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(1500)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> ivEmptyIcon.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(1500)
                        .withEndAction(() -> animateBreathing(ivEmptyIcon))
                        .start())
                .start();
    }

    /**
     * Loop de animación "respiración" para el icono del estado vacío.
     */
    private void animateBreathing(ImageView view) {
        if (emptyStateContainer.getVisibility() != View.VISIBLE) return;

        view.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(1500)
                .withEndAction(() -> view.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(1500)
                        .withEndAction(() -> animateBreathing(view))
                        .start())
                .start();
    }
}
