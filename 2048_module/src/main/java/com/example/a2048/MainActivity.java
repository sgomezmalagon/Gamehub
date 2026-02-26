package com.example.a2048;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.kaisenclicker.persistence.save.SqlRepository;

public class MainActivity extends AppCompatActivity {
    private GridLayout gridLayout;
    private TextView tvScore;
    private TextView tvTimer;
    private TextView tvUser;
    private TextView[][] cellViews = new TextView[4][4];
    private GameEngine gameEngine;
    private int moves = 0;
    private TextView tvMoves;
    private TextView tvBestScore;
    private int bestScore = 0;
    private static final String PREFS_NAME = "2048_SaveGame";
    private String userPrefsName = PREFS_NAME;

    // Repositorio global (BD de kaisen_clicker) para guardar stats del 2048
    private SqlRepository globalRepo;

    // Undo simple: guardar el último estado
    private int[][] lastBoard = null;
    private int lastScore = -1;

    // Modes
    public enum Mode { NORMAL, BLITZ }
    private Mode currentMode = Mode.NORMAL;
    private boolean initialBlitzSelected = false;

    // Timer
    private int seconds = 0; // in NORMAL: elapsed, in BLITZ: remaining
    private boolean isTimerRunning = false;
    private android.os.Handler timerHandler = new android.os.Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isTimerRunning) return;
            if (currentMode == Mode.NORMAL) {
                seconds++;
                updateTimerUI();
                timerHandler.postDelayed(this, 1000);
            } else {
                // BLITZ countdown
                if (seconds > 0) {
                    seconds--;
                    updateTimerUI();
                    if (seconds == 0) {
                        onGameOverByTime();
                    } else {
                        timerHandler.postDelayed(this, 1000);
                    }
                }
            }
        }
    };

    // current user id for per-user prefs
    private String currentUserId = "player1";

    public static final String EXTRA_USERNAME = "extra_username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_2048_main);

        // Read username from Hub intent (per-user isolation)
        String hubUser = getIntent().getStringExtra(EXTRA_USERNAME);
        if (hubUser != null && !hubUser.isEmpty()) {
            currentUserId = hubUser;
        }

        // Use per-user SharedPreferences name
        userPrefsName = PREFS_NAME + "_" + currentUserId;

        // Inicializar repositorio global (BD kaisen_clicker) para guardar stats del 2048
        String dbName = "kaisen_clicker_" + currentUserId + ".db";
        globalRepo = new SqlRepository(this, dbName);

        android.content.SharedPreferences settings = getSharedPreferences(userPrefsName, MODE_PRIVATE);
        // Si el usuario tenía seleccionado Blitz antes, iniciar en BLITZ y forzar 5:00
        try {
            boolean savedBlitz = settings.getBoolean("blitzMode", false);
            if (savedBlitz) {
                currentMode = Mode.BLITZ;
                seconds = 300;
                initialBlitzSelected = true;
            }
        } catch (Exception ignored) {}

        // Load theme preference
        boolean isNightMode = settings.getBoolean("nightMode", false);
        if (isNightMode) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        gameEngine = new GameEngine();

        tvScore = findViewById(R.id.tvScore);
        tvBestScore = findViewById(R.id.tvBestScore);
        tvTimer = findViewById(R.id.tvTimer);
        tvMoves = findViewById(R.id.tvMoves);
        tvUser = findViewById(R.id.tvUser);
        gridLayout = findViewById(R.id.gridLayoutGame);

        tvUser.setText(currentUserId);

        setupBoard();

        // Choose mode at start - if user previously had Blitz, skip dialog and start directly
        if (initialBlitzSelected) {
            // Start directly in BLITZ
            loadGame();
            updateUI();
            startTimer();
        } else {
            // Show dialog to let user pick mode
            GameModeDialog.show(this, mode -> {
                if (mode == MainActivity.Mode.BLITZ) {
                    currentMode = Mode.BLITZ;
                    seconds = 300;
                } else {
                    currentMode = Mode.NORMAL;
                    seconds = 0;
                }
                android.content.SharedPreferences.Editor editor = getSharedPreferences(userPrefsName, MODE_PRIVATE).edit();
                editor.putBoolean("blitzMode", currentMode == Mode.BLITZ);
                editor.apply();

                // after selecting mode, load game and start
                loadGame();
                updateUI();
                startTimer();
            });
        }

        // Buttons
        View btnUndo = findViewById(R.id.btn_undo);
        if (btnUndo != null) btnUndo.setOnClickListener(v -> undoMove());

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> showModeSelector());

        // Settings toggles theme directly
        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) btnSettings.setOnClickListener(v -> toggleTheme());

        // Restart button
        View btnRestart = findViewById(R.id.btnRestart);
        if (btnRestart != null) btnRestart.setOnClickListener(v -> {
            gameEngine.resetGame();
            moves = 0;
            lastBoard = null;
            lastScore = -1;
            resetTimer();
            saveGame();
            updateUI();
        });

        setupGestures();
    }

    private void showModeSelector() {
        stopTimer();
        saveGame();

        // Reset game state for new mode
        gameEngine.resetGame();
        moves = 0;
        lastBoard = null;
        lastScore = -1;

        GameModeDialog.show(this, mode -> {
            if (mode == Mode.BLITZ) {
                currentMode = Mode.BLITZ;
                seconds = 300;
            } else {
                currentMode = Mode.NORMAL;
                seconds = 0;
            }
            android.content.SharedPreferences.Editor editor =
                    getSharedPreferences(userPrefsName, MODE_PRIVATE).edit();
            editor.putBoolean("blitzMode", currentMode == Mode.BLITZ);
            editor.apply();

            updateTimerUI();
            updateUI();
            startTimer();
        });
    }

    private void saveLastState() {
        int[][] m = gameEngine.getMatrix();
        lastBoard = new int[4][4];
        for (int i = 0; i < 4; i++) System.arraycopy(m[i], 0, lastBoard[i], 0, 4);
        lastScore = gameEngine.score;
    }

    private void undoMove() {
        if (lastBoard != null) {
            gameEngine.setMatrix(lastBoard);
            gameEngine.setScore(lastScore);
            lastBoard = null; // only one undo
            lastScore = -1;
            updateUI();
        } else {
            android.widget.Toast.makeText(this, "Nada que deshacer", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBoard() {
        gridLayout.removeAllViews();
        gridLayout.setColumnCount(4);
        gridLayout.setRowCount(4);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (70 * getResources().getDisplayMetrics().density);
        int availableWidth = screenWidth - padding;
        if (availableWidth < 0) availableWidth = 0;
        int tileSize = availableWidth / 4;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                TextView cell = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = tileSize - 16;
                params.height = tileSize - 16;
                params.rowSpec = GridLayout.spec(i);
                params.columnSpec = GridLayout.spec(j);
                params.setMargins(8, 8, 8, 8);
                cell.setLayoutParams(params);
                cell.setTextSize(32);
                cell.setGravity(Gravity.CENTER);
                cell.setBackgroundResource(R.drawable.bg_tile);
                cell.setTextColor(ContextCompat.getColor(this, R.color.text_on_tile));
                cell.setTypeface(null, android.graphics.Typeface.BOLD);
                cellViews[i][j] = cell;
                gridLayout.addView(cell);
            }
        }
    }

    private void updateUI() {
        int[][] matrix = gameEngine.getMatrix();
        tvScore.setText(String.valueOf(gameEngine.score));

        // Per-user best score stored in SharedPreferences with key "best_score_<user>"
        android.content.SharedPreferences settings = getSharedPreferences(userPrefsName, MODE_PRIVATE);
        String bestKey = "best_score_" + currentUserId;
        int savedBest = settings.getInt(bestKey, 0);
        if (gameEngine.score > savedBest) {
            savedBest = gameEngine.score;
            android.content.SharedPreferences.Editor editor = settings.edit();
            editor.putInt(bestKey, savedBest);
            editor.apply();
            // Save new best into scores table (as a record for leaderboard)
            try {
                insertScoreToKaisenDb(currentUserId, savedBest, "{\"game\":\"2048\"}");
            } catch (Exception ignored) {}
            // Save new best into global DB (kv_store)
            try {
                if (globalRepo != null) {
                    globalRepo.putInt("2048_best_score", savedBest);
                }
            } catch (Exception ignored) {}
        }
        bestScore = savedBest;
        tvBestScore.setText(String.valueOf(bestScore));

        tvMoves.setText(moves + " MOVIMIENTOS");

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                int value = matrix[i][j];
                TextView cell = cellViews[i][j];
                cell.setBackgroundResource(R.drawable.bg_tile);
                GradientDrawable background = (GradientDrawable) cell.getBackground().mutate();
                if (value == 0) {
                    cell.setText("");
                    background.setColor(ContextCompat.getColor(this, R.color.tile_0));
                } else {
                    cell.setText(String.valueOf(value));
                    background.setColor(ContextCompat.getColor(this, getColorForValue(value)));
                    cell.setTextColor(ContextCompat.getColor(this, R.color.text_on_tile));
                }
            }
        }
    }

    private void setupGestures() {
        View mainView = findViewById(android.R.id.content);
        mainView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                saveLastState();
                gameEngine.moveLeft();
                moves++;
                updateUI();
            }
            @Override
            public void onSwipeRight() {
                saveLastState();
                gameEngine.moveRight();
                moves++;
                updateUI();
            }
            @Override
            public void onSwipeUp() {
                saveLastState();
                gameEngine.moveUp();
                moves++;
                updateUI();
            }
            @Override
            public void onSwipeDown() {
                saveLastState();
                gameEngine.moveDown();
                moves++;
                updateUI();
            }
        });
    }

    private int getColorForValue(int value) {
        switch (value) {
            case 0: return R.color.tile_0;
            case 2: return R.color.tile_2;
            case 4: return R.color.tile_4; // fixed typo
            case 8: return R.color.tile_8;
            case 16: return R.color.tile_16;
            case 32: return R.color.tile_32;
            case 64: return R.color.tile_64;
            case 128: return R.color.tile_128;
            case 256: return R.color.tile_256;
            case 512: return R.color.tile_512;
            case 1024: return R.color.tile_1024;
            case 2048: return R.color.tile_2048;
            default: return R.color.tile_2048;
        }
    }

    private void startTimer() {
        if (!isTimerRunning) {
            // Si estamos en BLITZ y por alguna razón seconds está a 0 o negativo,
            // forzamos el inicio en 300s (5 minutos) para evitar comenzar en 0.
            try {
                if (currentMode == Mode.BLITZ && seconds <= 0) {
                    seconds = 300;
                    updateTimerUI();
                }
            } catch (Exception ignored) {}
            isTimerRunning = true;
            // Mostrar inmediatamente el estado del temporizador en la UI
            try { updateTimerUI(); } catch (Exception ignored) {}
            timerHandler.postDelayed(timerRunnable, 1000);
        }
    }

    private void stopTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void resetTimer() {
        stopTimer();
        seconds = (currentMode == Mode.BLITZ) ? 300 : 0;
        updateTimerUI();
        startTimer();
    }

    private void updateTimerUI() {
        int min = seconds / 60;
        int sec = seconds % 60;
        String timeString = String.format(java.util.Locale.getDefault(), "%02d:%02d", min, sec);
        tvTimer.setText(timeString);
    }

    private void onGameOverByTime() {
        stopTimer();
        saveGame();

        // Mostrar diálogo de tiempo agotado con opciones
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_over, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Mostrar puntuación final
        android.widget.TextView tvFinalScore = dialogView.findViewById(R.id.tv_final_score);
        tvFinalScore.setText("Puntuación: " + gameEngine.score);

        // Reiniciar Blitz
        dialogView.findViewById(R.id.card_restart_blitz).setOnClickListener(v -> {
            dialog.dismiss();
            gameEngine.resetGame();
            moves = 0;
            lastBoard = null;
            lastScore = -1;
            currentMode = Mode.BLITZ;
            seconds = 300;
            updateTimerUI();
            updateUI();
            saveGame();
            startTimer();
        });

        // Cambiar modo de juego
        dialogView.findViewById(R.id.card_change_mode).setOnClickListener(v -> {
            dialog.dismiss();
            showModeSelector();
        });

        dialog.show();
    }

    private void toggleTheme() {
        int current = AppCompatDelegate.getDefaultNightMode();
        android.content.SharedPreferences settings = getSharedPreferences(userPrefsName, MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = settings.edit();
        if (current == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            editor.putBoolean("nightMode", false);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            editor.putBoolean("nightMode", true);
        }
        editor.apply();
    }

    private void saveGame() {
        android.content.SharedPreferences settings = getSharedPreferences(userPrefsName, MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = settings.edit();
        StringBuilder gridString = new StringBuilder();
        int[][] matrix = gameEngine.getMatrix();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                gridString.append(matrix[i][j]).append(",");
            }
        }
        editor.putString("grid_" + currentUserId, gridString.toString());
        editor.putInt("score_" + currentUserId, gameEngine.score);
        // Also save per-user best
        editor.putInt("best_score_" + currentUserId, bestScore);
        editor.putInt("seconds_" + currentUserId, seconds);
        editor.putInt("moves_" + currentUserId, moves);
        editor.apply();

        // ── Persistir stats del 2048 en la BD global (kaisen_clicker) ──
        try {
            if (globalRepo != null) {
                globalRepo.putInt("2048_score", gameEngine.score);
                globalRepo.putInt("2048_best_score", bestScore);
                globalRepo.putInt("2048_moves", moves);
                globalRepo.putInt("2048_seconds", seconds);
                globalRepo.putString("2048_grid", gridString.toString());
            }
        } catch (Exception ignored) {}

        // Also persist current score (optionally) as a session entry in scores table
        try {
            insertScoreToKaisenDb(currentUserId, gameEngine.score, "{\"game\":\"2048\",\"type\":\"session\"}");
        } catch (Exception ignored) {}
    }

    private void loadGame() {
        android.content.SharedPreferences settings = getSharedPreferences(userPrefsName, MODE_PRIVATE);
        // Load best score per user
        bestScore = settings.getInt("best_score_" + currentUserId, 0);
        tvBestScore.setText(String.valueOf(bestScore));
        String gridString = settings.getString("grid_" + currentUserId, "");
        if (!gridString.isEmpty()) {
            String[] values = gridString.split(",");
            int[][] newMatrix = new int[4][4];
            int index = 0;
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (index < values.length) {
                        try {
                            newMatrix[i][j] = Integer.parseInt(values[index]);
                        } catch (NumberFormatException e) { newMatrix[i][j] = 0; }
                        index++;
                    }
                }
            }
            gameEngine.setMatrix(newMatrix);
            gameEngine.setScore(settings.getInt("score_" + currentUserId, 0));
            seconds = settings.getInt("seconds_" + currentUserId, (currentMode == Mode.BLITZ) ? 300 : 0);
            // Si estamos en BLITZ y el valor cargado es 0 o negativo, forzamos inicio en 5 minutos
            try { if (currentMode == Mode.BLITZ && seconds <= 0) seconds = 300; } catch (Exception ignored) {}
            moves = settings.getInt("moves_" + currentUserId, 0);
        }

        // ── Sincronizar datos actuales a la BD global ──
        try {
            if (globalRepo != null) {
                globalRepo.putInt("2048_score", gameEngine.score);
                globalRepo.putInt("2048_best_score", bestScore);
                globalRepo.putInt("2048_moves", moves);
                globalRepo.putInt("2048_seconds", seconds);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
        saveGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemNavigationBar();
        startTimer();
    }

    /**
     * Helper local: inserta en el fichero de BD shared per-user en la tabla `scores`.
     * No requiere clases externas, protege contra ausencia de tabla creando la tabla si es necesario.
     */
    private void insertScoreToKaisenDb(String playerName, long scoreValue, String extraJson) {
        SQLiteDatabase db = null;
        try {
            String dbName = "kaisen_clicker_" + currentUserId + ".db";
            db = openOrCreateDatabase(dbName, MODE_PRIVATE, null);
            // Ensure table exists (schema compatible con AppDatabaseHelper)
            db.execSQL("CREATE TABLE IF NOT EXISTS scores (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "player_name TEXT NOT NULL, " +
                    "game_name TEXT, " +
                    "score_value INTEGER NOT NULL, " +
                    "created_at INTEGER DEFAULT (strftime('%s','now')), " +
                    "extra TEXT" +
                    ")");
            // Add game_name column if upgrading from old version
            try {
                db.execSQL("ALTER TABLE scores ADD COLUMN game_name TEXT");
            } catch (Exception ignored) { /* column already exists */ }
            ContentValues cv = new ContentValues();
            cv.put("player_name", playerName);
            cv.put("game_name", "2048");
            cv.put("score_value", scoreValue);
            if (extraJson != null) cv.put("extra", extraJson);
            db.insertWithOnConflict("scores", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            // no-op but avoid crash
        } finally {
            if (db != null) {
                try { db.close(); } catch (Exception ignored) {}
            }
        }
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
