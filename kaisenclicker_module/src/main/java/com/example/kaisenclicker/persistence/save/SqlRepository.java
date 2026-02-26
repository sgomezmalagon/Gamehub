package com.example.kaisenclicker.persistence.save;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class SqlRepository {
    private static final String TAG = "SqlRepository";
    private final AppDatabaseHelper helper;

    public SqlRepository(Context context) {
        helper = new AppDatabaseHelper(context.getApplicationContext());
    }

    /**
     * Constructor con nombre de BD personalizado (para separar datos por usuario).
     */
    public SqlRepository(Context context, String dbName) {
        helper = new AppDatabaseHelper(context.getApplicationContext(), dbName);
    }

    // Ensure kv_store exists before use
    private void ensureKvStoreExists(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS kv_store (k TEXT PRIMARY KEY, value_text TEXT, value_int INTEGER, value_long INTEGER, value_real REAL)");
        } catch (Exception e) {
            Log.w(TAG, "ensureKvStoreExists failed", e);
        }
    }

    // --- KV store helpers (use column 'k') ---
    public boolean putInt(String key, int value) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            ensureKvStoreExists(db);
            ContentValues cv = new ContentValues();
            cv.put("k", key);
            cv.put("value_int", value);
            long id = db.insertWithOnConflict("kv_store", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "putInt: key=" + key + " value=" + value + " rowId=" + id);
            return id != -1;
        } catch (Exception e) {
            Log.e(TAG, "putInt failed for key=" + key, e);
            try { // attempt to recreate table and retry once
                SQLiteDatabase db = helper.getWritableDatabase();
                ensureKvStoreExists(db);
                ContentValues cv = new ContentValues();
                cv.put("k", key);
                cv.put("value_int", value);
                long id = db.insertWithOnConflict("kv_store", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                return id != -1;
            } catch (Exception ex) {
                Log.e(TAG, "putInt retry failed for key=" + key, ex);
                return false;
            }
        }
    }

    public int getInt(String key, int defaultValue) {
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            ensureKvStoreExists(db);
            Cursor c = db.rawQuery("SELECT value_int FROM kv_store WHERE k = ?", new String[]{key});
            try {
                if (c.moveToFirst()) {
                    int v = c.getInt(0);
                    Log.d(TAG, "getInt: key=" + key + " -> " + v);
                    return v;
                }
            } finally {
                c.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "getInt failed for key=" + key, e);
            // Attempt a single recreate and retry
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                ensureKvStoreExists(db);
                Cursor c = db.rawQuery("SELECT value_int FROM kv_store WHERE k = ?", new String[]{key});
                try {
                    if (c.moveToFirst()) return c.getInt(0);
                } finally { c.close(); }
            } catch (Exception ex) {
                Log.e(TAG, "getInt retry failed for key=" + key, ex);
            }
        }
        return defaultValue;
    }

    public boolean putLong(String key, long value) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            ensureKvStoreExists(db);
            ContentValues cv = new ContentValues();
            cv.put("k", key);
            cv.put("value_long", value);
            long id = db.insertWithOnConflict("kv_store", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "putLong: key=" + key + " value=" + value + " rowId=" + id);
            return id != -1;
        } catch (Exception e) {
            Log.e(TAG, "putLong failed for key=" + key, e);
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                ensureKvStoreExists(db);
                ContentValues cv = new ContentValues();
                cv.put("k", key);
                cv.put("value_long", value);
                long id = db.insertWithOnConflict("kv_store", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                return id != -1;
            } catch (Exception ex) {
                Log.e(TAG, "putLong retry failed for key=" + key, ex);
                return false;
            }
        }
    }

    public boolean putString(String key, String value) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            ensureKvStoreExists(db);
            ContentValues cv = new ContentValues();
            cv.put("k", key);
            if (value != null) cv.put("value_text", value);
            else cv.putNull("value_text");
            long id = db.insertWithOnConflict("kv_store", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "putString: key=" + key + " value=" + value + " rowId=" + id);
            return id != -1;
        } catch (Exception e) {
            Log.e(TAG, "putString failed for key=" + key, e);
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                ensureKvStoreExists(db);
                ContentValues cv = new ContentValues();
                cv.put("k", key);
                if (value != null) cv.put("value_text", value);
                else cv.putNull("value_text");
                long id = db.insertWithOnConflict("kv_store", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                return id != -1;
            } catch (Exception ex) {
                Log.e(TAG, "putString retry failed for key=" + key, ex);
                return false;
            }
        }
    }

    public long getLong(String key, long defaultValue) {
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            ensureKvStoreExists(db);
            Cursor c = db.rawQuery("SELECT value_long FROM kv_store WHERE k = ?", new String[]{key});
            try {
                if (c.moveToFirst()) {
                    long v = c.getLong(0);
                    Log.d(TAG, "getLong: key=" + key + " -> " + v);
                    return v;
                }
            } finally {
                c.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "getLong failed for key=" + key, e);
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                ensureKvStoreExists(db);
                Cursor c = db.rawQuery("SELECT value_long FROM kv_store WHERE k = ?", new String[]{key});
                try { if (c.moveToFirst()) return c.getLong(0); } finally { c.close(); }
            } catch (Exception ex) {
                Log.e(TAG, "getLong retry failed for key=" + key, ex);
            }
        }
        return defaultValue;
    }

    public String getString(String key, String defaultValue) {
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            ensureKvStoreExists(db);
            Cursor c = db.rawQuery("SELECT value_text FROM kv_store WHERE k = ?", new String[]{key});
            try {
                if (c.moveToFirst()) {
                    if (c.isNull(0)) return null;
                    String v = c.getString(0);
                    Log.d(TAG, "getString: key=" + key + " -> " + v);
                    return v;
                }
            } finally {
                c.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "getString failed for key=" + key, e);
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                ensureKvStoreExists(db);
                Cursor c = db.rawQuery("SELECT value_text FROM kv_store WHERE k = ?", new String[]{key});
                try { if (c.moveToFirst()) return c.isNull(0) ? null : c.getString(0); } finally { c.close(); }
            } catch (Exception ex) {
                Log.e(TAG, "getString retry failed for key=" + key, ex);
            }
        }
        return defaultValue;
    }

    // --------------------------- Scores / Leaderboard helpers ---------------------------
    /**
     * Inserta una puntuación en la tabla `scores`.
     * @return id de la fila insertada o -1 si falló
     */
    public long insertScore(Long userId, String playerName, long scoreValue, String extraJson) {
        return insertScore(userId, playerName, null, scoreValue, extraJson);
    }

    /**
     * Inserta una puntuación en la tabla `scores` con nombre de juego.
     * @return id de la fila insertada o -1 si falló
     */
    public long insertScore(Long userId, String playerName, String gameName, long scoreValue, String extraJson) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            if (userId != null) cv.put("user_id", userId);
            cv.put("player_name", playerName);
            if (gameName != null) cv.put("game_name", gameName);
            cv.put("score_value", scoreValue);
            if (extraJson != null) cv.put("extra", extraJson);
            long id = db.insertWithOnConflict("scores", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "insertScore: id=" + id + " name=" + playerName + " game=" + gameName + " score=" + scoreValue);
            return id;
        } catch (Exception e) {
            Log.e(TAG, "insertScore failed", e);
            return -1;
        }
    }

    /**
     * Borra una puntuación por id.
     */
    public boolean deleteScoreById(long id) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            int rows = db.delete("scores", "id = ?", new String[]{String.valueOf(id)});
            Log.d(TAG, "deleteScoreById: id=" + id + " rows=" + rows);
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "deleteScoreById failed id=" + id, e);
            return false;
        }
    }

    /**
     * Obtiene un Cursor con las puntuaciones aplicando filtros simples y orden.
     * nameFilter: substring para player_name (LIKE %...%), puede ser null.
     * scoreOp: operador comparativo como ">", "<", "=", ">=", "<=". Debe validarse por el llamador.
     * scoreValue: valor para comparar; puede ser null.
     * orderBy: cadena para ORDER BY, p.ej. "score_value DESC" o "player_name ASC".
     */
    public Cursor getScoresCursor(String nameFilter, String scoreOp, Long scoreValue, String orderBy) {
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            StringBuilder where = new StringBuilder();
            java.util.ArrayList<String> args = new java.util.ArrayList<>();
            if (nameFilter != null && !nameFilter.isEmpty()) {
                where.append("player_name LIKE ?");
                args.add("%" + nameFilter + "%");
            }
            if (scoreOp != null && scoreValue != null) {
                // Allow only a small set of operators to avoid injection
                if (!"=".equals(scoreOp) && !">".equals(scoreOp) && !"<".equals(scoreOp) && !">=".equals(scoreOp) && !"<=".equals(scoreOp)) {
                    scoreOp = "=";
                }
                if (where.length() > 0) where.append(" AND ");
                where.append("score_value ").append(scoreOp).append(" ?");
                args.add(String.valueOf(scoreValue));
            }
            String orderClause = (orderBy == null || orderBy.isEmpty()) ? "score_value DESC, created_at DESC" : orderBy;
            Cursor c = db.query("scores", new String[]{"id", "user_id", "player_name", "game_name", "score_value", "created_at", "extra"},
                    where.length() == 0 ? null : where.toString(),
                    args.isEmpty() ? null : args.toArray(new String[0]),
                    null, null, orderClause);
            return c;
        } catch (Exception e) {
            Log.e(TAG, "getScoresCursor failed", e);
            return null;
        }
    }

    /**
     * Obtiene una fila específica de scores por id.
     */
    public Cursor getScoreById(long id) {
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor c = db.query("scores", new String[]{"id", "user_id", "player_name", "game_name", "score_value", "created_at", "extra"},
                    "id = ?", new String[]{String.valueOf(id)}, null, null, null);
            return c;
        } catch (Exception e) {
            Log.e(TAG, "getScoreById failed id=" + id, e);
            return null;
        }
    }

    /**
     * Incrementa atomically un entero en kv_store y devuelve el nuevo valor.
     */
    public int incrementIntKV(String key, int delta) {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = helper.getWritableDatabase();
            ensureKvStoreExists(db);
            db.beginTransaction();
            int current = 0;
            c = db.rawQuery("SELECT value_int FROM kv_store WHERE k = ?", new String[]{key});
            try {
                if (c.moveToFirst()) current = c.getInt(0);
            } finally {
                c.close();
                c = null;
            }
            int next = current + delta;
            ContentValues cv = new ContentValues();
            cv.put("k", key);
            cv.put("value_int", next);
            db.insertWithOnConflict("kv_store", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
            Log.d(TAG, "incrementIntKV: key=" + key + " delta=" + delta + " -> " + next);
            return next;
        } catch (Exception e) {
            Log.e(TAG, "incrementIntKV failed for key=" + key, e);
            return -1;
        } finally {
            if (db != null) {
                try { db.endTransaction(); } catch (Exception ignored) {}
            }
            if (c != null) try { c.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Incrementa atomically un long en kv_store y devuelve el nuevo valor.
     */
    public long incrementLongKV(String key, long delta) {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = helper.getWritableDatabase();
            ensureKvStoreExists(db);
            db.beginTransaction();
            long current = 0L;
            c = db.rawQuery("SELECT value_long FROM kv_store WHERE k = ?", new String[]{key});
            try {
                if (c.moveToFirst()) current = c.getLong(0);
            } finally {
                c.close();
                c = null;
            }
            long next = current + delta;
            ContentValues cv = new ContentValues();
            cv.put("k", key);
            cv.put("value_long", next);
            db.insertWithOnConflict("kv_store", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
            Log.d(TAG, "incrementLongKV: key=" + key + " delta=" + delta + " -> " + next);
            return next;
        } catch (Exception e) {
            Log.e(TAG, "incrementLongKV failed for key=" + key, e);
            return -1L;
        } finally {
            if (db != null) {
                try { db.endTransaction(); } catch (Exception ignored) {}
            }
            if (c != null) try { c.close(); } catch (Exception ignored) {}
        }
    }

    // --- Chest helpers (drops) ---
    /**
     * Devuelve la cantidad de cofres guardada (por defecto 0).
     */
    public int getChestCount() {
        try {
            return getInt("chest_count", 0);
        } catch (Exception e) {
            Log.w(TAG, "getChestCount failed", e);
            return 0;
        }
    }

    /**
     * Incrementa el contador de cofres en 1 y devuelve el nuevo valor (o -1 si falla).
     */
    public int incrementChestCount() {
        try {
            int next = incrementIntKV("chest_count", 1);
            Log.d(TAG, "incrementChestCount -> " + next);
            return next;
        } catch (Exception e) {
            Log.e(TAG, "incrementChestCount failed", e);
            return -1;
        }
    }

    /**
     * Registra en la base de datos que se ha derrotado un boss específico.
     * Crea la tabla 'bosses' si no existe.
     */
    public boolean recordBossDefeat(int level, String bossId) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            // Ensure table exists (safe for upgrades)
            db.execSQL("CREATE TABLE IF NOT EXISTS bosses (id INTEGER PRIMARY KEY AUTOINCREMENT, boss_id TEXT, level INTEGER, defeated_at INTEGER)");
            ContentValues cv = new ContentValues();
            cv.put("boss_id", bossId);
            cv.put("level", level);
            cv.put("defeated_at", System.currentTimeMillis());
            long res = db.insert("bosses", null, cv);
            Log.d(TAG, "recordBossDefeat: boss=" + bossId + " level=" + level + " res=" + res);
            return res != -1;
        } catch (Exception e) {
            Log.e(TAG, "recordBossDefeat failed boss=" + bossId + " level=" + level, e);
            return false;
        }
    }

    // --- Enemy helpers ---
    public int getEnemyLevel() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT enemy_level FROM enemies WHERE id = 1", null);
        try {
            if (c.moveToFirst()) return c.getInt(0);
        } finally {
            c.close();
        }
        return 1;
    }

    public boolean setEnemyLevel(int level) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("enemy_level", level);
            int rows = db.update("enemies", cv, "id = 1", null);
            Log.d(TAG, "setEnemyLevel: " + level + " rows=" + rows);
            if (rows > 0) return true;
            // fallback: insert row with id=1
            try {
                ContentValues cv2 = new ContentValues();
                cv2.put("id", 1);
                cv2.put("enemy_level", level);
                cv2.put("defeated_count", 0);
                long res = db.insertWithOnConflict("enemies", null, cv2, SQLiteDatabase.CONFLICT_REPLACE);
                Log.d(TAG, "setEnemyLevel inserted fallback res=" + res);
                return res != -1;
            } catch (Exception e) {
                Log.e(TAG, "setEnemyLevel insert fallback failed", e);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "setEnemyLevel failed", e);
            return false;
        }
    }

    // --- Current enemy state helpers (non-destructive, separate table) ---
    private void ensureEnemyStateTableExists(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS enemy_state (id INTEGER PRIMARY KEY, level INTEGER DEFAULT 1, is_boss INTEGER DEFAULT 0, enemy_id TEXT)");
            // Ensure a single row with id=1 exists
            Cursor c = db.rawQuery("SELECT id FROM enemy_state WHERE id = 1", null);
            try {
                if (!c.moveToFirst()) {
                    ContentValues cv = new ContentValues();
                    cv.put("id", 1);
                    cv.put("level", 1);
                    cv.put("is_boss", 0);
                    cv.put("enemy_id", (String) null);
                    db.insertWithOnConflict("enemy_state", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            } finally {
                c.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "ensureEnemyStateTableExists failed", e);
        }
    }

    public boolean setCurrentEnemyState(int level, boolean isBoss, String enemyId) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            ensureEnemyStateTableExists(db);
            ContentValues cv = new ContentValues();
            cv.put("level", level);
            cv.put("is_boss", isBoss ? 1 : 0);
            cv.put("enemy_id", enemyId);
            int rows = db.update("enemy_state", cv, "id = 1", null);
            if (rows == 0) {
                cv.put("id", 1);
                long res = db.insertWithOnConflict("enemy_state", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                Log.d(TAG, "setCurrentEnemyState inserted fallback res=" + res);
                return res != -1;
            }
            Log.d(TAG, "setCurrentEnemyState updated rows=" + rows + " level=" + level + " isBoss=" + isBoss + " id=" + enemyId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "setCurrentEnemyState failed", e);
            return false;
        }
    }

    public int getCurrentEnemyLevel() {
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            ensureEnemyStateTableExists(db);
            Cursor c = db.rawQuery("SELECT level FROM enemy_state WHERE id = 1", null);
            try {
                if (c.moveToFirst()) return c.getInt(0);
            } finally {
                c.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "getCurrentEnemyLevel failed", e);
        }
        return 1;
    }

    public boolean isCurrentEnemyBoss() {
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            ensureEnemyStateTableExists(db);
            Cursor c = db.rawQuery("SELECT is_boss FROM enemy_state WHERE id = 1", null);
            try {
                if (c.moveToFirst()) return c.getInt(0) == 1;
            } finally {
                c.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "isCurrentEnemyBoss failed", e);
        }
        return false;
    }

    public String getCurrentEnemyId() {
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            ensureEnemyStateTableExists(db);
            Cursor c = db.rawQuery("SELECT enemy_id FROM enemy_state WHERE id = 1", null);
            try {
                if (c.moveToFirst()) return c.getString(0);
            } finally {
                c.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "getCurrentEnemyId failed", e);
        }
        return null;
    }

    // --- Characters helpers ---
    public boolean upsertCharacter(int id, boolean unlocked, int level, int xp) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("id", id);
            cv.put("unlocked", unlocked ? 1 : 0);
            cv.put("level", level);
            cv.put("xp", xp);
            long res = db.insertWithOnConflict("characters", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "upsertCharacter id=" + id + " lvl=" + level + " xp=" + xp + " unlocked=" + unlocked + " res=" + res);
            return res != -1;
        } catch (Exception e) {
            Log.e(TAG, "upsertCharacter failed id=" + id, e);
            return false;
        }
    }

    public Map<Integer, CharacterRecord> getAllCharacters() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, unlocked, level, xp FROM characters", null);
        Map<Integer, CharacterRecord> map = new HashMap<>();
        try {
            while (c.moveToNext()) {
                int id = c.getInt(0);
                boolean unlocked = c.getInt(1) == 1;
                int level = c.getInt(2);
                int xp = c.getInt(3);
                map.put(id, new CharacterRecord(id, unlocked, level, xp));
            }
        } finally {
            c.close();
        }
        return map;
    }

    public static class CharacterRecord {
        public final int id;
        public final boolean unlocked;
        public final int level;
        public final int xp;

        public CharacterRecord(int id, boolean unlocked, int level, int xp) {
            this.id = id;
            this.unlocked = unlocked;
            this.level = level;
            this.xp = xp;
        }
    }

    // --- Upgrades helpers ---
    public boolean setUpgradeLevel(String id, int level) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("id", id);
            cv.put("level", level);
            long res = db.insertWithOnConflict("upgrades", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "setUpgradeLevel id=" + id + " level=" + level + " res=" + res);
            return res != -1;
        } catch (Exception e) {
            Log.e(TAG, "setUpgradeLevel failed id=" + id, e);
            return false;
        }
    }

    public int getUpgradeLevel(String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT level FROM upgrades WHERE id = ?", new String[]{id});
        try {
            if (c.moveToFirst()) return c.getInt(0);
        } finally {
            c.close();
        }
        return 0;
    }

    /**
     * Ensure the skills table exists with a composite primary key (id, character_id).
     */
    private void ensureSkillsTableExists(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS skills (id TEXT, character_id INTEGER, unlocked INTEGER, level INTEGER, PRIMARY KEY(id, character_id))");
        } catch (Exception e) {
            Log.w(TAG, "ensureSkillsTableExists failed", e);
        }
    }

    // --- Upserts / queries for skills ---
    public boolean upsertSkill(String skillId, Integer characterId, boolean unlocked, int level) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            ensureSkillsTableExists(db);
            ContentValues cv = new ContentValues();
            cv.put("id", skillId);
            if (characterId != null) cv.put("character_id", characterId);
            else cv.putNull("character_id");
            cv.put("unlocked", unlocked ? 1 : 0);
            cv.put("level", level);
            long res = db.insertWithOnConflict("skills", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "upsertSkill id=" + skillId + " charId=" + characterId + " lvl=" + level + " unlocked=" + unlocked + " res=" + res);
            return res != -1;
        } catch (Exception e) {
            Log.e(TAG, "upsertSkill failed id=" + skillId, e);
            return false;
        }
    }

    public int getSkillLevel(String skillId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT level FROM skills WHERE id = ? LIMIT 1", new String[]{skillId});
        try {
            if (c.moveToFirst()) return c.getInt(0);
        } finally {
            c.close();
        }
        return 0;
    }

    /**
     * Obtiene el nivel de una skill priorizando la entrada para el characterId dado.
     * Si no existe una entrada para characterId, cae a la entrada global (character_id IS NULL).
     */
    public int getSkillLevel(String skillId, Integer characterId) {
        ensureSkillsTableExists(helper.getReadableDatabase());
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            if (characterId != null) {
                c = db.rawQuery("SELECT level FROM skills WHERE id = ? AND character_id = ? LIMIT 1", new String[]{skillId, String.valueOf(characterId)});
                if (c.moveToFirst()) return c.getInt(0);
                c.close(); c = null;
                // fallback to global
                c = db.rawQuery("SELECT level FROM skills WHERE id = ? AND character_id IS NULL LIMIT 1", new String[]{skillId});
                if (c.moveToFirst()) return c.getInt(0);
                return 0;
            } else {
                c = db.rawQuery("SELECT level FROM skills WHERE id = ? LIMIT 1", new String[]{skillId});
                if (c.moveToFirst()) return c.getInt(0);
                return 0;
            }
        } catch (Exception e) {
            Log.w(TAG, "getSkillLevel with characterId failed for " + skillId, e);
            return 0;
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignored) {}
        }
    }

    public boolean isSkillUnlocked(String skillId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        ensureSkillsTableExists(db);
        Cursor c = db.rawQuery("SELECT unlocked FROM skills WHERE id = ? LIMIT 1", new String[]{skillId});
        try {
            if (c.moveToFirst()) return c.getInt(0) == 1;
        } finally {
            c.close();
        }
        return false;
    }

    /**
     * Comprueba si la skill está desbloqueada priorizando un registro para characterId y luego global.
     */
    public boolean isSkillUnlocked(String skillId, Integer characterId) {
        ensureSkillsTableExists(helper.getReadableDatabase());
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            if (characterId != null) {
                c = db.rawQuery("SELECT unlocked FROM skills WHERE id = ? AND character_id = ? LIMIT 1", new String[]{skillId, String.valueOf(characterId)});
                if (c.moveToFirst()) return c.getInt(0) == 1;
                c.close(); c = null;
                c = db.rawQuery("SELECT unlocked FROM skills WHERE id = ? AND character_id IS NULL LIMIT 1", new String[]{skillId});
                if (c.moveToFirst()) return c.getInt(0) == 1;
                return false;
            } else {
                c = db.rawQuery("SELECT unlocked FROM skills WHERE id = ? LIMIT 1", new String[]{skillId});
                if (c.moveToFirst()) return c.getInt(0) == 1;
                return false;
            }
        } catch (Exception e) {
            Log.w(TAG, "isSkillUnlocked with characterId failed for " + skillId, e);
            return false;
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignored) {}
        }
    }

    public boolean incrementEnemiesDefeated() {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = helper.getWritableDatabase();
            db.beginTransaction();
            // Leer valor actual
            c = db.rawQuery("SELECT defeated_count FROM enemies WHERE id = 1", null);
            int current = 0;
            try {
                if (c.moveToFirst()) current = c.getInt(0);
            } finally {
                c.close();
                c = null;
            }
            int next = current + 1;
            ContentValues cv = new ContentValues();
            cv.put("id", 1);
            cv.put("defeated_count", next);
            long res = db.insertWithOnConflict("enemies", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
            Log.d(TAG, "incrementEnemiesDefeated: " + next + " res=" + res);
            return res != -1;
        } catch (Exception e) {
            Log.e(TAG, "incrementEnemiesDefeated failed", e);
            return false;
        } finally {
            if (db != null) {
                try { db.endTransaction(); } catch (Exception ignored) {}
            }
            if (c != null) try { c.close(); } catch (Exception ignored) {}
        }
    }

    public void close() {
        helper.close();
    }

    // Diagnostic dump of kv_store for debugging
    public String dumpKvStore() {
        StringBuilder sb = new StringBuilder();
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            ensureKvStoreExists(db);
            Cursor c = db.rawQuery("SELECT k, value_text, value_int, value_long FROM kv_store", null);
            try {
                while (c.moveToNext()) {
                    sb.append(c.getString(0)).append("=");
                    String t = c.isNull(1) ? "" : c.getString(1);
                    long li = c.isNull(2) ? Long.MIN_VALUE : c.getLong(2);
                    long ll = c.isNull(3) ? Long.MIN_VALUE : c.getLong(3);
                    sb.append("text(").append(t).append(") int(").append(c.isNull(2) ? "null" : c.getString(2)).append(") long(").append(c.isNull(3) ? "null" : c.getString(3)).append("); ");
                }
            } finally { c.close(); }
        } catch (Exception e) {
            Log.w(TAG, "dumpKvStore failed", e);
        }
        return sb.toString();
    }
}
