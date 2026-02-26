package com.example.kaisenclicker.persistence.save;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AppDatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "kaisen_clicker.db";
    public static final int DATABASE_VERSION = 4;
    private static final String TAG = "AppDatabaseHelper";

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Constructor con nombre de BD personalizado (para separar datos por usuario).
     * @param context  contexto
     * @param dbName   nombre del fichero de BD (ej: "kaisen_clicker_sergio.db")
     */
    public AppDatabaseHelper(Context context, String dbName) {
        super(context, dbName, null, DATABASE_VERSION);
    }

    private static final String SQL_CREATE_USERS =
            "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "password_hash TEXT NOT NULL, " +
                    "created_at INTEGER DEFAULT (strftime('%s','now'))" +
                    ")";

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database tables");

        // Users table (global, shared across all users)
        db.execSQL(SQL_CREATE_USERS);

        // Generic key-value store for simple values (cursed energy, totals, etc.)
        // use column name 'k' instead of reserved word 'key'
        db.execSQL("CREATE TABLE IF NOT EXISTS kv_store (k TEXT PRIMARY KEY, value_text TEXT, value_int INTEGER, value_long INTEGER, value_real REAL)");

        // Characters table: id, unlocked, level, xp
        db.execSQL("CREATE TABLE IF NOT EXISTS characters (id INTEGER PRIMARY KEY, unlocked INTEGER DEFAULT 0, level INTEGER DEFAULT 1, xp INTEGER DEFAULT 0)");

        // Upgrades table: id (text), level, purchased
        db.execSQL("CREATE TABLE IF NOT EXISTS upgrades (id TEXT PRIMARY KEY, level INTEGER DEFAULT 0, purchased INTEGER DEFAULT 0)");

        // Skills table: id, character_id (nullable), unlocked, level
        db.execSQL("CREATE TABLE IF NOT EXISTS skills (id TEXT PRIMARY KEY, character_id INTEGER, unlocked INTEGER DEFAULT 0, level INTEGER DEFAULT 0)");

        // Enemies table: store enemy level and defeated_count
        db.execSQL("CREATE TABLE IF NOT EXISTS enemies (id INTEGER PRIMARY KEY AUTOINCREMENT, enemy_level INTEGER DEFAULT 1, defeated_count INTEGER DEFAULT 0)");

        // Insert a default enemies row to simplify queries (id = 1)
        try {
            db.execSQL("INSERT OR IGNORE INTO enemies (enemy_level, defeated_count) VALUES (1, 0)");
        } catch (Exception e) {
            Log.w(TAG, "onCreate: inserting default enemies failed", e);
        }

        // Scores / leaderboard table (shared across games, linked by user_id)
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS scores (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "player_name TEXT NOT NULL, " +
                    "game_name TEXT, " +
                    "score_value INTEGER NOT NULL, " +
                    "created_at INTEGER DEFAULT (strftime('%s','now')), " +
                    "extra TEXT" +
                    ")");
        } catch (Exception e) {
            Log.w(TAG, "onCreate: creating scores table failed", e);
        }

        // Insert default character rows (Ryomen Sukuna id=1, Satoru Gojo id=2) unlocked=0
        try {
            db.execSQL("INSERT OR IGNORE INTO characters (id, unlocked, level, xp) VALUES (1, 0, 1, 0)");
            db.execSQL("INSERT OR IGNORE INTO characters (id, unlocked, level, xp) VALUES (2, 0, 1, 0)");
        } catch (Exception e) {
            Log.w(TAG, "onCreate: inserting default characters failed", e);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        Log.i(TAG, "onOpen: verifying/creating required tables and defaults");
        try {
            // Enable foreign keys in case some code relies on them
            try {
                db.execSQL("PRAGMA foreign_keys = ON;");
            } catch (Exception ignored) {}

            db.execSQL(SQL_CREATE_USERS);
            db.execSQL("CREATE TABLE IF NOT EXISTS kv_store (k TEXT PRIMARY KEY, value_text TEXT, value_int INTEGER, value_long INTEGER, value_real REAL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS characters (id INTEGER PRIMARY KEY, unlocked INTEGER DEFAULT 0, level INTEGER DEFAULT 1, xp INTEGER DEFAULT 0)");
            db.execSQL("CREATE TABLE IF NOT EXISTS upgrades (id TEXT PRIMARY KEY, level INTEGER DEFAULT 0, purchased INTEGER DEFAULT 0)");
            db.execSQL("CREATE TABLE IF NOT EXISTS skills (id TEXT PRIMARY KEY, character_id INTEGER, unlocked INTEGER DEFAULT 0, level INTEGER DEFAULT 0)");
            db.execSQL("CREATE TABLE IF NOT EXISTS enemies (id INTEGER PRIMARY KEY AUTOINCREMENT, enemy_level INTEGER DEFAULT 1, defeated_count INTEGER DEFAULT 0)");

            // Ensure default rows exist
            try {
                db.execSQL("INSERT OR IGNORE INTO enemies (id, enemy_level, defeated_count) VALUES (1, 1, 0)");
            } catch (Exception ignored) {}
            try {
                db.execSQL("INSERT OR IGNORE INTO characters (id, unlocked, level, xp) VALUES (1, 0, 1, 0)");
                db.execSQL("INSERT OR IGNORE INTO characters (id, unlocked, level, xp) VALUES (2, 0, 1, 0)");
            } catch (Exception ignored) {}

            // Ensure scores table exists
            try {
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
            } catch (Exception ignored) {}
        } catch (Exception e) {
            Log.w(TAG, "onOpen: ensure tables failed", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database from " + oldVersion + " to " + newVersion);
        // For now drop and recreate (data-loss on upgrade). For production, implement migrations.
        db.execSQL("DROP TABLE IF EXISTS kv_store");
        db.execSQL("DROP TABLE IF EXISTS characters");
        db.execSQL("DROP TABLE IF EXISTS upgrades");
        db.execSQL("DROP TABLE IF EXISTS enemies");
        db.execSQL("DROP TABLE IF EXISTS skills");
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS scores");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Downgrading database from " + oldVersion + " to " + newVersion + ". Recreating tables.");
        onUpgrade(db, oldVersion, newVersion);
    }
}
