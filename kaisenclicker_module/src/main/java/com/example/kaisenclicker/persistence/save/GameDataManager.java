package com.example.kaisenclicker.persistence.save;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.kaisenclicker.model.character.CharacterSkillManager;

import java.util.Date;

public class GameDataManager {
    private static final String PREFS_NAME = "KaisenClickerData";
    private static final String TAG = "GameDataManager";

    // Keys
    private static final String KEY_CURSED_ENERGY = "cursed_energy";
    private static final String KEY_CHARACTER_UNLOCKED = "character_unlocked";
    private static final String KEY_ENEMY_LEVEL = "enemy_level";
    private static final String KEY_TAP_DAMAGE_LEVEL = "tap_damage_level";
    private static final String KEY_AUTO_CLICKER_LEVEL = "auto_clicker_level";
    private static final String KEY_AUTO_CLICKER_ENABLED = "auto_clicker_enabled";
    private static final String KEY_CRITICAL_DAMAGE_LEVEL = "critical_damage_level";
    private static final String KEY_ENERGY_BOOST_LEVEL = "energy_boost_level";
    private static final String KEY_CHARACTER_LEVEL = "character_level";
    private static final String KEY_CHARACTER_XP = "character_xp";

    // Statistics keys
    private static final String KEY_TOTAL_DAMAGE = "total_damage";
    private static final String KEY_TOTAL_CLICKS = "total_clicks";
    private static final String KEY_TOTAL_PLAY_SECONDS = "total_play_seconds";
    private static final String KEY_ENEMIES_DEFEATED = "enemies_defeated";
    private static final String KEY_BOSSES_DEFEATED = "bosses_defeated";
    private static final String KEY_CHARACTERS_UNLOCKED_COUNT = "characters_unlocked_count";
    private static final String KEY_MIGRATED_TO_SQL = "migrated_to_sql";
    private static final String KEY_CHEST_COUNT = "chest_count";

    private static final String KEY_PEAK_DPS = "peak_dps";
    private static final String KEY_ULTI_PROGRESS = "ulti_progress";
    private static final String KEY_SELECTED_CHARACTER = "selected_character_id";

    // transient enemy keys
    private static final String KEY_CURRENT_ENEMY_HP = "current_enemy_hp";
    private static final String KEY_CURRENT_ENEMY_ARMOR = "current_enemy_armor";
    private static final String KEY_CHOSO_SECOND_PHASE = "choso_second_phase";
    private static final String KEY_MAHITO_TRANSFORMED = "mahito_transformed";
    private static final String KEY_CURRENT_ENEMY_ID = "current_enemy_id";

    private SharedPreferences prefs;
    private final SqlRepository repository;
    private final CharacterSkillManager skillManager;

    // Character IDs
    public static final int CHAR_ID_RYOMEN_SUKUNA = 1;
    public static final int CHAR_ID_SATORU_GOJO = 2;
    public static final int TOTAL_CHARACTERS = 2;

    public GameDataManager(Context context) {
        this(context, null);
    }

    /**
     * Constructor con nombre de usuario. Aísla SharedPreferences y SQLite por usuario.
     * Si username es null o vacío, usa los nombres por defecto (compatibilidad hacia atrás).
     */
    public GameDataManager(Context context, String username) {
        String prefsSuffix = (username != null && !username.isEmpty()) ? "_" + username : "";
        String dbName = (username != null && !username.isEmpty())
                ? "kaisen_clicker_" + username + ".db"
                : AppDatabaseHelper.DATABASE_NAME;

        prefs = context.getSharedPreferences(PREFS_NAME + prefsSuffix, Context.MODE_PRIVATE);
        repository = new SqlRepository(context, dbName);
        skillManager = new CharacterSkillManager();

        migratePrefsToSqlIfNeeded();
        loadSkillLevelsIntoManager();
        performRepositoryHealthCheck();
    }

    private void performRepositoryHealthCheck() {
        if (repository == null) return;
        try {
            String testKey = "diag_test";
            String value = Long.toString(new Date().getTime());
            repository.putString(testKey, value);
            String read = repository.getString(testKey, "<null>");
            Log.i(TAG, "Repository health check: wrote='" + value + "' read='" + read + "'");
        } catch (Exception e) {
            Log.w(TAG, "Repository health check failed", e);
        }
    }

    private void migratePrefsToSqlIfNeeded() {
        try {
            boolean migrated = prefs.getBoolean(KEY_MIGRATED_TO_SQL, false);
            if (migrated) return;
            boolean ok = true;
            ok &= repository.putInt(KEY_CURSED_ENERGY, getCursedEnergy());
            ok &= repository.putInt(KEY_CHARACTER_UNLOCKED, isCharacterUnlocked() ? 1 : 0);
            ok &= repository.putInt(KEY_ENEMY_LEVEL, getEnemyLevel());
            ok &= repository.putInt(KEY_TAP_DAMAGE_LEVEL, prefs.getInt(KEY_TAP_DAMAGE_LEVEL, 1));
            ok &= repository.putInt(KEY_AUTO_CLICKER_LEVEL, prefs.getInt(KEY_AUTO_CLICKER_LEVEL, 1));
            ok &= repository.putLong(KEY_TOTAL_DAMAGE, prefs.getLong(KEY_TOTAL_DAMAGE, 0L));
            ok &= repository.putInt(KEY_TOTAL_CLICKS, prefs.getInt(KEY_TOTAL_CLICKS, 0));
            ok &= repository.putInt(KEY_CHEST_COUNT, prefs.getInt(KEY_CHEST_COUNT, 0));
            if (ok) prefs.edit().putBoolean(KEY_MIGRATED_TO_SQL, true).apply();
        } catch (Exception e) {
            Log.w(TAG, "migratePrefsToSqlIfNeeded failed", e);
        }
    }

    private void loadSkillLevelsIntoManager() {
        try {
            String[] knownSkills = new String[]{"cleave", "dismantle", "fuga", "domain", "amplificacion_azul", "ritual_inverso_rojo", "vacio_purpura"};
            for (String sid : knownSkills) {
                int lvl = getSkillLevel(sid, CHAR_ID_RYOMEN_SUKUNA);
                boolean unlocked = isSkillUnlocked(sid, CHAR_ID_RYOMEN_SUKUNA);
                com.example.kaisenclicker.model.skill.Skill s = skillManager.getSkillById(sid);
                if (s != null) {
                    s.setLevel(Math.max(0, lvl == 0 ? s.getLevel() : lvl));
                    s.setUnlocked(unlocked || s.isUnlocked());
                }
            }
        } catch (Exception e) { Log.w(TAG, "loadSkillLevelsIntoManager failed", e); }
    }

    // --- Repository access ---
    public SqlRepository getRepository() { return repository; }

    private boolean useSql() { return repository != null && prefs.getBoolean(KEY_MIGRATED_TO_SQL, false); }

    // --- Ulti ---
    public void saveUltiProgress(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        prefs.edit().putInt(KEY_ULTI_PROGRESS, clamped).apply();
        try { repository.putInt(KEY_ULTI_PROGRESS, clamped); } catch (Exception ignored) {}
    }
    public int getUltiProgress() { try { return useSql() ? repository.getInt(KEY_ULTI_PROGRESS, prefs.getInt(KEY_ULTI_PROGRESS, 0)) : prefs.getInt(KEY_ULTI_PROGRESS, 0); } catch (Exception ignored) { return prefs.getInt(KEY_ULTI_PROGRESS, 0); } }
    public void useUlti() { saveUltiProgress(0); }

    // --- Cursed energy ---
    public void saveCursedEnergy(int energy) { prefs.edit().putInt(KEY_CURSED_ENERGY, energy).apply(); try { repository.putInt(KEY_CURSED_ENERGY, energy); } catch (Exception ignored) {} }
    public int getCursedEnergy() { try { return useSql() ? repository.getInt(KEY_CURSED_ENERGY, prefs.getInt(KEY_CURSED_ENERGY, 0)) : prefs.getInt(KEY_CURSED_ENERGY, 0); } catch (Exception ignored) { return prefs.getInt(KEY_CURSED_ENERGY, 0); } }

    // --- Character unlocks ---
    public void saveCharacterUnlocked(boolean unlocked) { prefs.edit().putBoolean(KEY_CHARACTER_UNLOCKED, unlocked).apply(); try { repository.putInt(KEY_CHARACTER_UNLOCKED, unlocked ? 1 : 0); } catch (Exception ignored) {} }
    public boolean isCharacterUnlocked() { try { return useSql() ? repository.getInt(KEY_CHARACTER_UNLOCKED, prefs.getBoolean(KEY_CHARACTER_UNLOCKED, false) ? 1 : 0) == 1 : prefs.getBoolean(KEY_CHARACTER_UNLOCKED, false); } catch (Exception ignored) { return prefs.getBoolean(KEY_CHARACTER_UNLOCKED, false); } }
    public boolean isCharacterUnlockedById(int id) { try { if (repository != null) { java.util.Map<Integer, SqlRepository.CharacterRecord> all = repository.getAllCharacters(); if (all != null && all.containsKey(id)) return all.get(id).unlocked; } } catch (Exception ignored) {} return prefs.getBoolean(KEY_CHARACTER_UNLOCKED, false); }
    public boolean unlockCharacterById(int id) { try { prefs.edit().putBoolean(KEY_CHARACTER_UNLOCKED, true).apply(); boolean ok = true; if (repository != null) { ok &= repository.upsertCharacter(id, true, 1, 0); if (ok) incrementCharactersUnlocked(); } else incrementCharactersUnlocked(); return ok; } catch (Exception e) { Log.w(TAG, "unlockCharacterById failed", e); return false; } }
    public void incrementCharactersUnlocked() { int v = prefs.getInt(KEY_CHARACTERS_UNLOCKED_COUNT, 0) + 1; prefs.edit().putInt(KEY_CHARACTERS_UNLOCKED_COUNT, Math.min(TOTAL_CHARACTERS, v)).apply(); try { repository.incrementIntKV(KEY_CHARACTERS_UNLOCKED_COUNT, 1); } catch (Exception ignored) {} }
    public int getCharactersUnlockedCount() { try { if (useSql()) { java.util.Map<Integer, SqlRepository.CharacterRecord> all = repository.getAllCharacters(); if (all != null) return Math.min(TOTAL_CHARACTERS, all.size()); return Math.min(TOTAL_CHARACTERS, repository.getInt(KEY_CHARACTERS_UNLOCKED_COUNT, prefs.getInt(KEY_CHARACTERS_UNLOCKED_COUNT, 0))); } } catch (Exception ignored) {} return Math.min(TOTAL_CHARACTERS, prefs.getInt(KEY_CHARACTERS_UNLOCKED_COUNT, 0)); }

    // --- Enemy / spawn persistence ---

    public void saveCurrentEnemyState(int level, boolean isBoss, String enemyId) {
        try {
            prefs.edit().putInt(KEY_ENEMY_LEVEL, level).putString(KEY_CURRENT_ENEMY_ID, enemyId).apply();
            if (repository != null) {
                try { repository.setCurrentEnemyState(level, isBoss, enemyId); } catch (Exception ignored) {}
            }
        } catch (Exception e) { Log.w(TAG, "saveCurrentEnemyState failed", e); }
    }
    /** Guarda el nivel del enemigo (persistencia principal). */
    public void saveEnemyLevel(int level) {
        try {
            prefs.edit().putInt(KEY_ENEMY_LEVEL, level).apply();

            if (repository != null) {
                try { repository.putInt(KEY_ENEMY_LEVEL, level); } catch (Exception ignored) {}
                try { repository.setEnemyLevel(level); } catch (Exception ignored) {}
            }

            Log.d(TAG, "saveEnemyLevel completed val=" + level);
        } catch (Exception e) {
            Log.w(TAG, "saveEnemyLevel failed", e);
        }
    }
    /** Devuelve el nivel del enemigo (prefiere SQLite si está migrado). */
    public int getEnemyLevel() {
        try {

            if (useSql() && repository != null) {
                int repoVal = repository.getEnemyLevel();
                int prefsVal = prefs.getInt(KEY_ENEMY_LEVEL, 1);
                if (repoVal == 1 && prefsVal > 1) {
                    // sincronizar a DB si prefs tiene un valor mayor
                    try { repository.setEnemyLevel(prefsVal); } catch (Exception ignored) {}
                    return prefsVal;
                }
                return repoVal;
            }
        } catch (Exception ignored) {}
        return prefs.getInt(KEY_ENEMY_LEVEL, 1);
    }
    public int getCurrentEnemyLevelSql() { try { return useSql() ? repository.getCurrentEnemyLevel() : prefs.getInt(KEY_ENEMY_LEVEL, 1); } catch (Exception ignored) { return prefs.getInt(KEY_ENEMY_LEVEL, 1); } }
    public boolean isCurrentEnemyBoss() { try { return useSql() ? repository.isCurrentEnemyBoss() : false; } catch (Exception ignored) { return false; } }
    public String getCurrentEnemyId() { try { if (useSql() && repository != null) return repository.getCurrentEnemyId(); return prefs.getString(KEY_CURRENT_ENEMY_ID, null); } catch (Exception ignored) { return prefs.getString(KEY_CURRENT_ENEMY_ID, null); } }

    public void saveEnemyProgress(int currentHp, int currentArmor, boolean chosoSecondPhase, boolean mahitoTransformed) {
        try {
            prefs.edit().putInt(KEY_CURRENT_ENEMY_HP, currentHp).putInt(KEY_CURRENT_ENEMY_ARMOR, currentArmor)
                    .putBoolean(KEY_CHOSO_SECOND_PHASE, chosoSecondPhase).putBoolean(KEY_MAHITO_TRANSFORMED, mahitoTransformed).apply();
            try { repository.putInt(KEY_CURRENT_ENEMY_HP, currentHp); } catch (Exception ignored) {}
            try { repository.putInt(KEY_CURRENT_ENEMY_ARMOR, currentArmor); } catch (Exception ignored) {}
            try { repository.putInt(KEY_CHOSO_SECOND_PHASE, chosoSecondPhase ? 1 : 0); } catch (Exception ignored) {}
            try { repository.putInt(KEY_MAHITO_TRANSFORMED, mahitoTransformed ? 1 : 0); } catch (Exception ignored) {}
        } catch (Exception e) { Log.w(TAG, "saveEnemyProgress failed", e); }
    }
    public int getSavedEnemyCurrentHp() { try { return useSql() ? repository.getInt(KEY_CURRENT_ENEMY_HP, prefs.getInt(KEY_CURRENT_ENEMY_HP, -1)) : prefs.getInt(KEY_CURRENT_ENEMY_HP, -1); } catch (Exception ignored) { return prefs.getInt(KEY_CURRENT_ENEMY_HP, -1); } }
    public int getSavedEnemyArmor() { try { return useSql() ? repository.getInt(KEY_CURRENT_ENEMY_ARMOR, prefs.getInt(KEY_CURRENT_ENEMY_ARMOR, 0)) : prefs.getInt(KEY_CURRENT_ENEMY_ARMOR, 0); } catch (Exception ignored) { return prefs.getInt(KEY_CURRENT_ENEMY_ARMOR, 0); } }
    public boolean isChosoSecondPhaseSaved() { try { return useSql() ? repository.getInt(KEY_CHOSO_SECOND_PHASE, prefs.getBoolean(KEY_CHOSO_SECOND_PHASE, false) ? 1 : 0) == 1 : prefs.getBoolean(KEY_CHOSO_SECOND_PHASE, false); } catch (Exception ignored) { return prefs.getBoolean(KEY_CHOSO_SECOND_PHASE, false); } }
    public boolean isMahitoTransformedSaved() { try { return useSql() ? repository.getInt(KEY_MAHITO_TRANSFORMED, prefs.getBoolean(KEY_MAHITO_TRANSFORMED, false) ? 1 : 0) == 1 : prefs.getBoolean(KEY_MAHITO_TRANSFORMED, false); } catch (Exception ignored) { return prefs.getBoolean(KEY_MAHITO_TRANSFORMED, false); } }
    public void clearSavedEnemyProgress() {
        try {
            prefs.edit().remove(KEY_CURRENT_ENEMY_HP).remove(KEY_CURRENT_ENEMY_ARMOR).remove(KEY_CHOSO_SECOND_PHASE).remove(KEY_MAHITO_TRANSFORMED).remove(KEY_CURRENT_ENEMY_ID).apply();
            if (repository != null) {
                try {
                    repository.putInt(KEY_CURRENT_ENEMY_HP, -1);
                    repository.putInt(KEY_CURRENT_ENEMY_ARMOR, 0);
                    repository.putInt(KEY_CHOSO_SECOND_PHASE, 0);
                    repository.putInt(KEY_MAHITO_TRANSFORMED, 0);
                    repository.putString(KEY_CURRENT_ENEMY_ID, null);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) { Log.w(TAG, "clearSavedEnemyProgress failed", e); }
    }

    // --- Clicks / stats ---
    public int addClick() { int v = prefs.getInt(KEY_TOTAL_CLICKS, 0) + 1; prefs.edit().putInt(KEY_TOTAL_CLICKS, v).apply(); try { repository.incrementIntKV(KEY_TOTAL_CLICKS, 1); } catch (Exception ignored) {} return v; }
    public int getTotalClicks() { try { return useSql() ? repository.getInt(KEY_TOTAL_CLICKS, prefs.getInt(KEY_TOTAL_CLICKS, 0)) : prefs.getInt(KEY_TOTAL_CLICKS, 0); } catch (Exception ignored) { return prefs.getInt(KEY_TOTAL_CLICKS, 0); } }

    /** Añade daño total acumulado (estadística). */
    public void addTotalDamage(long amount) {
        try {
            long current = prefs.getLong(KEY_TOTAL_DAMAGE, 0L);
            long next = current + Math.max(0L, amount);
            prefs.edit().putLong(KEY_TOTAL_DAMAGE, next).apply();
            try { if (repository != null) repository.incrementLongKV(KEY_TOTAL_DAMAGE, Math.max(0L, amount)); } catch (Exception ignored) {}
        } catch (Exception e) { Log.w(TAG, "addTotalDamage failed", e); }
    }

    /** Devuelve el daño total acumulado. */
    public long getTotalDamage() {
        try { return useSql() ? repository.getLong(KEY_TOTAL_DAMAGE, prefs.getLong(KEY_TOTAL_DAMAGE, 0L)) : prefs.getLong(KEY_TOTAL_DAMAGE, 0L); } catch (Exception ignored) { return prefs.getLong(KEY_TOTAL_DAMAGE, 0L); }
    }

    /** Añade segundos jugados al contador total. */
    public void addPlaySeconds(long seconds) {
        if (seconds <= 0) return;
        try {
            long cur = prefs.getLong(KEY_TOTAL_PLAY_SECONDS, 0L);
            long next = cur + seconds;
            prefs.edit().putLong(KEY_TOTAL_PLAY_SECONDS, next).apply();
            try { if (repository != null) repository.incrementLongKV(KEY_TOTAL_PLAY_SECONDS, seconds); } catch (Exception ignored) {}
        } catch (Exception e) { Log.w(TAG, "addPlaySeconds failed", e); }
    }

    /** Devuelve el total de segundos jugados. */
    public long getTotalPlaySeconds() {
        try { return useSql() ? repository.getLong(KEY_TOTAL_PLAY_SECONDS, prefs.getLong(KEY_TOTAL_PLAY_SECONDS, 0L)) : prefs.getLong(KEY_TOTAL_PLAY_SECONDS, 0L); } catch (Exception ignored) { return prefs.getLong(KEY_TOTAL_PLAY_SECONDS, 0L); }
    }

    /** Calcula DPS medio = totalDamage / totalPlaySeconds (0 si sin datos). */
    public double getAverageDps() {
        try {
            long secs = getTotalPlaySeconds();
            if (secs <= 0) return 0.0;
            long dmg = getTotalDamage();
            return (double) dmg / (double) secs;
        } catch (Exception ignored) { return 0.0; }
    }

    /** Guarda el DPS pico si el valor proporcionado es mayor al guardado actualmente. */
    public boolean updatePeakDpsIfHigher(double currentDps) {
        try {
            double saved = getPeakDps();
            if (currentDps > saved) {
                savePeakDps(currentDps);
                return true;
            }
        } catch (Exception e) { Log.w(TAG, "updatePeakDpsIfHigher failed", e); }
        return false;
    }

    /** Persiste el DPS pico. */
    public void savePeakDps(double value) {
        try {
            long bits = Double.doubleToRawLongBits(value);
            prefs.edit().putLong(KEY_PEAK_DPS, bits).apply();
            try { if (repository != null) repository.putLong(KEY_PEAK_DPS, bits); } catch (Exception ignored) {}
        } catch (Exception e) { Log.w(TAG, "savePeakDps failed", e); }
    }

    /** Devuelve el DPS pico más alto registrado. */
    public double getPeakDps() {
        try {
            long bits = useSql()
                ? repository.getLong(KEY_PEAK_DPS, prefs.getLong(KEY_PEAK_DPS, 0L))
                : prefs.getLong(KEY_PEAK_DPS, 0L);
            if (bits == 0L) return 0.0;
            return Double.longBitsToDouble(bits);
        } catch (Exception ignored) { return 0.0; }
    }

    public void incrementEnemiesDefeated() { int v = prefs.getInt(KEY_ENEMIES_DEFEATED, 0) + 1; prefs.edit().putInt(KEY_ENEMIES_DEFEATED, v).apply(); try { repository.incrementIntKV(KEY_ENEMIES_DEFEATED, 1); repository.incrementEnemiesDefeated(); } catch (Exception ignored) {} }
    public int getEnemiesDefeated() { try { return useSql() ? repository.getInt(KEY_ENEMIES_DEFEATED, prefs.getInt(KEY_ENEMIES_DEFEATED, 0)) : prefs.getInt(KEY_ENEMIES_DEFEATED, 0); } catch (Exception ignored) { return prefs.getInt(KEY_ENEMIES_DEFEATED, 0); } }

    public void incrementBossesDefeated() { int v = prefs.getInt(KEY_BOSSES_DEFEATED, 0) + 1; prefs.edit().putInt(KEY_BOSSES_DEFEATED, v).apply(); try { repository.incrementIntKV(KEY_BOSSES_DEFEATED, 1); } catch (Exception ignored) {} }
    public int getBossesDefeated() { try { return useSql() ? repository.getInt(KEY_BOSSES_DEFEATED, prefs.getInt(KEY_BOSSES_DEFEATED, 0)) : prefs.getInt(KEY_BOSSES_DEFEATED, 0); } catch (Exception ignored) { return prefs.getInt(KEY_BOSSES_DEFEATED, 0); } }

    public int incrementChestCount() { try { int n = repository.incrementChestCount(); prefs.edit().putInt(KEY_CHEST_COUNT, n).apply(); return n; } catch (Exception ignored) {} int v = prefs.getInt(KEY_CHEST_COUNT, 0) + 1; prefs.edit().putInt(KEY_CHEST_COUNT, v).apply(); return v; }
    public int getChestCount() { try { return useSql() ? repository.getInt(KEY_CHEST_COUNT, prefs.getInt(KEY_CHEST_COUNT, 0)) : prefs.getInt(KEY_CHEST_COUNT, 0); } catch (Exception ignored) { return prefs.getInt(KEY_CHEST_COUNT, 0); } }
    public int decrementChestCount() { int v = getChestCount(); if (v <= 0) return 0; v--; prefs.edit().putInt(KEY_CHEST_COUNT, v).apply(); try { if (useSql()) repository.putInt(KEY_CHEST_COUNT, v); } catch (Exception ignored) {} return v; }

    public void recordBossDefeat(int level, String bossId) { incrementBossesDefeated(); try { repository.recordBossDefeat(level, bossId); } catch (Exception ignored) {} }

    // --- Upgrades & skills ---
    public void saveTapDamageLevel(int level) { prefs.edit().putInt(KEY_TAP_DAMAGE_LEVEL, level).apply(); try { repository.setUpgradeLevel(KEY_TAP_DAMAGE_LEVEL, level); } catch (Exception ignored) {} prefs.edit().putInt("tap_damage_value", calculateTotalDamage()).apply(); }
    public int getTapDamageLevel() { try { return useSql() ? repository.getUpgradeLevel(KEY_TAP_DAMAGE_LEVEL) : prefs.getInt(KEY_TAP_DAMAGE_LEVEL, 1); } catch (Exception ignored) { return prefs.getInt(KEY_TAP_DAMAGE_LEVEL, 1); } }
    public int getTapDamageValue() { try { return useSql() ? repository.getInt("tap_damage_value", calculateTotalDamage()) : prefs.getInt("tap_damage_value", calculateTotalDamage()); } catch (Exception ignored) { return calculateTotalDamage(); } }

    public void saveAutoClickerLevel(int level) { prefs.edit().putInt(KEY_AUTO_CLICKER_LEVEL, level).apply(); try { repository.setUpgradeLevel(KEY_AUTO_CLICKER_LEVEL, level); } catch (Exception ignored) {} }
    public int getAutoClickerLevel() { try { return useSql() ? repository.getUpgradeLevel(KEY_AUTO_CLICKER_LEVEL) : prefs.getInt(KEY_AUTO_CLICKER_LEVEL, 0); } catch (Exception ignored) { return prefs.getInt(KEY_AUTO_CLICKER_LEVEL, 0); } }

    public void saveAutoClickerEnabled(boolean enabled) { prefs.edit().putBoolean(KEY_AUTO_CLICKER_ENABLED, enabled).apply(); }
    public boolean isAutoClickerEnabled() { return prefs.getBoolean(KEY_AUTO_CLICKER_ENABLED, false); }

    // Critical Damage (Black Flash) upgrade
    public void saveCriticalDamageLevel(int level) {
        prefs.edit().putInt(KEY_CRITICAL_DAMAGE_LEVEL, level).apply();
        try { if (repository != null) repository.setUpgradeLevel(KEY_CRITICAL_DAMAGE_LEVEL, level); } catch (Exception ignored) {}
    }
    public int getCriticalDamageLevel() {
        try { return useSql() ? repository.getUpgradeLevel(KEY_CRITICAL_DAMAGE_LEVEL) : prefs.getInt(KEY_CRITICAL_DAMAGE_LEVEL, 1); } catch (Exception ignored) { return prefs.getInt(KEY_CRITICAL_DAMAGE_LEVEL, 1); }
    }

    // Energy Boost upgrade
    public void saveEnergyBoostLevel(int level) {
        prefs.edit().putInt(KEY_ENERGY_BOOST_LEVEL, level).apply();
        try { if (repository != null) repository.setUpgradeLevel(KEY_ENERGY_BOOST_LEVEL, level); } catch (Exception ignored) {}
    }
    public int getEnergyBoostLevel() {
        try { return useSql() ? repository.getUpgradeLevel(KEY_ENERGY_BOOST_LEVEL) : prefs.getInt(KEY_ENERGY_BOOST_LEVEL, 1); } catch (Exception ignored) { return prefs.getInt(KEY_ENERGY_BOOST_LEVEL, 1); }
    }

    public int calculateTotalDamage() { int baseDamage = 10; int tapLevel = prefs.getInt(KEY_TAP_DAMAGE_LEVEL, 1); try { if (repository != null) tapLevel = repository.getUpgradeLevel(KEY_TAP_DAMAGE_LEVEL); } catch (Exception ignored) {} int tapDamageBonus = 0; for (int i = 1; i < tapLevel; i++) tapDamageBonus += (15 * i); int critLevel = prefs.getInt(KEY_CRITICAL_DAMAGE_LEVEL, 1); try { if (repository != null) critLevel = repository.getUpgradeLevel(KEY_CRITICAL_DAMAGE_LEVEL); } catch (Exception ignored) {} float critBonus = 1.0f + ((critLevel - 1) * 0.1f); int total = (int) ((baseDamage + tapDamageBonus) * critBonus); total += Math.max(getCharacterLevel() - 1, 0) * 2; return Math.max(total, 10); }

    public boolean saveSkillLevel(String skillId, Integer characterId, int level) { try { String prefKey = "skill_" + skillId + "_level" + (characterId != null ? ("_char_" + characterId) : ""); prefs.edit().putInt(prefKey, level).apply(); if (repository != null) repository.upsertSkill(skillId, characterId, level > 0, level); try { com.example.kaisenclicker.model.skill.Skill s = skillManager.getSkillById(skillId); if (s != null) { s.setLevel(level); if (level > 0) s.setUnlocked(true); } } catch (Exception ignored) {} return true; } catch (Exception e) { Log.w(TAG, "saveSkillLevel failed", e); return false; } }
    public int getSkillLevel(String skillId, Integer characterId) { try { if (repository != null) { int l = repository.getSkillLevel(skillId, characterId); if (l > 0) return l; } } catch (Exception ignored) {} String prefKey = "skill_" + skillId + "_level" + (characterId != null ? ("_char_" + characterId) : ""); return prefs.getInt(prefKey, 0); }
    public boolean isSkillUnlocked(String skillId, Integer characterId) { try { if (repository != null) return repository.isSkillUnlocked(skillId, characterId); } catch (Exception ignored) {} String key = "skill_" + skillId + "_unlocked" + (characterId != null ? ("_char_" + characterId) : ""); return prefs.getBoolean(key, prefs.getBoolean("skill_" + skillId + "_unlocked", false)); }
    public boolean setSkillUnlocked(String skillId, boolean unlocked, Integer characterId) { try { String prefKey = "skill_" + skillId + "_unlocked" + (characterId != null ? ("_char_" + characterId) : ""); prefs.edit().putBoolean(prefKey, unlocked).apply(); if (repository != null) { int level = getSkillLevel(skillId, characterId); return repository.upsertSkill(skillId, characterId, unlocked, level); } try { com.example.kaisenclicker.model.skill.Skill s = skillManager.getSkillById(skillId); if (s != null) s.setUnlocked(unlocked); } catch (Exception ignored) {} return true; } catch (Exception e) { Log.w(TAG, "setSkillUnlocked failed", e); return false; } }

    public CharacterSkillManager getSkillManager() { return skillManager; }

    public void unlockAllSkillsForAllCharacters() {
        final int MAX = 10;
        String[] sukuna = new String[]{"cleave","dismantle","fuga","domain"};
        String[] gojo = new String[]{"amplificacion_azul","ritual_inverso_rojo","vacio_purpura","domain"};
        try {
            unlockCharacterById(CHAR_ID_RYOMEN_SUKUNA);
            unlockCharacterById(CHAR_ID_SATORU_GOJO);
            for (String s : sukuna) { saveSkillLevel(s, CHAR_ID_RYOMEN_SUKUNA, MAX); setSkillUnlocked(s, true, CHAR_ID_RYOMEN_SUKUNA); if (repository != null) repository.upsertSkill(s, CHAR_ID_RYOMEN_SUKUNA, true, MAX); }
            for (String s : gojo) { saveSkillLevel(s, CHAR_ID_SATORU_GOJO, MAX); setSkillUnlocked(s, true, CHAR_ID_SATORU_GOJO); if (repository != null) repository.upsertSkill(s, CHAR_ID_SATORU_GOJO, true, MAX); }
            try { java.util.List<String> all = new java.util.ArrayList<>(); for (String x : sukuna) if (!all.contains(x)) all.add(x); for (String x : gojo) if (!all.contains(x)) all.add(x); for (String sid : all) { com.example.kaisenclicker.model.skill.Skill sk = skillManager.getSkillById(sid); if (sk != null) { sk.setLevel(MAX); sk.setUnlocked(true); } } } catch (Exception ignored) {}
        } catch (Exception e) { Log.w(TAG, "unlockAllSkillsForAllCharacters failed", e); }
    }

    // --- Character progression (level/xp) ---
    public boolean setSelectedCharacterId(int id) {
        try {
            prefs.edit().putInt(KEY_SELECTED_CHARACTER, id).apply();
            try { repository.putInt(KEY_SELECTED_CHARACTER, id); } catch (Exception ignored) {}
            return true;
        } catch (Exception e) {
            Log.w(TAG, "setSelectedCharacterId failed", e);
            return false;
        }
    }

    public int getSelectedCharacterId() {
        try { return useSql() ? repository.getInt(KEY_SELECTED_CHARACTER, prefs.getInt(KEY_SELECTED_CHARACTER, CHAR_ID_RYOMEN_SUKUNA)) : prefs.getInt(KEY_SELECTED_CHARACTER, CHAR_ID_RYOMEN_SUKUNA); } catch (Exception ignored) { return prefs.getInt(KEY_SELECTED_CHARACTER, CHAR_ID_RYOMEN_SUKUNA); }
    }

    public void saveCharacterLevel(int level) {
        prefs.edit().putInt(KEY_CHARACTER_LEVEL, level).apply();
        try { repository.putInt(KEY_CHARACTER_LEVEL, level); } catch (Exception ignored) {}
    }

    public int getCharacterLevel() {
        try { return useSql() ? repository.getInt(KEY_CHARACTER_LEVEL, prefs.getInt(KEY_CHARACTER_LEVEL, 1)) : prefs.getInt(KEY_CHARACTER_LEVEL, 1); } catch (Exception ignored) { return prefs.getInt(KEY_CHARACTER_LEVEL, 1); }
    }

    public void saveCharacterXp(int xp) {
        prefs.edit().putInt(KEY_CHARACTER_XP, xp).apply();
        try { repository.putInt(KEY_CHARACTER_XP, xp); } catch (Exception ignored) {}
    }

    public int getCharacterXp() {
        try { return useSql() ? repository.getInt(KEY_CHARACTER_XP, prefs.getInt(KEY_CHARACTER_XP, 0)) : prefs.getInt(KEY_CHARACTER_XP, 0); } catch (Exception ignored) { return prefs.getInt(KEY_CHARACTER_XP, 0); }
    }

    public int getCharacterXpMax(int level) {
        int base = 100;
        return (int) (base * Math.pow(1.25, Math.max(level - 1, 0)));
    }

    /**
     * Añade XP al personaje, sube de nivel si corresponde y devuelve si subió de nivel.
     */
    public boolean addCharacterXp(int amount) {
        int level = getCharacterLevel();
        int xp = getCharacterXp() + Math.max(0, amount);
        boolean leveledUp = false;
        int max = getCharacterXpMax(level);
        while (xp >= max) {
            xp -= max;
            level++;
            max = getCharacterXpMax(level);
            leveledUp = true;
        }
        saveCharacterLevel(level);
        saveCharacterXp(xp);
        return leveledUp;
    }

    /**
     * Forzar migración desde SharedPreferences a SQLite (kv/tables). Devuelve true si parece correcta.
     */
    public boolean forceMigratePrefsToSqlNow() {
        if (repository == null) return false;
        boolean ok = true;
        try {
            ok &= repository.putInt(KEY_CURSED_ENERGY, prefs.getInt(KEY_CURSED_ENERGY, 0));
            ok &= repository.putInt(KEY_CHARACTER_UNLOCKED, prefs.getBoolean(KEY_CHARACTER_UNLOCKED, false) ? 1 : 0);
            ok &= repository.putInt(KEY_ENEMY_LEVEL, prefs.getInt(KEY_ENEMY_LEVEL, 1));

            ok &= repository.setUpgradeLevel(KEY_TAP_DAMAGE_LEVEL, prefs.getInt(KEY_TAP_DAMAGE_LEVEL, 1));
            ok &= repository.setUpgradeLevel(KEY_AUTO_CLICKER_LEVEL, prefs.getInt(KEY_AUTO_CLICKER_LEVEL, 1));

            ok &= repository.putLong(KEY_TOTAL_DAMAGE, prefs.getLong(KEY_TOTAL_DAMAGE, 0L));
            ok &= repository.putInt(KEY_TOTAL_CLICKS, prefs.getInt(KEY_TOTAL_CLICKS, 0));
            ok &= repository.putInt(KEY_CHEST_COUNT, prefs.getInt(KEY_CHEST_COUNT, 0));
            ok &= repository.putInt(KEY_CHARACTERS_UNLOCKED_COUNT, prefs.getInt(KEY_CHARACTERS_UNLOCKED_COUNT, 0));

            // Migrate known skill levels (best-effort)
            String[] knownSkills = new String[]{"cleave","dismantle","fuga","domain","amplificacion_azul","ritual_inverso_rojo","vacio_purpura"};
            for (String sid : knownSkills) {
                String prefKeyChar = "skill_" + sid + "_level_char_1";
                int lv = prefs.getInt(prefKeyChar, -1);
                if (lv < 0) {
                    String prefKey = "skill_" + sid + "_level";
                    lv = prefs.getInt(prefKey, 0);
                }
                if (lv > 0) ok &= repository.upsertSkill(sid, 1, true, lv);
            }

            if (ok) prefs.edit().putBoolean(KEY_MIGRATED_TO_SQL, true).apply();
            Log.i(TAG, "forceMigratePrefsToSqlNow: ok=" + ok);
            return ok;
        } catch (Exception e) {
            Log.w(TAG, "forceMigratePrefsToSqlNow failed", e);
            return false;
        }
    }

    /**
     * Resetea estadísticas y datos transitorios del juego (SharedPreferences y SQLite si está presente).
     * Usado por la UI de Estadísticas para un "reset" global de métricas.
     */
    public void resetAllData() {
        try {
            // Reset basic stats in SharedPreferences
            prefs.edit()
                    .putLong(KEY_TOTAL_DAMAGE, 0L)
                    .putLong(KEY_TOTAL_PLAY_SECONDS, 0L)
                    .putInt(KEY_TOTAL_CLICKS, 0)
                    .putInt(KEY_ENEMIES_DEFEATED, 0)
                    .putInt(KEY_BOSSES_DEFEATED, 0)
                    .putInt(KEY_CHEST_COUNT, 0)
                    .putInt(KEY_CHARACTERS_UNLOCKED_COUNT, 0)
                    .putInt(KEY_ENEMY_LEVEL, 1)
                    .apply();

            // Clear transient saved enemy progress
            clearSavedEnemyProgress();

            // Mirror reset to repository when available
            if (repository != null) {
                try { repository.putLong(KEY_TOTAL_DAMAGE, 0L); } catch (Exception ignored) {}
                try { repository.putInt(KEY_TOTAL_CLICKS, 0); } catch (Exception ignored) {}
                try { repository.putInt(KEY_ENEMIES_DEFEATED, 0); } catch (Exception ignored) {}
                try { repository.putInt(KEY_BOSSES_DEFEATED, 0); } catch (Exception ignored) {}
                try { repository.putInt(KEY_CHEST_COUNT, 0); } catch (Exception ignored) {}
                try { repository.putInt(KEY_CHARACTERS_UNLOCKED_COUNT, 0); } catch (Exception ignored) {}
                try { repository.putInt(KEY_ENEMY_LEVEL, 1); } catch (Exception ignored) {}
                try { repository.putInt(KEY_CURRENT_ENEMY_HP, -1); } catch (Exception ignored) {}
                try { repository.putInt(KEY_CURRENT_ENEMY_ARMOR, 0); } catch (Exception ignored) {}
                try { repository.putInt(KEY_CHOSO_SECOND_PHASE, 0); } catch (Exception ignored) {}
                try { repository.putInt(KEY_MAHITO_TRANSFORMED, 0); } catch (Exception ignored) {}
            }
            Log.i(TAG, "resetAllData: stats and transient data reset");
        } catch (Exception e) {
            Log.w(TAG, "resetAllData failed", e);
        }
    }

}
