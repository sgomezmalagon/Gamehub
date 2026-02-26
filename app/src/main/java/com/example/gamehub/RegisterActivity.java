package com.example.gamehub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kaisenclicker.persistence.save.UserRepository;

/**
 * Pantalla de registro de nuevo usuario.
 */
public class RegisterActivity extends BaseActivity {

    private EditText etUsername;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private TextView tvError;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userRepository = new UserRepository(this);

        initViews();
        setupListeners();
        animateEntrance();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tvError = findViewById(R.id.tvError);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void attemptRegister() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Validaciones
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        if (username.length() < 3) {
            showError(getString(R.string.error_short_username));
            return;
        }

        if (password.length() < 4) {
            showError(getString(R.string.error_short_password));
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError(getString(R.string.error_passwords_mismatch));
            return;
        }

        if (userRepository.userExists(username)) {
            showError(getString(R.string.error_user_exists));
            return;
        }

        // Registrar
        boolean success = userRepository.registerUser(username, password);
        if (success) {
            Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();
            // Ir al login
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            showError(getString(R.string.error_user_exists));
        }
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

