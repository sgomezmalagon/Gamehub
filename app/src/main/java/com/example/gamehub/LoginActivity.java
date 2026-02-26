package com.example.gamehub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.splashscreen.SplashScreen;

import com.example.gamehub.auth.SessionManager;
import com.example.kaisenclicker.persistence.save.UserRepository;

/**
 * Pantalla de inicio de sesión.
 * Si el usuario ya tiene sesión activa, salta directamente al Hub.
 */
public class LoginActivity extends BaseActivity {

    private EditText etUsername;
    private EditText etPassword;
    private TextView tvError;
    private Button btnLogin;
    private TextView tvGoToRegister;

    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Si ya hay sesión activa, ir directo al hub
        if (sessionManager.isLoggedIn()) {
            goToHub();
            return;
        }

        setContentView(R.layout.activity_login);

        userRepository = new UserRepository(this);

        initViews();
        setupListeners();
        animateEntrance();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Validaciones
        if (username.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        // Autenticar
        if (userRepository.authenticateUser(username, password)) {
            tvError.setVisibility(View.GONE);
            sessionManager.createSession(username);
            goToHub();
        } else {
            showError(getString(R.string.error_invalid_credentials));
            // Shake animation en el botón
            btnLogin.animate()
                    .translationX(-10).setDuration(50)
                    .withEndAction(() -> btnLogin.animate()
                            .translationX(10).setDuration(50)
                            .withEndAction(() -> btnLogin.animate()
                                    .translationX(-6).setDuration(50)
                                    .withEndAction(() -> btnLogin.animate()
                                            .translationX(0).setDuration(50).start())
                                    .start())
                            .start())
                    .start();
        }
    }

    private void goToHub() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
        tvError.setAlpha(0f);
        tvError.animate().alpha(1f).setDuration(200).start();
    }

    private void animateEntrance() {
        View root = findViewById(android.R.id.content);
        root.setAlpha(0f);
        root.animate()
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }
}

