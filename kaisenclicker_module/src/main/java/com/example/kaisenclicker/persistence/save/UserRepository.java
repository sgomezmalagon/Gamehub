package com.example.kaisenclicker.persistence.save;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Repositorio de usuarios.
 * Usa la base de datos por defecto de Kaisen Clicker (kaisen_clicker.db)
 * para la tabla global 'users' (compartida entre todos los usuarios).
 */
public class UserRepository {

    private final AppDatabaseHelper dbHelper;

    public UserRepository(Context context) {
        // Usa la BD por defecto (kaisen_clicker.db), NO la per-user
        this.dbHelper = new AppDatabaseHelper(context.getApplicationContext());
    }

    /**
     * Registra un nuevo usuario.
     * @return true si se creó correctamente, false si el usuario ya existe
     */
    public boolean registerUser(String username, String password) {
        if (userExists(username)) {
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username.trim().toLowerCase());
        values.put("password_hash", hashPassword(password));

        long result = db.insert("users", null, values);
        return result != -1;
    }

    /**
     * Valida las credenciales de un usuario.
     * @return true si el usuario existe y la contraseña es correcta
     */
    public boolean authenticateUser(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String hashedPassword = hashPassword(password);

        Cursor cursor = db.query(
                "users",
                new String[]{"id"},
                "username = ? AND password_hash = ?",
                new String[]{username.trim().toLowerCase(), hashedPassword},
                null, null, null
        );

        boolean valid = cursor.getCount() > 0;
        cursor.close();
        return valid;
    }

    /**
     * Comprueba si un nombre de usuario ya está registrado.
     */
    public boolean userExists(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "users",
                new String[]{"id"},
                "username = ?",
                new String[]{username.trim().toLowerCase()},
                null, null, null
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * Hashea la contraseña con SHA-256.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }
}

