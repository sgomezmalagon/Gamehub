package com.example.kaisenclicker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class StatsDatabaseHelper extends SQLiteOpenHelper {

    // Usar un fichero de BD separado para las stats del módulo para evitar colisiones
    private static final String DB_NAME = "kaisenclicker_stats.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_STATS = "stats";
    // columns
    public static final String COL_ID = "id";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_TOTAL_DAMAGE = "total_damage";
    public static final String COL_DPS = "dps";
    public static final String COL_ENEMIES = "enemies";
    public static final String COL_BOSSES = "bosses";
    public static final String COL_CLICKS = "clicks";
    public static final String COL_UNLOCKED = "unlocked";
    public static final String COL_NEXT_PROGRESS = "next_progress"; // 0-100

    public StatsDatabaseHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String create = "CREATE TABLE " + TABLE_STATS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USER_ID + " INTEGER, " +
                COL_TOTAL_DAMAGE + " INTEGER, " +
                COL_DPS + " REAL, " +
                COL_ENEMIES + " INTEGER, " +
                COL_BOSSES + " INTEGER, " +
                COL_CLICKS + " INTEGER, " +
                COL_UNLOCKED + " INTEGER, " +
                COL_NEXT_PROGRESS + " INTEGER" +
                ");";
        db.execSQL(create);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATS);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    // Fetch stats for a given user_id
    public Cursor getStatsForUser(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        String[] cols = {COL_TOTAL_DAMAGE, COL_DPS, COL_ENEMIES, COL_BOSSES, COL_CLICKS, COL_UNLOCKED, COL_NEXT_PROGRESS};
        String selection = COL_USER_ID + " = ?";
        String[] args = {String.valueOf(userId)};
        Cursor c = db.query(TABLE_STATS, cols, selection, args, null, null, null);
        return c;
    }

    // Insert default stats for a user if not exists
    public long ensureUserStats(long userId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_STATS, new String[]{COL_ID}, COL_USER_ID + " = ?", new String[]{String.valueOf(userId)}, null, null, null);
        if (c != null && c.moveToFirst()) {
            long id = c.getLong(0);
            c.close();
            return id;
        }
        if (c != null) c.close();

        ContentValues cv = new ContentValues();
        cv.put(COL_USER_ID, userId);
        cv.put(COL_TOTAL_DAMAGE, 0);
        cv.put(COL_DPS, 0.0);
        cv.put(COL_ENEMIES, 0);
        cv.put(COL_BOSSES, 0);
        cv.put(COL_CLICKS, 0);
        cv.put(COL_UNLOCKED, 0);
        cv.put(COL_NEXT_PROGRESS, 0);
        return db.insert(TABLE_STATS, null, cv);
    }

    // Reset stats for a user
    public int resetStatsForUser(long userId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TOTAL_DAMAGE, 0);
        cv.put(COL_DPS, 0.0);
        cv.put(COL_ENEMIES, 0);
        cv.put(COL_BOSSES, 0);
        cv.put(COL_CLICKS, 0);
        cv.put(COL_UNLOCKED, 0);
        cv.put(COL_NEXT_PROGRESS, 0);
        return db.update(TABLE_STATS, cv, COL_USER_ID + " = ?", new String[]{String.valueOf(userId)});
    }

    // Convenience method to update a stat column
    public int updateColumnForUser(long userId, String column, long value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(column, value);
        return db.update(TABLE_STATS, cv, COL_USER_ID + " = ?", new String[]{String.valueOf(userId)});
    }
}
