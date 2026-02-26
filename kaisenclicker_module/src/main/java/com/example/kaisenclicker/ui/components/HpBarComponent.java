package com.example.kaisenclicker.ui.components;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.kaisenclicker.R;

/**
 * Barra de vida profesional inspirada en Jujutsu Kaisen: Phantom Parade
 *
 * Características:
 * - Degradado dinámico: Verde → Amarillo → Rojo según % de vida
 * - Animaciones suaves con easing personalizado
 * - Efecto de daño: reducción instantánea + transición fluida
 * - Soporte para HP dinámico
 * - Escalable en diferentes resoluciones
 */
public class HpBarComponent extends LinearLayout {

    private ProgressBar hpBar;
    private ProgressBar armorBar;
    private TextView hpText;
    private TextView enemyName;
    private TextView enemyLevel;
    // Mahoraga stacks indicator (se mostrará junto al enemyName)
    private TextView mahoragaStacksView;
    private ImageView enemyBossIcon;
    private ObjectAnimator animator;
    private ObjectAnimator armorAnimator;

    private int currentHP = 100;
    private int maxHP = 100;
    // Armor values (applies only to bosses or when explicitly set)
    private int currentArmor = 0;
    private int maxArmor = 0;

    private static final int DAMAGE_ANIMATION_DURATION = 600;    // ms
    private static final int ARMOR_ANIMATION_DURATION = 420;     // ms
    private static final int HEAL_ANIMATION_DURATION = 400;      // ms
    private static final TimeInterpolator DAMAGE_INTERPOLATOR = new DecelerateInterpolator(1.8f);
    private static final TimeInterpolator HEAL_INTERPOLATOR = new OvershootInterpolator(0.8f);

    public HpBarComponent(Context context) {
        super(context);
        init(context);
    }

    public HpBarComponent(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HpBarComponent(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.hp_bar_component, this);
        hpBar = findViewById(R.id.hp_bar_progress);
        armorBar = findViewById(R.id.armor_bar_progress);
        // hp_text was removed from the layout (no numeric HP shown). Keep hpText null-safe.
        hpText = null; // findViewById(R.id.hp_text) removed intentionally
        enemyName = findViewById(R.id.enemy_name);
        enemyLevel = findViewById(R.id.enemy_level);
        enemyBossIcon = findViewById(R.id.enemy_boss_icon);

        // Crear la vista de stacks de Mahoraga y añadirla junto al nombre si es posible
        try {
            mahoragaStacksView = new TextView(context);
            mahoragaStacksView.setTag("mahoraga_stacks_view");
            mahoragaStacksView.setTextSize(12f);
            mahoragaStacksView.setTypeface(mahoragaStacksView.getTypeface(), android.graphics.Typeface.BOLD);
            mahoragaStacksView.setTextColor(0xFFFB8C00); // naranja
            mahoragaStacksView.setVisibility(GONE);
            // Intentar insertar junto al enemyName dentro de este LinearLayout
            int insertIndex = -1;
            for (int i = 0; i < getChildCount(); i++) {
                View ch = getChildAt(i);
                if (ch == enemyName) { insertIndex = i + 1; break; }
            }
            if (insertIndex >= 0) {
                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(dpToPx(6), 0, 0, 0);
                addView(mahoragaStacksView, insertIndex, lp);
            } else {
                // fallback: añadir al final
                addView(mahoragaStacksView);
            }
        } catch (Exception ignored) {}

        // Configuración inicial
        hpBar.setMax(maxHP);
        hpBar.setProgress(currentHP);
        if (armorBar != null) {
            armorBar.setMax(Math.max(1, maxArmor));
            armorBar.setProgress(currentArmor);
            armorBar.setVisibility(currentArmor > 0 ? VISIBLE : GONE);
        }
    }

    /**
     * Establece el HP máximo e inicializa la barra
     */
    public void setMaxHealth(int maxHP) {
        this.maxHP = Math.max(1, maxHP);
        this.currentHP = this.maxHP;
        hpBar.setMax(this.maxHP);
        hpBar.setProgress(this.currentHP);
        updateDisplay();
    }

    /**
     * Actualiza el nivel del enemigo
     */
    public void setEnemyLevel(int level) {
        if (enemyLevel != null) {
            enemyLevel.setText("Level " + level);
        }
    }

    /**
     * Muestra u oculta el icono de boss junto al nombre.
     */
    public void setBossVisible(boolean visible) {
        try {
            if (enemyBossIcon != null) {
                enemyBossIcon.setVisibility(visible ? VISIBLE : GONE);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Actualiza el nombre del enemigo
     */
    public void setEnemyName(String name) {
        if (enemyName != null) {
            enemyName.setText(name);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** Muestra o actualiza el contador de stacks de Mahoraga junto al nombre. */
    public void setMahoragaStacks(int stacks) {
        try {
            if (mahoragaStacksView == null) return;
            if (stacks <= 0) {
                mahoragaStacksView.setVisibility(GONE);
            } else {
                // Mostrar como ' xN' para que quede cerca del nombre
                mahoragaStacksView.setText(" x" + stacks);
                mahoragaStacksView.setVisibility(VISIBLE);
            }
        } catch (Exception ignored) {}
    }

    /** Oculta el indicador de stacks de Mahoraga. */
    public void clearMahoragaStacks() {
        try { if (mahoragaStacksView != null) mahoragaStacksView.setVisibility(GONE); } catch (Exception ignored) {}
    }

    /**
     * Aplica daño con animación de impacto
     * Efecto: reducción instantánea pequeña + transición suave
     */
    public void takeDamage(int damage) {
        int remaining = damage;
        // Si hay armadura, consumirla primero
        if (currentArmor > 0 && armorBar != null) {
            int armorDamage = Math.min(currentArmor, remaining);
            int armorFrom = currentArmor;
            int armorTo = Math.max(0, currentArmor - armorDamage);
            // Cancel any running armor animation
            if (armorAnimator != null && armorAnimator.isRunning()) armorAnimator.cancel();
            // Ensure armor bar visible
            armorBar.setVisibility(VISIBLE);
            // Animate armor progress from armorFrom to armorTo
            try {
                armorAnimator = ObjectAnimator.ofInt(armorBar, "progress", armorFrom, armorTo);
                armorAnimator.setDuration(ARMOR_ANIMATION_DURATION);
                armorAnimator.setInterpolator(DAMAGE_INTERPOLATOR);
                armorAnimator.addUpdateListener(anim -> {
                    try { currentArmor = armorBar.getProgress(); } catch (Exception ignored) {}
                });
                armorAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(android.animation.Animator animation) {
                        try {
                            currentArmor = armorTo;
                            armorBar.setProgress(currentArmor);
                            if (currentArmor <= 0) armorBar.setVisibility(GONE);
                        } catch (Exception ignored) {}
                    }
                });
                armorAnimator.start();
            } catch (Exception ignored) {
                // Fallback: set instantly
                setArmor(armorTo);
            }
            remaining -= armorDamage;
            // Si todavía hay armadura, no tocar HP
            if (remaining <= 0) return;
        } else if (currentArmor > 0) {
            // No armorBar available, apply instantly
            int armorDamage = Math.min(currentArmor, remaining);
            currentArmor = Math.max(0, currentArmor - armorDamage);
            if (armorBar != null) armorBar.setProgress(currentArmor);
            remaining -= armorDamage;
            if (remaining <= 0) return;
        }

        int newHP = Math.max(0, currentHP - remaining);
        // Small overshoot visual effect (kept for compatibility)
        int damageOvershoot = Math.max(0, currentHP - remaining - 2);
        animateHealthChange(currentHP, newHP, DAMAGE_ANIMATION_DURATION, DAMAGE_INTERPOLATOR);
        currentHP = newHP;
    }

    /**
     * Cura con animación suave
     */
    public void heal(int amount) {
        int newHP = Math.min(maxHP, currentHP + amount);
        animateHealthChange(currentHP, newHP, HEAL_ANIMATION_DURATION, HEAL_INTERPOLATOR);
        currentHP = newHP;
    }

    /**
     * Establece el HP sin animación (útil para inicialización)
     */
    public void setHealth(int hp) {
        this.currentHP = Math.min(hp, maxHP);
        hpBar.setProgress(currentHP);
        updateDisplay();
    }

    /**
     * Anima el cambio de vida con interpolador personalizado
     */
    private void animateHealthChange(int from, int to, int duration, TimeInterpolator interpolator) {
        // Cancelar animación anterior si está en progreso
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }

        animator = ObjectAnimator.ofInt(hpBar, "progress", from, to);
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(animation -> updateDisplay());
        animator.start();
    }

    /**
     * Actualiza el texto de HP dinámicamente
     */
    private void updateDisplay() {
        currentHP = hpBar.getProgress();

        // No numeric HP text shown by design. If hpText exists (legacy), update it safely.
        if (hpText != null) {
            hpText.setText(currentHP + " / " + maxHP);
            updateTextColor();
        }
    }

    /**
     * Cambia el color del texto según el porcentaje de vida
     */
    private void updateTextColor() {
        if (hpText == null) return; // nothing to do if text view absent

        int percentage = getHealthPercentage();
        int textColor;

        if (percentage >= 60) {
            textColor = 0xFFFFFFFF; // Blanco - Vida alta
        } else if (percentage >= 30) {
            textColor = 0xFFFBBF24; // Amarillo - Vida media
        } else {
            textColor = 0xFFEF4444; // Rojo - Vida baja
        }

        hpText.setTextColor(textColor);
    }

    /**
     * Obtiene el porcentaje de vida (0-100)
     */
    public int getHealthPercentage() {
        if (maxHP == 0) return 0;
        return (int) ((float) currentHP / maxHP * 100);
    }

    /**
     * Obtiene el HP currente
     */
    public int getCurrentHP() {
        return currentHP;
    }

    /**
     * Establece la armadura máxima y la resetea al valor dado
     */
    public void setArmor(int armor) {
        this.maxArmor = Math.max(0, armor);
        this.currentArmor = Math.max(0, Math.min(armor, maxArmor));
        if (armorBar != null) {
            armorBar.setMax(Math.max(1, maxArmor));
            armorBar.setProgress(currentArmor);
            armorBar.setVisibility(currentArmor > 0 ? VISIBLE : GONE);
        }
    }

    public void setArmorMaxAndCurrent(int max, int current) {
        this.maxArmor = Math.max(0, max);
        this.currentArmor = Math.max(0, Math.min(current, this.maxArmor));
        if (armorBar != null) {
            armorBar.setMax(Math.max(1, this.maxArmor));
            armorBar.setProgress(this.currentArmor);
            armorBar.setVisibility(this.currentArmor > 0 ? VISIBLE : GONE);
        }
    }

    public int getArmorCurrent() { return currentArmor; }
    public int getArmorMax() { return maxArmor; }

    public void setArmorVisible(boolean visible) {
        if (armorBar != null) armorBar.setVisibility(visible ? VISIBLE : GONE);
    }

    /**
     * Limpia las animaciones al destruir
     */
    @Override
    public void onDetachedFromWindow() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        if (armorAnimator != null && armorAnimator.isRunning()) {
            armorAnimator.cancel();
        }
        super.onDetachedFromWindow();
    }
}
