package com.example.kaisenclicker.ui.components;

import androidx.annotation.DrawableRes;

public class SkillData {
    private int iconRes;
    private long cooldown;
    private int level;

    public SkillData(@DrawableRes int iconRes, long cooldown, int level) {
        this.iconRes = iconRes;
        this.cooldown = cooldown;
        this.level = level;
    }

    public int getIconRes() {
        return iconRes;
    }

    public long getCooldown() {
        // El cooldown se reduce un 10% por cada nivel adicional
        return (long) (cooldown * Math.pow(0.9, level - 1));
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
