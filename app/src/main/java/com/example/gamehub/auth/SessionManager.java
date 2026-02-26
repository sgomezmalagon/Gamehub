package com.example.gamehub.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestiona la sesión del usuario actual.
 * Usa SharedPreferences para mantener al usuario logueado entre reinicios.
 */
public class SessionManager {

    private static final String PREFS_NAME = "GameHubSession";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_STATUS = "user_status";
    private static final String KEY_PHOTO_URI = "photo_uri";
    private static final String KEY_TOTAL_POINTS = "total_points";
    private static final String KEY_GAMES_PLAYED = "games_played";
    private static final String KEY_MEMBER_SINCE = "member_since";
    private static final String KEY_GAME_START_TIME = "game_start_time";
    private static final String KEY_TOTAL_TIME_PLAYED = "total_time_played";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Guarda la sesión del usuario tras un login exitoso.
     */
    public void createSession(String username) {
        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USERNAME, username);
        // Solo establece memberSince si no existía previamente
        if (!prefs.contains(KEY_MEMBER_SINCE)) {
            editor.putLong(KEY_MEMBER_SINCE, System.currentTimeMillis());
        }
        editor.apply();
    }

    /**
     * Cierra la sesión del usuario.
     */
    public void logout() {
        prefs.edit().clear().apply();
    }

    /**
     * @return true si hay un usuario logueado
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * @return nombre del usuario logueado, o null si no hay sesión
     */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    // ═══════════════════════════════════════════════════
    //                  PERFIL DE USUARIO
    // ═══════════════════════════════════════════════════

    /**
     * Guarda el estado del usuario (online, playing, away).
     */
    public void setStatus(String status) {
        prefs.edit().putString(KEY_STATUS, status).apply();
    }

    /**
     * @return estado del usuario o "online" por defecto
     */
    public String getStatus() {
        return prefs.getString(KEY_STATUS, "online");
    }

    /**
     * Guarda la URI de la foto de perfil.
     */
    public void setPhotoUri(String uri) {
        prefs.edit().putString(KEY_PHOTO_URI, uri).apply();
    }

    /**
     * @return URI de la foto de perfil, o null si no se ha establecido
     */
    public String getPhotoUri() {
        return prefs.getString(KEY_PHOTO_URI, null);
    }

    /**
     * Obtiene los puntos totales del usuario.
     */
    public int getTotalPoints() {
        return prefs.getInt(KEY_TOTAL_POINTS, 0);
    }

    /**
     * Establece los puntos totales del usuario.
     */
    public void setTotalPoints(int points) {
        prefs.edit().putInt(KEY_TOTAL_POINTS, points).apply();
    }

    /**
     * Suma puntos al total del usuario.
     */
    public void addPoints(int points) {
        int current = getTotalPoints();
        prefs.edit().putInt(KEY_TOTAL_POINTS, current + points).apply();
    }

    /**
     * Obtiene el número de partidas jugadas.
     */
    public int getGamesPlayed() {
        return prefs.getInt(KEY_GAMES_PLAYED, 0);
    }

    /**
     * Incrementa el contador de partidas jugadas.
     */
    public void incrementGamesPlayed() {
        int current = getGamesPlayed();
        prefs.edit().putInt(KEY_GAMES_PLAYED, current + 1).apply();
    }

    /**
     * Guarda la fecha en que el usuario se registró.
     */
    public void setMemberSince(long timestamp) {
        prefs.edit().putLong(KEY_MEMBER_SINCE, timestamp).apply();
    }

    /**
     * @return timestamp de cuando se registró el usuario
     */
    public long getMemberSince() {
        return prefs.getLong(KEY_MEMBER_SINCE, System.currentTimeMillis());
    }

    // ═══════════════════════════════════════════════════
    //               TRACKING DE TIEMPO
    // ═══════════════════════════════════════════════════

    /**
     * Marca el inicio de una sesión de juego (cuando el usuario abre un juego).
     */
    public void markGameStarted() {
        prefs.edit().putLong(KEY_GAME_START_TIME, System.currentTimeMillis()).apply();
    }

    /**
     * Calcula y acumula el tiempo jugado desde el último markGameStarted().
     * Se llama al volver del juego.
     * @return los segundos jugados en esta sesión, o 0 si no había marca de inicio
     */
    public long markGameEnded() {
        long startTime = prefs.getLong(KEY_GAME_START_TIME, 0);
        if (startTime <= 0) return 0;

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        if (elapsed > 0 && elapsed < 86400) { // máximo 24h para evitar datos corruptos
            long current = getTotalTimePlayed();
            prefs.edit()
                    .putLong(KEY_TOTAL_TIME_PLAYED, current + elapsed)
                    .remove(KEY_GAME_START_TIME)
                    .apply();
        } else {
            prefs.edit().remove(KEY_GAME_START_TIME).apply();
        }
        return Math.max(elapsed, 0);
    }

    /**
     * @return total de segundos jugados en todos los juegos
     */
    public long getTotalTimePlayed() {
        return prefs.getLong(KEY_TOTAL_TIME_PLAYED, 0);
    }
}

