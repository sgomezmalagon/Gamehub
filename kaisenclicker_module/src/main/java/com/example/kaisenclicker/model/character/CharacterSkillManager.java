package com.example.kaisenclicker.model.character;

import com.example.kaisenclicker.model.skill.Skill;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de habilidades del personaje
 * Controla qué habilidades están desbloqueadas y sus niveles
 */
public class CharacterSkillManager {
    private List<Skill> skills;

    public CharacterSkillManager() {
        this.skills = new ArrayList<>();
        initializeSkills();
    }

    /**
     * Inicializa las habilidades del personaje
     */
    private void initializeSkills() {
        // Habilidad 1: Cleave - Ataque básico con sangrado
        Skill cleave = new Skill(
            "cleave",
            "Cleave",
            "Ataque que inflige sangrado. Daño: 100% + 20% del daño base por nivel",
            Skill.SkillType.NORMAL_1,
            10
        );
        cleave.setCooldownMs(2000); // 2 segundos de cooldown
        // Parámetros de sangrado: 5s total, tick cada 1s, 20% base + 5% por nivel
        cleave.setBleedDurationMs(5000);
        cleave.setBleedTickMs(1000);
        cleave.setBleedBaseFactor(0.2f);
        cleave.setBleedPerLevelFactor(0.05f);
        skills.add(cleave);

        // Habilidad 2: Dismantle - Ataque especial
        Skill dismantle = new Skill(
            "dismantle",
            "Dismantle",
            "Ataque devastador que reduce defensa. Daño: 150% + 30% del daño base por nivel",
            Skill.SkillType.NORMAL_2,
            10
        );
        dismantle.setCooldownMs(3000); // 3 segundos de cooldown
        skills.add(dismantle);

        // Habilidad 3: Fuga - Evasión
        Skill fuga = new Skill(
            "fuga",
            "Fuga",
            "Esquiva el próximo ataque. Daño: 80% + 15% del daño base por nivel",
            Skill.SkillType.NORMAL_3,
            10
        );
        fuga.setCooldownMs(4000); // 4 segundos de cooldown
        skills.add(fuga);

        // Habilidad Definitiva: Domain Expansion
        Skill domain = new Skill(
            "domain",
            "Expansion de Dominio",
            "Ataque definitivo masivo. Daño: 300% + 50% del daño base por nivel",
            Skill.SkillType.ULTIMATE,
            5
        );
        domain.setCooldownMs(8000); // 8 segundos de cooldown
        skills.add(domain);
    }

    /**
     * Obtiene una habilidad por ID
     */
    public Skill getSkillById(String id) {
        for (Skill skill : skills) {
            if (skill.getId().equals(id)) {
                return skill;
            }
        }
        return null;
    }

    /**
     * Obtiene todas las habilidades del personaje
     */
    public List<Skill> getAllSkills() {
        return new ArrayList<>(skills);
    }

    /**
     * Obtiene las habilidades desbloqueadas
     */
    public List<Skill> getUnlockedSkills() {
        List<Skill> unlockedSkills = new ArrayList<>();
        for (Skill skill : skills) {
            if (skill.isUnlocked() && skill.getLevel() > 0) {
                unlockedSkills.add(skill);
            }
        }
        return unlockedSkills;
    }

    /**
     * Desbloquea una habilidad (la primera vez)
     */
    public void unlockSkill(String skillId) {
        Skill skill = getSkillById(skillId);
        if (skill != null && !skill.isUnlocked()) {
            skill.setUnlocked(true);
            skill.setLevel(1);
        }
    }

    /**
     * Sube el nivel de una habilidad
     */
    public boolean levelUpSkill(String skillId) {
        Skill skill = getSkillById(skillId);
        if (skill != null && skill.isUnlocked()) {
            int newLevel = Math.min(skill.getLevel() + 1, skill.getMaxLevel());
            skill.setLevel(newLevel);
            return newLevel < skill.getMaxLevel();
        }
        return false;
    }

    /**
     * Calcula el daño de una habilidad basado en el daño base
     */
    public int calculateSkillDamage(String skillId, int baseDamage) {
        Skill skill = getSkillById(skillId);
        if (skill == null) return 0;

        int skillLevel = skill.getLevel();
        int damageMultiplier;

        switch (skill.getType()) {
            case NORMAL_1: // Cleave: 100% + 20% por nivel
                damageMultiplier = (int) (baseDamage * (1.0f + (0.2f * skillLevel)));
                break;
            case NORMAL_2: // Dismantle: (handled separately as percent of enemy HP)
                // Keep a fallback if someone calls calculateSkillDamage for dismantle
                damageMultiplier = (int) (baseDamage * (1.5f + (0.3f * skillLevel)));
                break;
            case NORMAL_3: // Fuga: golpe devastador — mayor escala por nivel
                // Escala más agresiva: base 120% + 35% por nivel
                damageMultiplier = (int) (baseDamage * (1.2f + (0.35f * skillLevel)));
                break;
            case ULTIMATE: // Domain: serie de cortes con alto daño, mayor escala por nivel
                // Escala más agresiva: base 300% + 60% por nivel
                damageMultiplier = (int) (baseDamage * (3.0f + (0.6f * skillLevel)));
                break;
            default:
                damageMultiplier = baseDamage;
        }

        return damageMultiplier;
    }

    /**
     * Calcula el daño de Dismantle en función de la vida actual del enemigo.
     * Dismantle quita un porcentaje de la vida actual del enemigo; el porcentaje aumenta con el nivel de la habilidad.
     * Parámetros: enemyHp (vida actual, no la máxima), enemyRes (no usado aquí), enemyLevel y skillId.
     */
    public int calculateDismantleDamage(int enemyHp, int enemyRes, int enemyLevel, String skillId) {
        Skill skill = getSkillById(skillId);
        if (skill == null) return 0;
        int skillLevel = skill.getLevel();

        // Porcentaje base y por nivel: por ejemplo base 12% + 3% por nivel
        float basePercent = 0.12f;
        float perLevel = 0.03f;
        float percent = basePercent + (perLevel * skillLevel);
        // Cap al 80% para evitar eliminación instantánea a niveles extremos
        percent = Math.min(percent, 0.80f);

        int damage = Math.max(1, Math.round(enemyHp * percent));
        return damage;
    }

    /**
     * Marca una habilidad como usada (inicia cooldown)
     */
    public void useSkill(String skillId) {
        Skill skill = getSkillById(skillId);
        if (skill != null && skill.canUse()) {
            skill.setLastUsedTime(System.currentTimeMillis());
        }
    }
}
