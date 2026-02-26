package com.example.kaisenclicker.model.skill;

/**
 * Modelo de datos para una habilidad de personaje
 * Las habilidades son ACTIVAS y se usan en combate
 */
public class Skill {
    private String id;
    private String name;
    private String description;
    private int level;
    private int maxLevel;
    private boolean unlocked;
    private SkillType type;
    private int cooldownMs; // cooldown base (ms)
    private long lastUsedTime;

    // Parámetros opcionales de sangrado (bleed / DoT)
    private int bleedDurationMs;    // duración total del sangrado (ms)
    private int bleedTickMs;        // intervalo entre ticks (ms)
    private float bleedBaseFactor;  // factor base relativo al daño por click (ej. 0.2f = 20%)
    private float bleedPerLevelFactor; // factor adicional por nivel (ej. 0.05f = 5% por nivel)

    public enum SkillType {
        NORMAL_1("Cleave"),           // Ataque básico + sangrado
        NORMAL_2("Dismantle"),         // Ataque especial + bonus daño
        NORMAL_3("Fuga"),              // Evasión + contraataque
        ULTIMATE("Domain Expansion");  // Habilidad definitiva

        private final String displayName;

        SkillType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Skill(String id, String name, String description, SkillType type, int maxLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.maxLevel = maxLevel;
        this.level = 0;
        this.unlocked = false;
        this.cooldownMs = 0;
        this.lastUsedTime = 0;
        // bleed defaults (0 = sin sangrado)
        this.bleedDurationMs = 0;
        this.bleedTickMs = 0;
        this.bleedBaseFactor = 0f;
        this.bleedPerLevelFactor = 0f;
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getLevel() { return level; }
    public int getMaxLevel() { return maxLevel; }
    public boolean isUnlocked() { return unlocked; }
    public SkillType getType() { return type; }
    public int getCooldownMs() { return cooldownMs; }
    public long getLastUsedTime() { return lastUsedTime; }

    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    public void setLevel(int level) { this.level = Math.min(level, maxLevel); }
    public void setCooldownMs(int cooldown) { this.cooldownMs = cooldown; }
    public void setLastUsedTime(long time) { this.lastUsedTime = time; }

    // Bleed getters/setters
    public int getBleedDurationMs() { return bleedDurationMs; }
    public void setBleedDurationMs(int bleedDurationMs) { this.bleedDurationMs = bleedDurationMs; }
    public int getBleedTickMs() { return bleedTickMs; }
    public void setBleedTickMs(int bleedTickMs) { this.bleedTickMs = bleedTickMs; }
    public float getBleedBaseFactor() { return bleedBaseFactor; }
    public void setBleedBaseFactor(float bleedBaseFactor) { this.bleedBaseFactor = bleedBaseFactor; }
    public float getBleedPerLevelFactor() { return bleedPerLevelFactor; }
    public void setBleedPerLevelFactor(float bleedPerLevelFactor) { this.bleedPerLevelFactor = bleedPerLevelFactor; }

    public boolean hasBleed() {
        return bleedDurationMs > 0 && bleedTickMs > 0 && (bleedBaseFactor > 0f || bleedPerLevelFactor > 0f);
    }

    /**
     * Calcula el daño por tick del sangrado basado en el daño base (por click)
     */
    public int computeBleedPerTick(int baseDamage) {
        if (!hasBleed()) return 0;
        float factor = bleedBaseFactor + (bleedPerLevelFactor * Math.max(0, level));
        int dmg = Math.max(1, Math.round(baseDamage * factor));
        return dmg;
    }

    /**
     * Calcula el cooldown efectivo en ms según el nivel de la habilidad.
     * Por defecto reducimos 10% del cooldown por cada nivel extra (nivel 1 = 100%).
     */
    public int getEffectiveCooldownMs() {
        if (level <= 1) return cooldownMs;
        double factor = Math.pow(0.9, level - 1); // 10% menos por nivel
        return (int) Math.max(100, cooldownMs * factor); // mínimo 100ms
    }

    /**
     * Verifica si la habilidad puede ser usada (cooldown disponible)
     */
    public boolean canUse() {
        if (!unlocked || level <= 0) return false;
        long timeSinceLastUse = System.currentTimeMillis() - lastUsedTime;
        return timeSinceLastUse >= getEffectiveCooldownMs();
    }

    /**
     * Obtiene el tiempo restante de cooldown en ms (basado en cooldown efectivo)
     */
    public long getRemainingCooldown() {
        long timeSinceLastUse = System.currentTimeMillis() - lastUsedTime;
        long remaining = getEffectiveCooldownMs() - timeSinceLastUse;
        return Math.max(0, remaining);
    }
}
