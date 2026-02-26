package com.example.gamehub;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.example.gamehub.auth.SessionManager;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Pantalla de perfil del usuario.
 * Muestra foto, nombre, estado, puntos y estadísticas.
 */
public class ProfileActivity extends BaseActivity {

    private SessionManager sessionManager;
    private ShapeableImageView ivProfilePhoto;
    private TextView tvProfileUsername;
    private TextView tvProfileStatus;
    private View viewStatusDot;
    private TextView tvProfilePoints;
    private TextView tvProfileGamesPlayed;
    private TextView tvProfileMemberSince;
    private TextView tvProfileTimePlayed;

    // Launcher para seleccionar foto de galería
    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    // Persistir permiso de lectura
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {
                    }
                    sessionManager.setPhotoUri(uri.toString());
                    loadProfilePhoto();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        initViews();
        setupListeners();
        loadProfileData();
        animateEntrance();
    }

    private void initViews() {
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvProfileUsername = findViewById(R.id.tvProfileUsername);
        tvProfileStatus = findViewById(R.id.tvProfileStatus);
        viewStatusDot = findViewById(R.id.viewStatusDot);
        tvProfilePoints = findViewById(R.id.tvProfilePoints);
        tvProfileGamesPlayed = findViewById(R.id.tvProfileGamesPlayed);
        tvProfileMemberSince = findViewById(R.id.tvProfileMemberSince);
        tvProfileTimePlayed = findViewById(R.id.tvProfileTimePlayed);
    }

    private void setupListeners() {
        findViewById(R.id.btnBackProfile).setOnClickListener(v -> finish());

        findViewById(R.id.btnChangePhoto).setOnClickListener(v ->
                photoPickerLauncher.launch("image/*"));

        findViewById(R.id.layoutStatus).setOnClickListener(v -> showStatusPicker());
    }

    private void loadProfileData() {
        // Nombre
        String username = sessionManager.getUsername();
        tvProfileUsername.setText(username != null ? username : "Usuario");

        // Foto
        loadProfilePhoto();

        // Estado
        updateStatusUI(sessionManager.getStatus());

        // Puntos
        int points = sessionManager.getTotalPoints();
        tvProfilePoints.setText(getString(R.string.points_format, points));

        // Partidas jugadas
        int gamesPlayed = sessionManager.getGamesPlayed();
        tvProfileGamesPlayed.setText(String.valueOf(gamesPlayed));

        // Tiempo jugado
        long totalSeconds = sessionManager.getTotalTimePlayed();
        tvProfileTimePlayed.setText(formatTime(totalSeconds));

        // Miembro desde
        long memberSince = sessionManager.getMemberSince();
        String date = new SimpleDateFormat("MMM yyyy", Locale.getDefault())
                .format(new Date(memberSince));
        tvProfileMemberSince.setText(date);
    }

    private void loadProfilePhoto() {
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

    private void showStatusPicker() {
        String[] statuses = {
                getString(R.string.status_online),
                getString(R.string.status_playing),
                getString(R.string.status_away)
        };
        String[] statusKeys = {"online", "playing", "away"};

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_status)
                .setItems(statuses, (dialog, which) -> {
                    sessionManager.setStatus(statusKeys[which]);
                    updateStatusUI(statusKeys[which]);
                })
                .show();
    }

    private void updateStatusUI(String status) {
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
        viewStatusDot.getBackground().setTint(getColor(colorRes));
        tvProfileStatus.setText(textRes);
        tvProfileStatus.setTextColor(getColor(colorRes));
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

