package com.example.kaisenclicker.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import android.os.Handler;
import android.os.Looper;
import android.animation.ObjectAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.CycleInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.ViewGroup;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.kaisenclicker.R;
import com.example.kaisenclicker.model.character.CharacterSkillManager;
import com.example.kaisenclicker.model.skill.Skill;
import com.example.kaisenclicker.ui.activities.MainActivity;
import com.example.kaisenclicker.ui.components.HpBarComponent;
import com.example.kaisenclicker.ui.components.SkillButtonView;
import com.example.kaisenclicker.persistence.save.GameDataManager;
import com.google.android.material.card.MaterialCardView;

import java.util.Random;

public class CampaignFragment extends Fragment {

    // UI Elements
    private MaterialCardView enemyCard;
    // Guardar Drawable original del fondo para poder restaurarlo tras la ulti
    private Drawable originalBackgroundDrawable = null;
    private TextView tvPlayerEnergy;
    private View energyDisplayCard;
    private HpBarComponent hpBar;
    private SkillButtonView btnSkill1, btnSkill2, btnSkill3, btnSkillUltimate;
    private View btnToggleAutoClicker;
    private View cardAutoClickerToggle;
    private View autoclickerStatusDot;
    private TextView tvAutoClickerLabel;
    private TextView tvAutoClickerState;
    private LinearLayout skillBar;
    private ImageView ivEnemyYusepe; // ImageView del enemigo
    private ProgressBar ultimateProgressBar;
    private TextView tvUltimatePercent;
    private TextView tvDamagePopup; // nuevo
    private TextView tvDebugOverlay;
    private ImageView skillGifOverlay; // Fullscreen GIF overlay for skill animations

    // Handler para refrescar cooldowns en tiempo real
    private final Handler cooldownHandler = new Handler(Looper.getMainLooper());
    private final Runnable cooldownRefresher = new Runnable() {
        @Override
        public void run() {
            updateSkillButtons();
            cooldownHandler.postDelayed(this, 200);
        }
    };

    // Enemy Stats
    private int enemyLevel = 1;
    private int enemyMaxHp;
    private int enemyCurrentHp;
    private int damagePerTap = 10;

    // Calcula el HP máximo de un enemigo según su nivel.
    // Uso de crecimiento exponencial para que niveles altos escalen significativamente.
    // growthFactor controla lo rápido que sube la vida por nivel (1.0 = sin crecimiento).
    private int calculateEnemyMaxHp(int level) {
        if (level <= 1) return 50;
        final double baseHp = 45.0; // HP base para nivel 1
        final double growthFactor = 1.11; // ajustar para velocidad de escalado (1.10-1.13 recomendado)
        // Limitar exponent para evitar overflow en casos extremos
        double exponent = Math.max(0, level - 1);
        // Calcular hp y redondear
        double hpDouble = baseHp * Math.pow(growthFactor, exponent);
        // Protección: cap razonable para evitar desbordamientos
        double maxCap = 5e8; // 500 millones como tope razonable
        if (hpDouble > maxCap) hpDouble = maxCap;
        return Math.max(10, (int) Math.round(hpDouble));
    }

    // Mantener referencia del drawable original para revertir
    private int ivEnemyOriginalRes = R.drawable.yusepe;
    // Guardar el ScaleType original del ImageView del enemigo como ordinal para restaurarlo después de transformaciones
    private int originalEnemyScaleTypeOrdinal = android.widget.ImageView.ScaleType.CENTER_CROP.ordinal();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Skill System
    private CharacterSkillManager skillManager;

    // AutoClicker Stats
    private final int autoClickerLevel = 0;
    private boolean autoClickerEnabled = false;

    // Ultimate charge
    private int ultimateCharge = 0; // 0-100
    private final int ULTIMATE_CHARGE_MAX = 100;

    private Random random;

    // Mantener referencia para cancelar el revert programado si hay múltiples clicks
    private Runnable revertImageRunnable = null;

    // Mantener Runnables activos de sangrado para poder cancelarlos al cambiar de enemigo
    private static class BleedTask {
        Runnable r;
        boolean persistent; // si true, no se cancela al spawnear nuevo enemigo
        BleedTask(Runnable r, boolean persistent) { this.r = r; this.persistent = persistent; }
    }
    private final java.util.List<BleedTask> activeBleedRunnables = new java.util.ArrayList<>();

    // Flags para rastrear si se ha usado Cleave y Dismantle en el combate actual
    private boolean cleaveUsedThisFight = false;
    private boolean dismantleUsedThisFight = false;

    // --- DPS tracking (ventana deslizante de 3s) ---
    private static final long DPS_WINDOW_MS = 3000L;
    private final java.util.List<long[]> dpsLog = new java.util.ArrayList<>(); // {timestamp, daño}
    private double currentRealtimeDps = 0.0;
    private final Runnable dpsRefresher = new Runnable() {
        @Override
        public void run() {
            recalculateRealtimeDps();
            mainHandler.postDelayed(this, 500);
        }
    };

    // Gojo domain state: cuando true las habilidades de Gojo no tendrán cooldowns y Purpura será gratis
    private boolean gojoDomainActive = false;
    private Runnable gojoDomainEndRunnable = null;
    // Sukuna domain state: cuando true el dominio sigue activo y el daño persiste entre enemigos
    private boolean sukunaDomainActive = false;
    private final java.util.List<Runnable> domainRunnables = new java.util.ArrayList<>();
    // Flag para saber si el enemigo actual es un boss
    private boolean isBossEnemy = false;
    // Estado del enemigo actual
    private String currentEnemyId = null;
    private int currentArmorValue = 0;
    // Transformación específica para Mahito
    private boolean mahitoTransformed = false;

    // Mahoraga adaptation (boss ability)
    private boolean mahoragaAdaptationActive = false;
    private int mahoragaAdaptationStacks = 0;
    private final int MAHORAGA_MAX_ADAPT_STACKS = 5; // tope de adaptación
    private final float MAHORAGA_ADAPT_PER_STACK = 0.08f; // 8% menos daño por stack (habilidades)
    private final long MAHORAGA_ADAPT_TICK_MS = 5000L; // cada 5s gana 1 stack
    private int mahoragaHitsReceived = 0; // contador de golpes recibidos (taps + habilidades)
    private final float MAHORAGA_HIT_REDUCTION_PER_HIT = 0.005f; // 0.5% menos daño por golpe recibido
    private final float MAHORAGA_HIT_REDUCTION_MAX = 0.35f; // máximo 35% de reducción por golpes
    private Runnable mahoragaAdaptRunnable = null;
    // Timestamp del último spawn para prevenir que DoTs persistentes maten inmediatamente al siguiente enemigo
    private long lastSpawnTimestampMs = 0L;
    private static final long PERSISTENT_BLEED_GRACE_MS = 700L; // ms de gracia tras spawn
    // UI elements for Mahoraga adaptation (created at runtime)
    private ImageView mahoragaAdaptIcon = null;
    private TextView mahoragaAdaptText = null;
    private static final String MAHORAGA_ADAPT_CONTAINER_TAG = "mahoraga_adapt_ui";

    // Boss identifiers (expandible)
    private static final String BOSS_CHOSO = "choso";
    // Identificador específico para la segunda fase de Choso (persistible)
    private static final String BOSS_CHOSO_SECOND = "choso_second_phase";
    private static final String BOSS_MAHITO = "mahito"; // ya implementado parcialmente
    private static final String BOSS_MAHORAGA = "mahoraga";

    // Estado y constantes para la segunda fase específica de Choso
    private boolean chosoSecondPhaseActive = false;
    private static final float CHOSO_SECOND_PHASE_DAMAGE_MULTIPLIER = 0.6f; // recibe 60% del daño en 2ª fase

    // Helpers para resolver recursos de boss por id
    private int getBossDrawableRes(String bossId) {
        if (bossId == null) return 0;
        switch (bossId) {
            case BOSS_CHOSO:
                return R.drawable.choso_boss;
            case BOSS_CHOSO_SECOND:
                return R.drawable.choso_boss_second_phase;
            case BOSS_MAHITO:
                return R.drawable.mahito;
            case BOSS_MAHORAGA:
                // Si agregaste el drawable mahoraga_boss, usarlo aquí
                return R.drawable.mahoraga_boss;
            default:
                return 0;
        }
    }

    private int getBossDamagedDrawableRes(String bossId) {
        if (bossId == null) return 0;
        switch (bossId) {
            case BOSS_CHOSO:
                return R.drawable.damaged_choso_boss;
            case BOSS_CHOSO_SECOND:
                // En segunda fase seguimos devolviendo el drawable dañado si alguna lógica lo pide,
                // aunque normalmente no reproduciremos la animación de daño en 2ª fase.
                return R.drawable.damaged_choso_boss;
            case BOSS_MAHITO:
                return R.drawable.damaged_mahito;
            case BOSS_MAHORAGA:
                return 0; // Mahoraga no cambia de imagen al recibir daño
            default:
                return 0;
        }
    }

    private String getBossDisplayName(String bossId) {
        if (bossId == null) return "Enemy";
        switch (bossId) {
            case BOSS_CHOSO: return "Choso";
            case BOSS_CHOSO_SECOND: return "Choso (Segunda Fase)";
            case BOSS_MAHITO: return "Mahito";
            case BOSS_MAHORAGA: return "Mahoraga";
            default: return "Enemy";
        }
    }

    // Helper: busca un drawable entre varios nombres y devuelve el id (0 si no existe)
    private int findDrawableByNames(String... names) {
        if (getContext() == null) return 0;
        for (String n : names) {
            int id = getResources().getIdentifier(n, "drawable", getContext().getPackageName());
            if (id != 0) return id;
        }
        return 0;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_campaign, container, false);

        // Guardar el fondo original del fragment para poder restaurarlo tras efectos temporales
        try { originalBackgroundDrawable = view.getBackground(); } catch (Exception ignored) {}

        random = new Random();
        // Preferir el SkillManager central en GameDataManager para que los cambios persistidos
        // (niveles/desbloqueos) se reflejen en la UI de combate.
        skillManager = null;
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            try { skillManager = mainActivity.getGameDataManager().getSkillManager(); } catch (Exception ignored) {}
        }
        if (skillManager == null) {
            // Fallback local instance (solo si no hay GameDataManager disponible)
            skillManager = new CharacterSkillManager();
        }
        // Sync persisted skill levels/unlocks from GameDataManager into the active skillManager
        try {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                com.example.kaisenclicker.persistence.save.GameDataManager gdm = mainActivity.getGameDataManager();
                // default to Ryomen Sukuna for now; if you implement character selection, pass that id
                int charId = GameDataManager.CHAR_ID_RYOMEN_SUKUNA;
                syncSkillsFromGameDataManager(gdm, charId);
            }
        } catch (Exception ignored) {}

        // Cargar nivel del enemigo guardado
        enemyLevel = 1;
        try {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                enemyLevel = mainActivity.getGameDataManager().getEnemyLevel();
                if (enemyLevel < 1) enemyLevel = 1;
            }
        } catch (Exception ignored) {}

        enemyCard = view.findViewById(R.id.enemy_card);
        tvPlayerEnergy = view.findViewById(R.id.tv_player_energy);
        energyDisplayCard = view.findViewById(R.id.energy_display_card);
        hpBar = view.findViewById(R.id.hp_bar_component);
        tvDebugOverlay = view.findViewById(R.id.tv_debug_overlay);
        skillGifOverlay = view.findViewById(R.id.skill_gif_overlay);
        btnToggleAutoClicker = view.findViewById(R.id.btn_toggle_autoclicker);
        cardAutoClickerToggle = view.findViewById(R.id.card_autoclicker_toggle);
        autoclickerStatusDot = view.findViewById(R.id.autoclicker_status_dot);
        tvAutoClickerLabel = view.findViewById(R.id.tv_autoclicker_label);
        tvAutoClickerState = view.findViewById(R.id.tv_autoclicker_state);
        skillBar = view.findViewById(R.id.skill_bar);

        // Referencia al ImageView del enemigo (para animaciones de daño)
        ivEnemyYusepe = view.findViewById(R.id.iv_enemy_yusepe);
        tvDamagePopup = view.findViewById(R.id.tv_damage_popup);
        // Aseguramos el drawable original (en caso de que se cambie en layout)
        ivEnemyOriginalRes = R.drawable.yusepe;
        try { originalEnemyScaleTypeOrdinal = ivEnemyYusepe.getScaleType().ordinal(); } catch (Exception ignored) {}

        // SkillButtonViews (iconos y listeners)
        btnSkill1 = view.findViewById(R.id.btn_skill_1);
        btnSkill2 = view.findViewById(R.id.btn_skill_2);
        btnSkill3 = view.findViewById(R.id.btn_skill_3);
        btnSkillUltimate = view.findViewById(R.id.btn_skill_ultimate);
        // Asegurar que el icono de la ultimate muestre la Expansión de Dominio (según personaje seleccionado)
        if (btnSkillUltimate != null) {
            try {
                int sel = GameDataManager.CHAR_ID_RYOMEN_SUKUNA;
                if (getActivity() != null && getActivity() instanceof MainActivity) {
                    MainActivity m = (MainActivity) getActivity();
                    try { sel = m.getGameDataManager().getSelectedCharacterId(); } catch (Exception ignored) {}
                }
                if (sel == GameDataManager.CHAR_ID_SATORU_GOJO) btnSkillUltimate.setSkillIcon(R.drawable.gojo_domain);
                else btnSkillUltimate.setSkillIcon(R.drawable.sukuna_domain);
            } catch (Exception ignored) { btnSkillUltimate.setSkillIcon(R.drawable.sukuna_domain); }
        }

        // Determinar personaje seleccionado para descripciones
        int selectedChar = GameDataManager.CHAR_ID_RYOMEN_SUKUNA;
        try {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                selectedChar = ((MainActivity) getActivity()).getGameDataManager().getSelectedCharacterId();
            }
        } catch (Exception ignored) {}
        boolean isGojo = (selectedChar == GameDataManager.CHAR_ID_SATORU_GOJO);

        btnSkill1.setOnSkillClickListener(v -> useSkill("cleave"));
        btnSkill1.setSkillDescription(isGojo
                ? getString(R.string.skill_amplificacion_azul_name) + ": " + getString(R.string.skill_amplificacion_azul_desc)
                : "Cleave: Daño base + sangrado. Causa daño adicional por nivel.");
        btnSkill1.setOnSkillLongClickListener();
        btnSkill2.setOnSkillClickListener(v -> useSkill("dismantle"));
        btnSkill2.setSkillDescription(isGojo
                ? getString(R.string.skill_ritual_inverso_rojo_name) + ": " + getString(R.string.skill_ritual_inverso_rojo_desc)
                : "Dismantle: Daño aumentado y reduce defensa del enemigo.");
        btnSkill2.setOnSkillLongClickListener();
        btnSkill3.setOnSkillClickListener(v -> useSkill("fuga"));
        btnSkill3.setSkillDescription(isGojo
                ? getString(R.string.skill_vacio_purpura_name) + ": " + getString(R.string.skill_vacio_purpura_desc)
                : "Fuga: Evasión y contraataque. Daño y esquiva.");
        btnSkill3.setOnSkillLongClickListener();
        btnSkillUltimate.setOnSkillClickListener(v -> {
            if (ultimateCharge >= ULTIMATE_CHARGE_MAX) {
                useSkill("domain");
                setUltimateCharge(0);
            }
        });
        // Set a character-specific description for the ultimate (Gojo has a special domain behavior)
        try {
            int sel = GameDataManager.CHAR_ID_RYOMEN_SUKUNA;
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                MainActivity m = (MainActivity) getActivity();
                try { sel = m.getGameDataManager().getSelectedCharacterId(); } catch (Exception ignored) {}
            }
            if (sel == GameDataManager.CHAR_ID_SATORU_GOJO) {
                btnSkillUltimate.setSkillDescription("Expansión de Dominio: Durante la duración del dominio tus habilidades no tendrán cooldown.");
            } else {
                btnSkillUltimate.setSkillDescription("Domain: Expansión de Dominio. Daño masivo a todos los enemigos.");
            }
        } catch (Exception ignored) {
            btnSkillUltimate.setSkillDescription("Domain: Expansión de Dominio. Daño masivo a todos los enemigos.");
        }
        btnSkillUltimate.setOnSkillLongClickListener();

        // Click listener en el enemigo para atacar
        enemyCard.setOnClickListener(v -> attackEnemy());
        // Long click: si es boss, mostrar sus habilidades; si es debug, resetear enemigos
        enemyCard.setOnLongClickListener(v -> {
            // Si es un boss, mostrar info de habilidades
            if (isBossEnemy && currentEnemyId != null) {
                showBossAbilitiesDialog(currentEnemyId);
                return true;
            }
            // Debug: reset enemigos a nivel 1
            try {
                if (getActivity() != null && getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (mainActivity.isDebugBuildPublic()) {
                        resetEnemiesToLevelOne();
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            return false;
        });
        setupAutoClickerButton();
        initializeSkills();
        spawnNewEnemy();
        updateEnergyDisplay();
        // Start refresher
        cooldownHandler.post(cooldownRefresher);
        // Iniciar tracker de DPS en tiempo real
        mainHandler.post(dpsRefresher);

        autoClickerEnabled = false;
        stopAutoClicker();

        // Referencia a la barra de carga de ultimate
        ultimateProgressBar = view.findViewById(R.id.ultimate_progress_bar);
        tvUltimatePercent = view.findViewById(R.id.tv_ultimate_percent);
        // Restaurar progreso guardado de la ulti (SharedPreferences/SQLite)
        int savedUlti = 0;
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            try { savedUlti = mainActivity.getGameDataManager().getUltiProgress(); } catch (Exception ignored) {}
        }
        setUltimateCharge(savedUlti);

        return view;
    }

    private void setupAutoClickerButton() {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            int acLevel = mainActivity.getGameDataManager().getAutoClickerLevel();

            // Siempre mostrar el botón
            if (cardAutoClickerToggle != null) cardAutoClickerToggle.setVisibility(View.VISIBLE);

            if (acLevel > 0) {
                // ── COMPRADO: permitir toggle on/off ──
                if (cardAutoClickerToggle != null) {
                    cardAutoClickerToggle.setAlpha(1f);
                    cardAutoClickerToggle.setClickable(true);
                }

                // Restaurar el estado guardado (por defecto OFF)
                autoClickerEnabled = mainActivity.getGameDataManager().isAutoClickerEnabled();

                if (autoClickerEnabled) {
                    startAutoClicker(acLevel);
                } else {
                    stopAutoClicker();
                }
                updateAutoClickerButtonUI();

                View clickTarget = (cardAutoClickerToggle != null) ? cardAutoClickerToggle : btnToggleAutoClicker;
                if (clickTarget != null) {
                    clickTarget.setOnClickListener(v -> {
                        int currentLevel = mainActivity.getGameDataManager().getAutoClickerLevel();
                        if (autoClickerEnabled) {
                            stopAutoClicker();
                            autoClickerEnabled = false;
                        } else {
                            autoClickerEnabled = true;
                            startAutoClicker(currentLevel);
                        }
                        // Persistir estado on/off
                        mainActivity.getGameDataManager().saveAutoClickerEnabled(autoClickerEnabled);
                        updateAutoClickerButtonUI();
                        // Pequeña animación de pulso al pulsar
                        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80)
                            .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                            .start();
                    });
                }
            } else {
                // ── NO COMPRADO: botón visible pero bloqueado ──
                stopAutoClicker();
                autoClickerEnabled = false;
                updateAutoClickerLockedUI();

                if (cardAutoClickerToggle != null) {
                    cardAutoClickerToggle.setAlpha(0.45f);
                    cardAutoClickerToggle.setClickable(false);
                }
                View clickTarget = (cardAutoClickerToggle != null) ? cardAutoClickerToggle : btnToggleAutoClicker;
                if (clickTarget != null) {
                    clickTarget.setOnClickListener(null);
                }
            }
        } else {
            stopAutoClicker();
            autoClickerEnabled = false;
        }
    }

    private void updateAutoClickerLockedUI() {
        if (btnToggleAutoClicker != null) {
            btnToggleAutoClicker.setBackgroundResource(R.drawable.bg_autoclicker_off);
        }
        if (autoclickerStatusDot != null) {
            autoclickerStatusDot.setBackgroundResource(R.drawable.dot_autoclicker_off);
        }
        if (tvAutoClickerLabel != null) {
            tvAutoClickerLabel.setTextColor(0xFF444444);
            tvAutoClickerLabel.setText("AUTO");
        }
        if (tvAutoClickerState != null) {
            tvAutoClickerState.setText("🔒");
            tvAutoClickerState.setTextColor(0xFF555555);
        }
    }

    private void updateAutoClickerButtonUI() {
        if (btnToggleAutoClicker == null) return;
        if (autoClickerEnabled) {
            btnToggleAutoClicker.setBackgroundResource(R.drawable.bg_autoclicker_on);
            if (autoclickerStatusDot != null) autoclickerStatusDot.setBackgroundResource(R.drawable.dot_autoclicker_on);
            if (tvAutoClickerLabel != null) tvAutoClickerLabel.setTextColor(0xFF90A4AE);
            if (tvAutoClickerState != null) {
                tvAutoClickerState.setText("ON");
                tvAutoClickerState.setTextColor(0xFF00CED1);
            }
        } else {
            btnToggleAutoClicker.setBackgroundResource(R.drawable.bg_autoclicker_off);
            if (autoclickerStatusDot != null) autoclickerStatusDot.setBackgroundResource(R.drawable.dot_autoclicker_off);
            if (tvAutoClickerLabel != null) tvAutoClickerLabel.setTextColor(0xFF666666);
            if (tvAutoClickerState != null) {
                tvAutoClickerState.setText("OFF");
                tvAutoClickerState.setTextColor(0xFF888888);
            }
        }
    }

    /**
     * Inicializa los botones de habilidades y sus listeners
     */
    private void initializeSkills() {
        // Desbloquear habilidades basándose en el nivel del personaje
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            int characterLevel = mainActivity.getGameDataManager().getCharacterLevel();
            com.example.kaisenclicker.persistence.save.GameDataManager gdm = mainActivity.getGameDataManager();
            int charId = com.example.kaisenclicker.persistence.save.GameDataManager.CHAR_ID_RYOMEN_SUKUNA;

            // Niveles de desbloqueo actualizados
            if (characterLevel >= 5) {
                // Unlock in-memory and persist
                skillManager.unlockSkill("cleave");
                try { gdm.saveSkillLevel("cleave", charId, Math.max(1, gdm.getSkillLevel("cleave", charId))); gdm.setSkillUnlocked("cleave", true, charId); } catch (Exception ignored) {}
            }
            if (characterLevel >= 15) {
                skillManager.unlockSkill("dismantle");
                try { gdm.saveSkillLevel("dismantle", charId, Math.max(1, gdm.getSkillLevel("dismantle", charId))); gdm.setSkillUnlocked("dismantle", true, charId); } catch (Exception ignored) {}
            }
            if (characterLevel >= 30) {
                skillManager.unlockSkill("fuga");
                try { gdm.saveSkillLevel("fuga", charId, Math.max(1, gdm.getSkillLevel("fuga", charId))); gdm.setSkillUnlocked("fuga", true, charId); } catch (Exception ignored) {}
            }
            if (characterLevel >= 80) {
                skillManager.unlockSkill("domain");
                try { gdm.saveSkillLevel("domain", charId, Math.max(1, gdm.getSkillLevel("domain", charId))); gdm.setSkillUnlocked("domain", true, charId); } catch (Exception ignored) {}
            }
        }
        updateSkillButtons();
    }

    /**
     * Actualiza la visibilidad y estado de los botones de habilidades
     */
    private void updateSkillButtons() {
        int characterLevel = 0;
        int selectedCharId = GameDataManager.CHAR_ID_RYOMEN_SUKUNA;
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            characterLevel = ((MainActivity) getActivity()).getGameDataManager().getCharacterLevel();
            try { selectedCharId = ((MainActivity) getActivity()).getGameDataManager().getSelectedCharacterId(); } catch (Exception ignored) {}
        }
        boolean isGojo = (selectedCharId == GameDataManager.CHAR_ID_SATORU_GOJO);

        // Descripciones según personaje
        String skill1Desc = isGojo
                ? getString(R.string.skill_amplificacion_azul_name) + ": " + getString(R.string.skill_amplificacion_azul_desc)
                : "Cleave: Daño base + sangrado. Causa daño adicional por nivel.";
        String skill2Desc = isGojo
                ? getString(R.string.skill_ritual_inverso_rojo_name) + ": " + getString(R.string.skill_ritual_inverso_rojo_desc)
                : "Dismantle: Daño aumentado y reduce defensa del enemigo.";
        String skill3Desc = isGojo
                ? getString(R.string.skill_vacio_purpura_name) + ": " + getString(R.string.skill_vacio_purpura_desc)
                : "Fuga: Evasión y contraataque. Daño y esquiva.";
        String skill1LockedDesc = isGojo ? "Desbloquea Amplificación Azul al nivel 5" : "Desbloquea Cleave al nivel 5";
        String skill2LockedDesc = isGojo ? "Desbloquea Ritual Inverso Rojo al nivel 15" : "Desbloquea Dismantle al nivel 15";
        String skill3LockedDesc = isGojo ? "Desbloquea Vacío Púrpura al nivel 30" : "Desbloquea Fuga al nivel 30";
        // Cleave
        Skill cleave = skillManager.getSkillById("cleave");
        if (btnSkill1 != null) {
            int iconForSkill1 = (selectedCharId == GameDataManager.CHAR_ID_SATORU_GOJO) ? R.drawable.blue_skill : R.drawable.cleave_image;
            if (cleave != null && cleave.isUnlocked()) {
                // If the skill is unlocked in the manager (persisted), prefer that state
                btnSkill1.setVisibility(View.VISIBLE);
                btnSkill1.setEnabled(cleave.canUse());
                btnSkill1.setSkillIcon(iconForSkill1);
                btnSkill1.setSkillDescription(skill1Desc);
                btnSkill1.setAlpha(1f);
                if (!cleave.canUse()) btnSkill1.startCooldown(cleave.getRemainingCooldown());
            } else if (characterLevel < 5) {
                btnSkill1.setVisibility(View.VISIBLE);
                btnSkill1.setEnabled(false);
                btnSkill1.setSkillIcon(iconForSkill1);
                btnSkill1.setSkillDescription(skill1LockedDesc);
                btnSkill1.setAlpha(0.4f);
            } else {
                // Not unlocked in manager and player has sufficient level -> show but disabled until unlocked in inventory/shops
                btnSkill1.setVisibility(View.VISIBLE);
                btnSkill1.setEnabled(false);
                btnSkill1.setSkillIcon(iconForSkill1);
                btnSkill1.setSkillDescription("Disponible para desbloquear");
                btnSkill1.setAlpha(0.6f);
            }
        }
        // Dismantle
        Skill dismantle = skillManager.getSkillById("dismantle");
        if (btnSkill2 != null) {
            int iconForSkill2 = (selectedCharId == GameDataManager.CHAR_ID_SATORU_GOJO) ? R.drawable.red_skill : R.drawable.dismanteal;
            if (dismantle != null && dismantle.isUnlocked()) {
                btnSkill2.setVisibility(View.VISIBLE);
                btnSkill2.setEnabled(dismantle.canUse());
                btnSkill2.setSkillIcon(iconForSkill2);
                btnSkill2.setSkillDescription(skill2Desc);
                btnSkill2.setAlpha(1f);
                if (!dismantle.canUse()) { btnSkill2.startCooldown(dismantle.getRemainingCooldown()); }
            } else if (characterLevel < 15) {
                btnSkill2.setVisibility(View.VISIBLE);
                btnSkill2.setEnabled(false);
                btnSkill2.setSkillIcon(iconForSkill2);
                btnSkill2.setSkillDescription(skill2LockedDesc);
                btnSkill2.setAlpha(0.4f);
            } else {
                btnSkill2.setVisibility(View.VISIBLE);
                btnSkill2.setEnabled(false);
                btnSkill2.setSkillIcon(iconForSkill2);
                btnSkill2.setSkillDescription("Disponible para desbloquear");
                btnSkill2.setAlpha(0.6f);
            }
        }
        // Fuga
        Skill fuga = skillManager.getSkillById("fuga");
        if (btnSkill3 != null) {
            int iconForSkill3 = (selectedCharId == GameDataManager.CHAR_ID_SATORU_GOJO) ? R.drawable.hollow_purple : R.drawable.fuga_image;
            int requiredFugaLevel = 30;
            boolean meetsFugaLevel = characterLevel >= requiredFugaLevel;

            // Consultar estado persistido desde GameDataManager (pref/sql) para ser más robusto
            boolean persistedUnlocked = false;
            int persistedLevel = 0;
            try {
                if (getActivity() != null && getActivity() instanceof MainActivity) {
                    GameDataManager gdmLocal = ((MainActivity) getActivity()).getGameDataManager();
                    int charIdLocal = GameDataManager.CHAR_ID_RYOMEN_SUKUNA;
                    persistedUnlocked = gdmLocal.isSkillUnlocked("fuga", charIdLocal);
                    persistedLevel = gdmLocal.getSkillLevel("fuga", charIdLocal);
                }
            } catch (Exception ignored) {}

            // If the persisted state indicates unlocked or a positive level, treat as unlocked
            boolean effectivelyUnlocked = persistedUnlocked || (persistedLevel > 0) || (fuga != null && fuga.isUnlocked()) || meetsFugaLevel;

            // Si el jugador ya usó Cleave y Dismantle (en cualquier momento previo), consideramos
            // la secuencia (usarlas) preparada y permitimos usar Fuga aunque no sea en el mismo enemigo.
            boolean sequenceReady = cleaveUsedThisFight && dismantleUsedThisFight;

            if (effectivelyUnlocked || sequenceReady) {
                boolean fugaEnabled;
                // Decide availability: if we have a Skill object, prefer its cooldown; otherwise allow if persisted/level/sequenceReady
                if (fuga != null) {
                    fugaEnabled = fuga.getRemainingCooldown() == 0;
                } else if (persistedLevel > 0 || meetsFugaLevel || sequenceReady) {
                    fugaEnabled = true;
                } else {
                    fugaEnabled = false;
                }
                btnSkill3.setVisibility(View.VISIBLE);
                btnSkill3.setEnabled(fugaEnabled);
                btnSkill3.setSkillIcon(iconForSkill3);
                btnSkill3.setAlpha(fugaEnabled ? 1f : 0.4f);
                // Mostrar una descripción que indique si la secuencia está lista
                if (sequenceReady) {
                    btnSkill3.setSkillDescription(isGojo
                            ? "Vacío Púrpura listo: Azul + Rojo usados"
                            : "Fuga listo: Cleave + Dismantle usados (usa ulti ≥50% para ejecutar)");
                } else {
                    btnSkill3.setSkillDescription(skill3Desc);
                }
                if (fuga != null && !fugaEnabled) btnSkill3.startCooldown(fuga.getRemainingCooldown());
            } else if (characterLevel < requiredFugaLevel) {
                btnSkill3.setVisibility(View.VISIBLE);
                btnSkill3.setEnabled(false);
                btnSkill3.setSkillIcon(iconForSkill3);
                btnSkill3.setSkillDescription(skill3LockedDesc);
                btnSkill3.setAlpha(0.4f);
            } else {
                btnSkill3.setVisibility(View.VISIBLE);
                btnSkill3.setEnabled(false);
                btnSkill3.setSkillIcon(iconForSkill3);
                btnSkill3.setSkillDescription("Disponible para desbloquear");
                btnSkill3.setAlpha(0.6f);
            }
        }
        // Domain
        Skill domain = skillManager.getSkillById("domain");
        if (btnSkillUltimate != null) {
            // Elegir el drawable de la ultimate basado en el personaje seleccionado
            int iconForUltimate = (selectedCharId == GameDataManager.CHAR_ID_SATORU_GOJO) ? R.drawable.gojo_domain : R.drawable.sukuna_domain;
            String resName = "<none>";
            try { resName = getResources().getResourceEntryName(iconForUltimate); } catch (Exception ignored) {}
            android.util.Log.d("CampaignFragment", "updateSkillButtons: selectedCharId=" + selectedCharId + " iconForUltimate=" + iconForUltimate + " (" + resName + ")");
            if (domain != null && domain.isUnlocked()) {
                btnSkillUltimate.setVisibility(View.VISIBLE);
                btnSkillUltimate.setEnabled(domain.canUse());
                btnSkillUltimate.setSkillIcon(iconForUltimate);
                android.util.Log.d("CampaignFragment", "updateSkillButtons: set ultimate icon resource " + iconForUltimate + " (" + resName + ")");
                if (selectedCharId == GameDataManager.CHAR_ID_SATORU_GOJO) {
                    btnSkillUltimate.setSkillDescription("Expansión de Dominio: Durante la duración del dominio tus habilidades no tendrán cooldown.");
                } else {
                    btnSkillUltimate.setSkillDescription("Domain: Expansión de Dominio. Daño masivo a todos los enemigos.");
                }
                btnSkillUltimate.setAlpha(1f);
                if (!domain.canUse()) btnSkillUltimate.startCooldown(domain.getRemainingCooldown());
            } else if (characterLevel < 80) {
                btnSkillUltimate.setVisibility(View.VISIBLE);
                btnSkillUltimate.setEnabled(false);
                btnSkillUltimate.setSkillIcon(iconForUltimate);
                android.util.Log.d("CampaignFragment", "updateSkillButtons: set ultimate icon (locked) resource " + iconForUltimate + " (" + resName + ")");
                btnSkillUltimate.setSkillDescription("Desbloquea Domain al nivel 80");
                btnSkillUltimate.setAlpha(0.4f);
            } else {
                btnSkillUltimate.setVisibility(View.VISIBLE);
                btnSkillUltimate.setEnabled(false);
                btnSkillUltimate.setSkillIcon(iconForUltimate);
                android.util.Log.d("CampaignFragment", "updateSkillButtons: set ultimate icon (available) resource " + iconForUltimate + " (" + resName + ")");
                btnSkillUltimate.setSkillDescription("Disponible para desbloquear");
                btnSkillUltimate.setAlpha(0.6f);
            }
        }
    }

    /**
     * Usa una habilidad activa contra el enemigo
     */
    private void useSkill(String skillId) {
        Skill skill = skillManager.getSkillById(skillId);
        // Determine character level and required level for this skill
        int charLevel = 0;
        int selectedCharId = GameDataManager.CHAR_ID_RYOMEN_SUKUNA;
        try {
            if (getActivity() instanceof MainActivity) {
                MainActivity ma = (MainActivity) getActivity();
                charLevel = ma.getGameDataManager().getCharacterLevel();
                try { selectedCharId = ma.getGameDataManager().getSelectedCharacterId(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        int requiredLevel;
        switch (skillId) {
            case "cleave": requiredLevel = 5; break;
            case "dismantle": requiredLevel = 15; break;
            case "fuga": requiredLevel = 30; break;
            case "domain": requiredLevel = 80; break;
            default: requiredLevel = 1; break;
        }
        boolean meetsLevel = charLevel >= requiredLevel;

        // If skill object is missing and player doesn't meet level requirement -> not found
        if (skill == null && !meetsLevel) {
            android.util.Log.w("CampaignFragment", "useSkill: skill==null and level insufficient for id=" + skillId);
            return;
        }

        // Treat as unlocked if either the skill object is unlocked OR the character meets the level requirement
        boolean unlocked = (skill != null && skill.isUnlocked()) || meetsLevel;
        int skillLevel = (skill != null) ? skill.getLevel() : 1; // fallback level 1 when created by level
        boolean canUse = (skill == null) ? true : skill.canUse();
        // If Gojo's Domain is active and the selected character is Gojo, allow use regardless of cooldown
        if (selectedCharId == GameDataManager.CHAR_ID_SATORU_GOJO && gojoDomainActive) {
            canUse = true;
        }

        // Debug / info: si la habilidad no existe o no puede usarse, mostrar por qué
        if (enemyCurrentHp <= 0) {
            return;
        }
        if (!unlocked) {
            android.util.Log.i("CampaignFragment", "useSkill: skill locked id=" + skillId + " level=" + skillLevel);
            return;
        }
        if (skillLevel <= 0) {
            android.util.Log.i("CampaignFragment", "useSkill: skill level 0 id=" + skillId);
            return;
        }
        if (!canUse) {
            long rem = (skill != null) ? skill.getRemainingCooldown() : 0;
            android.util.Log.i("CampaignFragment", "useSkill: on cooldown id=" + skillId + " remaining=" + rem);
            return;
        }

        // Requisitos para Fuga
        if ("fuga".equals(skillId)) {
            // If Gojo's Domain is active, allow Purpura without consuming ulti or requiring ulti
            if (selectedCharId == GameDataManager.CHAR_ID_SATORU_GOJO && gojoDomainActive) {
                // allow usage without checking ultimateCharge and without consuming it
            } else {
                // Always require that Cleave and Dismantle were used at least once since the last usage/spawn
                if (!cleaveUsedThisFight || !dismantleUsedThisFight) {
                    return;
                }
                // For non-Gojo or outside domain, require 50% of ulti
                int requiredUltiForFuga = (int) Math.ceil(ULTIMATE_CHARGE_MAX * 0.50);
                if (ultimateCharge < requiredUltiForFuga) {
                    return;
                }
                // Consume 50% of the ulti
                setUltimateCharge(ultimateCharge - requiredUltiForFuga);
                // After using Fuga, require Cleave+Dismantle again
                cleaveUsedThisFight = false;
                dismantleUsedThisFight = false;
            }
        }

        // Calcular daño de la habilidad
        int skillDamage = skillManager.calculateSkillDamage(skillId, damagePerTap);

        // Aplicar efecto especial según habilidad
        String effectMessage = "";
        switch (skillId) {
            case "cleave":
                // Aplicar daño inmediato reforzado por Cleave (escala con el nivel de la skill)
                cleaveUsedThisFight = true;
                // Mostrar efecto visual de Cleave (solo para Sukuna)
                if (selectedCharId != GameDataManager.CHAR_ID_SATORU_GOJO) {
                    try { showCleaveEffect(); } catch (Exception ignored) {}
                }

                try {
                    Skill cleaveSkill = skillManager.getSkillById("cleave");
                    int cleaveLv = (cleaveSkill != null) ? Math.max(1, cleaveSkill.getLevel()) : 1;
                    // Damage base calculado por SkillManager
                    int baseCleave = skillManager.calculateSkillDamage("cleave", damagePerTap);
                    // Multiplicador: 1 + 0.5 * level (50% extra por nivel)
                    double multiplier = 1.0 + 0.5 * (double) cleaveLv;
                    long damageLong = Math.round(baseCleave * multiplier);
                    // Limitar a un tope razonable (no más de 10x HP o Integer.MAX_VALUE)
                    long maxAllowed = Math.min((long) Integer.MAX_VALUE, (long) enemyMaxHp * 10L);
                    int cleaveDamage = (int) Math.max(1L, Math.min(damageLong, maxAllowed));

                    android.util.Log.d("CampaignFragment", "Cleave damage=" + cleaveDamage + " base=" + baseCleave + " lv=" + cleaveLv + " enemyMaxHp=" + enemyMaxHp);
                    // Aplicar daño inmediato
                    applyDamageToEnemy(cleaveDamage, true);

                    // Si la skill tiene parámetros de sangrado, conservar el DoT original
                    if (cleaveSkill != null && cleaveSkill.hasBleed()) {
                        // Si Choso está en segunda fase, es inmune a sangrados
                        if (isBossEnemy && BOSS_CHOSO.equals(currentEnemyId) && chosoSecondPhaseActive) {
                            // Choso segunda fase: inmune al sangrado
                        } else {
                            final int bleedDuration = cleaveSkill.getBleedDurationMs();
                            final int bleedTick = cleaveSkill.getBleedTickMs();
                            final int ticks = Math.max(1, bleedDuration / Math.max(1, bleedTick));
                            final int bleedPerTick = cleaveSkill.computeBleedPerTick(damagePerTap);
                            effectMessage = "🩸 Cleave! Sangrado aplicado!";
                            for (int i = 1; i <= ticks; i++) {
                                final int delay = i * bleedTick;
                                final int dmgTick = bleedPerTick;
                                final boolean isPersistent = true;
                                Runnable r = new Runnable() {
                                    @Override public void run() {
                                        try {
                                            long now = System.currentTimeMillis();
                                            long sinceSpawn = now - lastSpawnTimestampMs;
                                            if (isPersistent && sinceSpawn < PERSISTENT_BLEED_GRACE_MS) {
                                                // Retrasar hasta que termine la ventana de gracia
                                                mainHandler.postDelayed(this, PERSISTENT_BLEED_GRACE_MS - sinceSpawn);
                                                return;
                                            }
                                        } catch (Exception ignored) {}
                                        // Bleed from Cleave should persist across enemies until its duration finishes
                                        applyDamageToEnemy(dmgTick, true);
                                    }
                                };
                                // marcar persistente para que siga al siguiente enemigo
                                activeBleedRunnables.add(new BleedTask(r, true));
                                mainHandler.postDelayed(r, delay);
                            }
                        }
                     } else {
                         effectMessage = "🩸 Cleave!";
                     }
                } catch (Exception e) {
                    // Fallback: aplicar daño base si hay fallo
                    effectMessage = "🩸 Cleave!";
                    applyDamageToEnemy(skillManager.calculateSkillDamage("cleave", damagePerTap), true);
                }
                break;
            case "dismantle":
                // Dismantle: daño porcentual de la vida actual (más nivel -> mayor %)
                // Mostrar efecto visual de Dismantle (solo para Sukuna)
                if (selectedCharId != GameDataManager.CHAR_ID_SATORU_GOJO) {
                    try { showDismantleEffect(); } catch (Exception ignored) {}
                }
                try {
                    int enemyRes2 = 0; // placeholder
                    int dismantleDamageActual = skillManager.calculateDismantleDamage(enemyCurrentHp, enemyRes2, enemyLevel, skillId);
                    skillDamage = dismantleDamageActual;
                    dismantleUsedThisFight = true;
                    effectMessage = "⚡ Dismantle! Corte porcentual aplicado!";
                    applyDamageToEnemy(skillDamage, true);
                } catch (Exception e) {
                    // fallback
                    effectMessage = "⚡ Dismantle!";
                    applyDamageToEnemy(skillDamage, true);
                }
                break;
            case "fuga":
                // Fuga (Sukuna) / Hollow Purple (Gojo): mostrar GIF a pantalla completa antes del daño
                try {
                    // Elegir GIF según personaje: Gojo = hollow_purple_animation.gif, Sukuna = fuga_image
                    int gifRes = (selectedCharId == GameDataManager.CHAR_ID_SATORU_GOJO)
                            ? R.drawable.hollow_purple_animation : R.drawable.fuga_animation;
                    int gifDuration = 1500; // 1.5 segundos de animación

                    // Calcular daño antes de la animación
                    final Skill fugaSkill = skillManager.getSkillById("fuga");
                    final int skillLv = (fugaSkill != null) ? Math.max(1, fugaSkill.getLevel()) : 1;
                    int baseBurst = skillManager.calculateSkillDamage("fuga", damagePerTap);
                    long burstCalc = (long) baseBurst * (2 + (long) skillLv);
                    int minByHp = Math.max(1, enemyMaxHp / 4);
                    long maxAllowed = Math.min((long) Integer.MAX_VALUE, (long) enemyMaxHp * 10L);
                    long burstLong = Math.max((long) minByHp, Math.min(burstCalc, maxAllowed));
                    final int burst = (int) burstLong;

                    effectMessage = "💨 Fuga! GOLPE DEVASTADOR!";
                    android.util.Log.d("CampaignFragment", "Fuga burst=" + burst + " enemyMaxHp=" + enemyMaxHp + " skillLv=" + skillLv);

                    // Mostrar GIF a pantalla completa, luego aplicar daño al terminar
                    showSkillGifAnimation(gifRes, gifDuration, () -> {
                        // Aplicar el daño inmediato grande
                        applyDamageToEnemy(burst, true);

                        // Aplicar quemadura como DoT
                        if (fugaSkill != null) {
                            int baseDurationMs = 4000;
                            int extraPerLevelMs = 1000;
                            int durationMs = baseDurationMs + (extraPerLevelMs * Math.max(0, fugaSkill.getLevel() - 1));
                            int tickMs = 1000;
                            int ticks = Math.max(1, durationMs / tickMs);
                            int burnPerTick = Math.max(1, Math.round(damagePerTap * (0.35f + 0.12f * fugaSkill.getLevel())));

                            for (int i = 1; i <= ticks; i++) {
                                final int delay = i * tickMs;
                                final int dmgTick = burnPerTick;
                                Runnable r = () -> {
                                    // Burn from Fuga: no persistir entre enemigos (por defecto)
                                    if (enemyCurrentHp <= 0) return;
                                    applyDamageToEnemy(dmgTick, true);
                                };
                                activeBleedRunnables.add(new BleedTask(r, false));
                                mainHandler.postDelayed(r, delay);
                            }
                        }
                    });
                } catch (Exception e) {
                    effectMessage = "💨 Fuga!";
                    applyDamageToEnemy(skillDamage, true);
                }
                break;
            case "domain":
                // If selected character is Gojo, Domain grants 5s of no-cooldown and free Purpura usage
                try {
                    if (selectedCharId == GameDataManager.CHAR_ID_SATORU_GOJO) {
                        final int domainDurationMs = 5000;
                        effectMessage = "✨ Expansión de Dominio activada: habilidades sin cooldown";
                        // Mostrar GIF de dominio de Gojo a pantalla completa
                        showSkillGifAnimation(R.drawable.gojo_domain_animation, 2000, () -> {
                            // Activate domain state after GIF
                            gojoDomainActive = true;
                            try { updateSkillButtons(); } catch (Exception ignored) {}
                            try { if (getView() != null) getView().setBackgroundResource(R.drawable.gojo_domain_background); } catch (Exception ignored) {}
                            if (gojoDomainEndRunnable != null) {
                                mainHandler.removeCallbacks(gojoDomainEndRunnable);
                            }
                            gojoDomainEndRunnable = () -> {
                                try {
                                    gojoDomainActive = false;
                                    if (getView() != null) {
                                        try { getView().setBackground(originalBackgroundDrawable); } catch (Exception ignored) {}
                                    }
                                    updateSkillButtons();
                                } catch (Exception ignored) {}
                            };
                            mainHandler.postDelayed(gojoDomainEndRunnable, domainDurationMs);
                        });
                        break;
                    }
                    // Non-Gojo (Sukuna) domain: ráfaga devastadora de Cleaves y Dismantles durante 5s
                    final int domainDurationMs = 5000;
                    final int strikeIntervalMs = 400; // un golpe cada 400ms
                    final int totalStrikes = domainDurationMs / strikeIntervalMs; // ~12 golpes

                    // Obtener niveles de habilidades para escalar daño
                    Skill cleaveSkillDom = skillManager.getSkillById("cleave");
                    Skill dismantleSkillDom = skillManager.getSkillById("dismantle");
                    final int cleaveLvDom = (cleaveSkillDom != null) ? Math.max(1, cleaveSkillDom.getLevel()) : 1;
                    final int baseCleaveForDomain = skillManager.calculateSkillDamage("cleave", damagePerTap);
                    final double cleaveMultDom = 1.0 + 0.5 * (double) cleaveLvDom;
                    final int cleaveDmgDom = (int) Math.max(1L, Math.min(Math.round(baseCleaveForDomain * cleaveMultDom), (long) enemyMaxHp * 10L));
                    final int dismantleDmgDom = skillManager.calculateDismantleDamage(enemyCurrentHp, 0, enemyLevel, "dismantle");

                    // Sangrado continuo durante todo el dominio (pasiva de Cleave)
                    final int bleedPerTickDom = (cleaveSkillDom != null) ? cleaveSkillDom.computeBleedPerTick(damagePerTap) : Math.max(1, damagePerTap / 4);
                    final int bleedTickMsDom = 500;
                    final int bleedTicksDom = domainDurationMs / bleedTickMsDom;

                    effectMessage = "🌀 ¡Malevolent Shrine activado!";

                    // Mostrar GIF de dominio de Sukuna, luego desatar la ráfaga
                    showSkillGifAnimation(R.drawable.sukuna_domain_animation, 2000, () -> {
                        // Activar estado de dominio de Sukuna (persiste entre enemigos)
                        sukunaDomainActive = true;
                        domainRunnables.clear();

                        // Cambiar fondo a Shrine
                        if (getView() != null) {
                            try { getView().setBackgroundResource(R.drawable.shrine_background); } catch (Exception ignored) {}
                        }

                        // Ráfaga de golpes alternando Cleave y Dismantle
                        for (int i = 0; i < totalStrikes; i++) {
                            final int delay = i * strikeIntervalMs;
                            final boolean isCleaveStrike = (i % 2 == 0);
                            final int strikeDmg = isCleaveStrike ? cleaveDmgDom : dismantleDmgDom;

                            Runnable strikeRunnable = new Runnable() {
                                @Override public void run() {
                                    try {
                                        long now = System.currentTimeMillis();
                                        long sinceSpawn = now - lastSpawnTimestampMs;
                                        if (sinceSpawn < PERSISTENT_BLEED_GRACE_MS) {
                                            mainHandler.postDelayed(this, PERSISTENT_BLEED_GRACE_MS - sinceSpawn);
                                            return;
                                        }
                                    } catch (Exception ignored) {}
                                    // NO comprobar enemyCurrentHp <= 0: el daño se aplica al enemigo actual sea quien sea
                                    applyDamageToEnemy(strikeDmg, true);
                                    // Mostrar efecto visual siempre
                                    try {
                                        if (isCleaveStrike) {
                                            showCleaveEffect();
                                        } else {
                                            showDismantleEffect();
                                        }
                                    } catch (Exception ignored) {}
                                }
                            };
                            domainRunnables.add(strikeRunnable);
                            activeBleedRunnables.add(new BleedTask(strikeRunnable, true));
                            mainHandler.postDelayed(strikeRunnable, delay);
                        }

                        // Sangrado continuo durante todo el dominio (pasiva de Cleave)
                        for (int i = 1; i <= bleedTicksDom; i++) {
                            final int bleedDelay = i * bleedTickMsDom;
                            Runnable bleedR = new Runnable() {
                                @Override public void run() {
                                    try {
                                        long now = System.currentTimeMillis();
                                        long sinceSpawn = now - lastSpawnTimestampMs;
                                        if (sinceSpawn < PERSISTENT_BLEED_GRACE_MS) {
                                            mainHandler.postDelayed(this, PERSISTENT_BLEED_GRACE_MS - sinceSpawn);
                                            return;
                                        }
                                    } catch (Exception ignored) {}
                                    // No comprobar muerte: el sangrado sigue al siguiente enemigo
                                    applyDamageToEnemy(bleedPerTickDom, true);
                                }
                            };
                            domainRunnables.add(bleedR);
                            // domain runnables deben persistir mientras el dominio esté activo
                            activeBleedRunnables.add(new BleedTask(bleedR, true));
                             mainHandler.postDelayed(bleedR, bleedDelay);
                        }

                        // Programar fin del dominio: restaurar fondo y limpiar estado
                        Runnable domainEndRunnable = () -> {
                            sukunaDomainActive = false;
                            domainRunnables.clear();
                            try {
                                if (getView() != null) getView().setBackground(originalBackgroundDrawable);
                            } catch (Exception ignored) {}
                        };
                        mainHandler.postDelayed(domainEndRunnable, domainDurationMs + 200);
                    });
                } catch (Exception e) {
                    effectMessage = "🌀 Domain!";
                    applyDamageToEnemy(skillDamage, true);
                }
                break;
        }

        // Marcar habilidad como usada
        try {
            // If Gojo domain is active and the selected character is Gojo, do not register skill use nor start cooldowns so abilities feel free
            if (!(selectedCharId == GameDataManager.CHAR_ID_SATORU_GOJO && gojoDomainActive)) {
                if (skill != null) skillManager.useSkill(skillId);
            }
        } catch (Exception ignored) {}

         // Iniciar cooldown visual en la SkillButtonView correspondiente
         Skill s = skillManager.getSkillById(skillId);
         if (s != null) {
             int cd = s.getEffectiveCooldownMs();
             // Don't start visual cooldowns during Gojo domain
             if (!(selectedCharId == GameDataManager.CHAR_ID_SATORU_GOJO && gojoDomainActive)) {
                 switch (skillId) {
                     case "cleave": if (btnSkill1 != null) btnSkill1.startCooldown(cd); break;
                     case "dismantle": if (btnSkill2 != null) btnSkill2.startCooldown(cd); break;
                     case "fuga": if (btnSkill3 != null) btnSkill3.startCooldown(cd); break;
                     case "domain": if (btnSkillUltimate != null) btnSkillUltimate.startCooldown(cd); break;
                 }
             }
         }

         // Actualizar botones con nuevos cooldowns
         updateSkillButtons();

        // NO llamar a onEnemyDefeated() aquí: applyDamageToEnemy() ya lo hace
        // si el enemigo muere. Llamarlo dos veces causaba que se saltara un nivel.

        // Actualizar cada 100ms para mostrar cooldown en tiempo real
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            this::updateSkillButtons,
            100
        );
    }

    private void spawnNewEnemy() {
        // Cancelar cualquier DoT pendiente del enemigo anterior (pero NO los del dominio de Sukuna)
        if (!activeBleedRunnables.isEmpty()) {
            java.util.Iterator<BleedTask> it = activeBleedRunnables.iterator();
            java.util.List<Runnable> toRetain = new java.util.ArrayList<>();
            while (it.hasNext()) {
                BleedTask bt = it.next();
                // Si es persistente, lo mantenemos
                if (bt.persistent) {
                    toRetain.add(bt.r);
                    continue;
                }
                // Si pertenece al dominio activo, mantenerlo
                if (sukunaDomainActive && domainRunnables.contains(bt.r)) {
                    toRetain.add(bt.r);
                    continue;
                }
                // Cancelar los no persistentes
                try { mainHandler.removeCallbacks(bt.r); } catch (Exception ignored) {}
                it.remove();
            }
            // Si había dominio activo, asegurar que los domainRunnables sigan presentes
            if (sukunaDomainActive) {
                // reconstruir la lista con las runnables a retener
                java.util.List<BleedTask> newList = new java.util.ArrayList<>();
                for (BleedTask bt : activeBleedRunnables) {
                    if (bt.persistent || domainRunnables.contains(bt.r)) newList.add(bt);
                }
                activeBleedRunnables.clear();
                activeBleedRunnables.addAll(newList);
            }
        }

        // Registrar timestamp de spawn para que DoTs persistentes respeten una breve gracia
        lastSpawnTimestampMs = System.currentTimeMillis();
        // Detener la adaptación de Mahoraga si estaba activa del enemigo anterior
        try { stopMahoragaAdaptation(); } catch (Exception ignored) {}

        // NOTA: no resetear aquí los flags de Cleave/Dismantle para permitir que la
        // secuencia (usarlas) se guarde entre enemigos. Solo se resetearán al usar
        // "fuga" (o si quieres forzar un reset explícito en otro lugar).

        // Calcular HP máximo y si es boss (esto debe ir antes de intentar restaurar progreso guardado)
        enemyMaxHp = calculateEnemyMaxHp(enemyLevel);
        isBossEnemy = (enemyLevel % 10 == 0);
        // Por defecto asumimos vida llena; luego intentaremos restaurar progreso guardado
        enemyCurrentHp = enemyMaxHp;

        // Intentar restaurar progreso del enemigo desde GameDataManager si coincide el nivel y el tipo (boss/normal)
        boolean restoredEnemyProgress = false;
        try {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                com.example.kaisenclicker.persistence.save.GameDataManager gdm = mainActivity.getGameDataManager();
                if (gdm != null) {
                    int savedHp = gdm.getSavedEnemyCurrentHp();
                    int savedArmor = gdm.getSavedEnemyArmor();
                    int savedLevel = gdm.getCurrentEnemyLevelSql();
                    boolean savedIsBoss = gdm.isCurrentEnemyBoss();
                    String savedEnemyId = gdm.getCurrentEnemyId();
                    boolean savedChosoPhase = gdm.isChosoSecondPhaseSaved();
                    boolean savedMahitoTrans = gdm.isMahitoTransformedSaved();

                    // Debug logs for restoration
                    android.util.Log.d("CampaignFragment", "restore check: savedHp=" + savedHp + " savedArmor=" + savedArmor + " savedLevel=" + savedLevel + " savedIsBoss=" + savedIsBoss + " savedEnemyId=" + savedEnemyId + " savedChosoPhase=" + savedChosoPhase + " savedMahitoTrans=" + savedMahitoTrans);

                    // Restaurar solo si existe progreso guardado y el nivel coincide
                    // Only restore when there is a positive saved HP (avoid restoring dead/enemy with 0 HP)
                    if (savedHp > 0 && savedLevel == enemyLevel && savedIsBoss == isBossEnemy) {
                        enemyCurrentHp = Math.max(0, Math.min(enemyMaxHp, savedHp));
                        currentArmorValue = Math.max(0, savedArmor);
                        chosoSecondPhaseActive = savedChosoPhase;
                        mahitoTransformed = savedMahitoTrans;
                        // Restaurar también el id del enemigo guardado para usarlo al elegir drawable y aplicar fases
                        if (savedEnemyId != null) currentEnemyId = savedEnemyId;
                        restoredEnemyProgress = true;
                        android.util.Log.d("CampaignFragment", "restoredEnemyProgress=true: enemyCurrentHp=" + enemyCurrentHp + " currentArmorValue=" + currentArmorValue + " currentEnemyId=" + currentEnemyId + " chosoSecondPhaseActive=" + chosoSecondPhaseActive + " mahitoTransformed=" + mahitoTransformed);
                    } else {
                        // Si había guardado un progreso con HP <= 0 para el mismo nivel/tipo, limpiarlo (evita quedar atascado)
                        if (savedHp <= 0 && savedLevel == enemyLevel && savedIsBoss == isBossEnemy) {
                            try {
                                android.util.Log.i("CampaignFragment", "found saved dead enemy (hp<=0) for same level-> clearing saved progress to avoid stuck spawn");
                                mainActivity.getGameDataManager().clearSavedEnemyProgress();
                                return; // salir para evitar sobreescritura accidental
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // Reiniciar la barra de vida con el nuevo nivel
        if (hpBar != null) {
            hpBar.setMaxHealth(enemyMaxHp);
            hpBar.setHealth(enemyCurrentHp);
            hpBar.setEnemyLevel(enemyLevel);
            // Mostrar el nombre correcto: 'Mahito' si es boss, 'Yusepe' si no
            hpBar.setEnemyName(isBossEnemy ? "Mahito" : "Yusepe");
        }

        // Si el enemigo restaurado estaba en segunda fase y es Choso, ajustar la imagen/estado visual
        try {
            // usar currentEnemyId restaurado (si lo hay) para comprobar si era Choso
            if (isBossEnemy && chosoSecondPhaseActive && (BOSS_CHOSO.equals(currentEnemyId) || BOSS_CHOSO_SECOND.equals(currentEnemyId))) {
                if (hpBar != null) hpBar.setArmorMaxAndCurrent(currentArmorValue, currentArmorValue);
                if (ivEnemyYusepe != null) {
                    ivEnemyYusepe.setImageResource(R.drawable.choso_boss_second_phase);
                    try { ivEnemyYusepe.setScaleType(ImageView.ScaleType.CENTER_INSIDE); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        // Mostrar el icono de boss junto al nombre usando el componente de HP
        try {
            if (hpBar != null) {
                hpBar.setBossVisible(isBossEnemy);
            }
        } catch (Exception ignored) {}

        // Si es boss, mostrar la imagen 'mahito' en el ImageView principal y actualizar el drawable original
        if (ivEnemyYusepe != null) {
            // Resetear escala y posición por si quedó agrandado o trasladado de la transformación previa
            try { ivEnemyYusepe.setScaleX(1f); ivEnemyYusepe.setScaleY(1f); ivEnemyYusepe.setTranslationY(0f); } catch (Exception ignored) {}
            if (isBossEnemy) {
                // Elegir el boss a spawnear: si restauramos progreso, usar el id restaurado; si no, elegir aleatoriamente entre los bosses disponibles
                String bossToSpawn;
                if (restoredEnemyProgress && currentEnemyId != null) {
                    bossToSpawn = currentEnemyId;
                } else {
                    // Random entre Choso, Mahito y Mahoraga para los bosses de nivel múltiplo de 10
                    try {
                        int pick = (random != null) ? random.nextInt(3) : 0; // 0,1,2
                        switch (pick) {
                            case 0: bossToSpawn = BOSS_CHOSO; break;
                            case 1: bossToSpawn = BOSS_MAHITO; break;
                            default: bossToSpawn = BOSS_MAHORAGA; break;
                        }
                    } catch (Exception ignored) {
                        bossToSpawn = BOSS_CHOSO;
                    }
                }
                int bossRes = getBossDrawableRes(bossToSpawn);
                try {
                    if (bossRes != 0) {
                        ivEnemyYusepe.setImageResource(bossRes);
                        // Ajustes visuales más discretos para que Choso no ocupe tanto espacio
                        try {
                            ivEnemyYusepe.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            // Escala reducida para que Choso no ocupe tanto espacio
                            ivEnemyYusepe.setScaleX(0.8f);
                            ivEnemyYusepe.setScaleY(0.8f);
                            ivEnemyYusepe.setTranslationY(-dpToPx(2));
                            // Guardar el drawable y el scaleType como estado original para revertir correctamente
                            ivEnemyOriginalRes = bossRes;
                            originalEnemyScaleTypeOrdinal = android.widget.ImageView.ScaleType.CENTER_INSIDE.ordinal();
                        } catch (Exception ignored) {
                            ivEnemyOriginalRes = bossRes;
                        }
                    } else {
                        ivEnemyYusepe.setImageResource(ivEnemyOriginalRes);
                    }
                } catch (Exception ignored) {
                    ivEnemyYusepe.setImageResource(ivEnemyOriginalRes);
                }
                // Ajustar nombre en la barra
                try { if (hpBar != null) hpBar.setEnemyName(getBossDisplayName(bossToSpawn)); } catch (Exception ignored) {}
                // Guardar id actual
                currentEnemyId = bossToSpawn;
                // Si el boss seleccionado es Mahoraga, iniciar su adaptación periódica
                if (BOSS_MAHORAGA.equals(bossToSpawn)) {
                    startMahoragaAdaptation();
                }
            } else {
                ivEnemyYusepe.setImageResource(ivEnemyOriginalRes);
            }
        }

        if (isBossEnemy) {
            // Establecer armadura: base 50% del HP, y aumentar un poco por nivel para hacerlo más resistente
            // Fórmula: armorPercent = 0.5 + min(0.5, enemyLevel * 0.02) -> entre 50% y 100% del HP
            try {
                if (hpBar != null) {
                    if (!restoredEnemyProgress) {
                        // No hay progreso restaurado: inicializar armadura a valores por defecto
                        float extra = Math.min(0.5f, enemyLevel * 0.02f); // +2% por nivel, tope +50%
                        float armorPercent = 0.5f + extra;
                        int armorValue = Math.max(1, (int) (enemyMaxHp * armorPercent));
                        hpBar.setArmorMaxAndCurrent(armorValue, armorValue);
                        // Sincronizar la variable de control con el componente inmediatamente
                        try { currentArmorValue = hpBar.getArmorCurrent(); } catch (Exception ignored) { currentArmorValue = armorValue; }
                        // Forzar visibilidad y log
                        try { hpBar.setArmorVisible(currentArmorValue > 0); } catch (Exception ignored) {}
                        android.util.Log.d("CampaignFragment", "boss spawn init armorValue=" + armorValue + " currentArmorValue=" + currentArmorValue);
                        // Resetear flags de transformaciones sólo si no restauramos progreso
                        mahitoTransformed = false;
                        chosoSecondPhaseActive = false;
                    } else {
                        // Progreso restaurado: conservar armor y flags previos, y aplicar al hpBar
                        hpBar.setArmorMaxAndCurrent(currentArmorValue, currentArmorValue);
                        try { hpBar.setArmorVisible(currentArmorValue > 0); } catch (Exception ignored) {}
                        android.util.Log.d("CampaignFragment", "boss spawn restored armor currentArmorValue=" + currentArmorValue + " currentEnemyId=" + currentEnemyId + " chosoPhase=" + chosoSecondPhaseActive);
                        try { currentArmorValue = hpBar.getArmorCurrent(); } catch (Exception ignored) {}
                        // mahitoTransformed and chosoSecondPhaseActive were restored earlier
                    }
                }
            } catch (Exception ignored) {}
        } else {
            // Restaurar icono oculto por defecto
            try { if (hpBar != null) hpBar.setBossVisible(false); } catch (Exception ignored) {}
        }

        // Si es boss, mostrar la imagen 'mahito' en el ImageView principal y actualizar el drawable original
        if (ivEnemyYusepe != null) {
            // Resetear escala y posición por si quedó agrandado o trasladado de la transformación previa
            try { ivEnemyYusepe.setScaleX(1f); ivEnemyYusepe.setScaleY(1f); ivEnemyYusepe.setTranslationY(0f); } catch (Exception ignored) {}
            if (isBossEnemy) {
                // Elegir el boss a spawnear: si restauramos progreso, usar el id restaurado; si no, elegir aleatoriamente entre los bosses disponibles
                String bossToSpawn;
                if (restoredEnemyProgress && currentEnemyId != null) {
                    bossToSpawn = currentEnemyId;
                } else {
                    // Random entre Choso, Mahito y Mahoraga para los bosses de nivel múltiplo de 10
                    try {
                        int pick = (random != null) ? random.nextInt(3) : 0; // 0,1,2
                        switch (pick) {
                            case 0: bossToSpawn = BOSS_CHOSO; break;
                            case 1: bossToSpawn = BOSS_MAHITO; break;
                            default: bossToSpawn = BOSS_MAHORAGA; break;
                        }
                    } catch (Exception ignored) {
                        bossToSpawn = BOSS_CHOSO;
                    }
                }
                int bossRes = getBossDrawableRes(bossToSpawn);
                try {
                    if (bossRes != 0) {
                        ivEnemyYusepe.setImageResource(bossRes);
                        // Ajustes visuales más discretos para que Choso no ocupe tanto espacio
                        try {
                            ivEnemyYusepe.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            // Escala reducida para que Choso no ocupe tanto espacio
                            ivEnemyYusepe.setScaleX(0.8f);
                            ivEnemyYusepe.setScaleY(0.8f);
                            ivEnemyYusepe.setTranslationY(-dpToPx(2));
                            // Guardar el drawable y el scaleType como estado original para revertir correctamente
                            ivEnemyOriginalRes = bossRes;
                            originalEnemyScaleTypeOrdinal = android.widget.ImageView.ScaleType.CENTER_INSIDE.ordinal();
                        } catch (Exception ignored) {
                            ivEnemyOriginalRes = bossRes;
                        }
                    } else {
                        ivEnemyYusepe.setImageResource(ivEnemyOriginalRes);
                    }
                } catch (Exception ignored) {
                    ivEnemyYusepe.setImageResource(ivEnemyOriginalRes);
                }
                // Ajustar nombre en la barra
                try { if (hpBar != null) hpBar.setEnemyName(getBossDisplayName(bossToSpawn)); } catch (Exception ignored) {}
                // Guardar id actual
                currentEnemyId = bossToSpawn;
                // Si el boss seleccionado es Mahoraga, iniciar su adaptación periódica
                if (BOSS_MAHORAGA.equals(bossToSpawn)) {
                    startMahoragaAdaptation();
                }
            } else {
                ivEnemyYusepe.setImageResource(ivEnemyOriginalRes);
            }
        }

        // Si es Choso, establecer estado inicial para la segunda fase
        if (isBossEnemy && BOSS_CHOSO.equals(currentEnemyId) && !restoredEnemyProgress) {
            chosoSecondPhaseActive = false;
        }

        // Persistir el estado actual del enemigo (nivel, si es boss y su id)
        try {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                String enemyId = isBossEnemy ? (currentEnemyId != null ? currentEnemyId : BOSS_CHOSO) : "yusepe";
                currentEnemyId = enemyId;
                mainActivity.getGameDataManager().saveCurrentEnemyState(enemyLevel, isBossEnemy, enemyId);
                // También persistir HP/armadura/fase actuales para que el cambio de pestañas no reinicie el enemigo
                try {
                    android.util.Log.d("CampaignFragment", "save on spawn: hp=" + enemyCurrentHp + " armor=" + currentArmorValue + " chosoPhase=" + chosoSecondPhaseActive + " mahitoTrans=" + mahitoTransformed + " enemyId=" + enemyId);
                    mainActivity.getGameDataManager().saveEnemyProgress(enemyCurrentHp, currentArmorValue, chosoSecondPhaseActive, mahitoTransformed);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // Mostrar mensaje
    }

    private void attackEnemy() {
        if (enemyCurrentHp <= 0) {
            return; // El enemigo ya está muerto
        }

        // Aplicar daño (la lógica de armadura se maneja dentro de hpBar.takeDamage)

        // Contabilizar click/tap del jugador
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            try {
                int newTotal = mainActivity.getGameDataManager().addClick();
            } catch (Exception ignored) {}
        }

        // Cargar ulti por cada tap
        addUltimateCharge(5); // Puedes ajustar la cantidad

        // Aplicar daño centralizado (actualiza hpBar y enemyCurrentHp, reproduce animación y comprueba muerte)
        applyDamageToEnemy(damagePerTap, false);
    }

    // Muestra un popup de daño (animación hacia arriba + fade)
    private void showDamagePopup(int amount) {
        if (tvDamagePopup == null) return;
        final String text = (amount <= 0) ? "-0" : ("-" + amount);
        // Asegurarnos de ejecutar en UI thread
        if (getView() != null) {
            getView().post(() -> {
                try {
                    tvDamagePopup.setText(text);
                    tvDamagePopup.setTranslationY(0f);
                    tvDamagePopup.setAlpha(1f);
                    tvDamagePopup.setVisibility(View.VISIBLE);
                    // Animación: subir y desvanecer
                    tvDamagePopup.animate()
                            .translationYBy(-140f)
                            .alpha(0f)
                            .setDuration(700)
                            .withEndAction(() -> {
                                tvDamagePopup.setVisibility(View.GONE);
                                tvDamagePopup.setAlpha(1f);
                                tvDamagePopup.setTranslationY(0f);
                            }).start();
                } catch (Exception ignored) {}
            });
        }
    }

    /**
     * Aplica daño al enemigo teniendo en cuenta armadura (gestionada por HpBarComponent).
     * Sincroniza enemyCurrentHp con la barra y dispara animaciones + comprobación de muerte.
     */
    private void applyDamageToEnemy(int amount) {
        applyDamageToEnemy(amount, false);
    }

    /**
     * Versión extendida que conoce si el daño viene de una habilidad (fromSkill=true)
     */
    private void applyDamageToEnemy(int amount, boolean fromSkill) {
         if (amount <= 0) return;

        // Aplicar reducción de daño específica de Choso en segunda fase
        int effectiveAmount = amount;
         try {
             if (isBossEnemy && BOSS_CHOSO.equals(currentEnemyId) && chosoSecondPhaseActive) {
                 effectiveAmount = Math.max(1, Math.round(amount * CHOSO_SECOND_PHASE_DAMAGE_MULTIPLIER));
             }

            // === MAHORAGA: Reducción por Adaptación (stacks de tiempo) - SOLO habilidades ===
            if (isBossEnemy && BOSS_MAHORAGA.equals(currentEnemyId) && mahoragaAdaptationStacks > 0 && fromSkill) {
                float stackReduction = MAHORAGA_ADAPT_PER_STACK * mahoragaAdaptationStacks;
                stackReduction = Math.min(stackReduction, 0.85f);
                effectiveAmount = Math.max(1, Math.round(effectiveAmount * (1.0f - stackReduction)));
            }

            // === MAHORAGA: Reducción progresiva por golpes recibidos - TODOS los ataques (taps + habilidades) ===
            if (isBossEnemy && BOSS_MAHORAGA.equals(currentEnemyId)) {
                float hitReduction = Math.min(MAHORAGA_HIT_REDUCTION_PER_HIT * mahoragaHitsReceived, MAHORAGA_HIT_REDUCTION_MAX);
                if (hitReduction > 0f) {
                    effectiveAmount = Math.max(1, Math.round(effectiveAmount * (1.0f - hitReduction)));
                }
                // Incrementar contador de golpes recibidos
                mahoragaHitsReceived++;
            }
         } catch (Exception ignored) {}

        // Si el daño proviene de una habilidad contra Mahoraga, incrementa su adaptación de stacks
        try {
            if (fromSkill && isBossEnemy && BOSS_MAHORAGA.equals(currentEnemyId)) {
                if (mahoragaAdaptationStacks < MAHORAGA_MAX_ADAPT_STACKS) {
                    mahoragaAdaptationStacks = Math.min(MAHORAGA_MAX_ADAPT_STACKS, mahoragaAdaptationStacks + 1);
                    try {
                        if (mahoragaAdaptText != null) mahoragaAdaptText.setText(String.valueOf(mahoragaAdaptationStacks));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

         // Primero proyectar el HP resultante si aplicamos este daño (toma en cuenta armadura)
         int projectedHP = enemyCurrentHp;
         try {
             if (hpBar != null) {
                 int armorNow = hpBar.getArmorCurrent();
                 int hpNow = hpBar.getCurrentHP();
                 int remaining = effectiveAmount;
                 if (armorNow > 0) {
                     int armorDamage = Math.min(armorNow, remaining);
                     remaining -= armorDamage;
                 }
                 projectedHP = Math.max(0, hpNow - remaining);
             } else {
                 projectedHP = Math.max(0, enemyCurrentHp - effectiveAmount);
             }
         } catch (Exception ignored) {}

        // PROYECCIÓN: detectar si este golpe romperá la armadura de Choso
        try {
            if (isBossEnemy && BOSS_CHOSO.equals(currentEnemyId) && hpBar != null && !chosoSecondPhaseActive) {
                int armorNow = hpBar.getArmorCurrent();
                int armorDamage = Math.min(armorNow, effectiveAmount);
                int armorAfterProjected = Math.max(0, armorNow - armorDamage);
                if (armorNow > 0 && armorAfterProjected == 0) {
                    // Activar segunda fase antes de aplicar el daño para que la imagen cambie inmediatamente
                    chosoSecondPhaseActive = true;
                    currentEnemyId = BOSS_CHOSO_SECOND;
                    try {
                        if (ivEnemyYusepe != null) {
                            ivEnemyYusepe.setImageResource(R.drawable.choso_boss_second_phase);
                            ivEnemyYusepe.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            ivEnemyYusepe.setScaleX(0.9f);
                            ivEnemyYusepe.setScaleY(0.9f);
                            ivEnemyYusepe.setTranslationY(-dpToPx(4));
                            ivEnemyOriginalRes = R.drawable.choso_boss_second_phase;
                            originalEnemyScaleTypeOrdinal = android.widget.ImageView.ScaleType.CENTER_INSIDE.ordinal();
                        }
                    } catch (Exception ignored) {}
                    // Cancelar DoTs
                    if (!activeBleedRunnables.isEmpty()) {
                        for (BleedTask bt : activeBleedRunnables) {
                            try { mainHandler.removeCallbacks(bt.r); } catch (Exception ignored) {}
                        }
                        activeBleedRunnables.clear();
                    }
                    // Persistir estado inmediato
                    try {
                        if (getActivity() != null && getActivity() instanceof MainActivity) {
                            MainActivity m = (MainActivity) getActivity();
                            int armorNowToSave = hpBar.getArmorCurrent();
                            android.util.Log.d("CampaignFragment", "projected choso phase change - save: hp=" + enemyCurrentHp + " armor=" + armorNowToSave + " currentEnemyId=" + currentEnemyId);
                            m.getGameDataManager().saveCurrentEnemyState(enemyLevel, true, currentEnemyId);
                            m.getGameDataManager().saveEnemyProgress(enemyCurrentHp, armorNowToSave, chosoSecondPhaseActive, mahitoTransformed);
                        }
                    } catch (Exception ignored) {}
                    // Provide feedback
                }
            }
        } catch (Exception ignored) {}

        // Si es Mahito (boss) y la proyección deja su HP <= 50% y aún no se ha transformado -> transformar antes de aplicar daño
        try {
            if (isBossEnemy && !mahitoTransformed && currentEnemyId != null && "mahito".equals(currentEnemyId)) {
                if (projectedHP <= (enemyMaxHp / 2)) {
                    mahitoTransformed = true;
                    // Transform: cambiar a la forma verdadera
                    try {
                        if (ivEnemyYusepe != null) {
                            ivEnemyYusepe.setImageResource(R.drawable.mahito_true_form);
                            // Para la forma verdadera, preferimos mostrar la imagen completa sin recortar
                            try { ivEnemyYusepe.setScaleType(ImageView.ScaleType.CENTER_INSIDE); } catch (Exception ignored) {}
                            // Escalar más grande y acercar ligeramente la imagen para que no parezca tan alejada
                            float scaleFactor = 1.3f; // aumentado: antes 1.25f
                            int liftDp = 10; // subir ligeramente para que parezca más cerca
                            ivEnemyYusepe.animate()
                                    .scaleX(scaleFactor)
                                    .scaleY(scaleFactor)
                                    .translationYBy(-dpToPx(liftDp))
                                    .setDuration(420)
                                    .withLayer()
                                    .start();
                        }
                    } catch (Exception ignored) {}
                    try { ivEnemyOriginalRes = R.drawable.mahito_true_form; } catch (Exception ignored) {}
                    // Curar toda la vida, rellenar la armadura y actualizar el nombre visible a 'Mahito True Form'
                    enemyCurrentHp = enemyMaxHp;
                    try {
                        if (hpBar != null) {
                            // asegurar HP y barra
                            hpBar.setHealth(enemyMaxHp);
                            hpBar.setBossVisible(true);
                            hpBar.setArmorMaxAndCurrent(currentArmorValue, currentArmorValue);
                            // Cambiar nombre mostrado
                            hpBar.setEnemyName("Mahito True Form");
                            hpBar.setEnemyLevel(enemyLevel);
                        }
                    } catch (Exception ignored) {}
                    // Actualizar identificador actual para persistencia y referencias
                    currentEnemyId = "mahito_true_form";
                    // Persistir estado (opcional)
                    try { if (getActivity() != null && getActivity() instanceof MainActivity) {
                        MainActivity m = (MainActivity) getActivity();
                        android.util.Log.d("CampaignFragment", "mahito transformed - save current state: enemyId=mahito_true_form hp=" + enemyCurrentHp + " armor=" + currentArmorValue);
                        m.getGameDataManager().saveCurrentEnemyState(enemyLevel, isBossEnemy, "mahito_true_form");
                    }} catch (Exception ignored) {}
                    // No aplicar el daño que provocó la transformación
                    return;
                }
            }
        } catch (Exception ignored) {}

        // Aplicar daño normalmente (la barra maneja armadura internamente)
        if (hpBar != null) {
            // Capturar armadura antes del daño para detectar ruptura
            int armorBefore = hpBar.getArmorCurrent();
            hpBar.takeDamage(effectiveAmount);
            try { enemyCurrentHp = hpBar.getCurrentHP(); } catch (Exception ignored) { enemyCurrentHp = Math.max(0, enemyCurrentHp - effectiveAmount); }
            // Actualizar currentArmorValue desde el componente tras el daño
            try { currentArmorValue = hpBar.getArmorCurrent(); } catch (Exception ignored) {}
            int armorAfter = hpBar.getArmorCurrent();
            android.util.Log.d("CampaignFragment", "applyDamageToEnemy: effectiveAmount=" + effectiveAmount + " armorBefore=" + armorBefore + " armorAfter=" + armorAfter + " currentArmorValue=" + currentArmorValue + " chosoPhase=" + chosoSecondPhaseActive);
            // Si rompimos la armadura ahora y aún no habíamos activado la segunda fase, activar
            try {
                if (isBossEnemy && BOSS_CHOSO.equals(currentEnemyId) && !chosoSecondPhaseActive && armorBefore > 0 && armorAfter == 0) {
                    chosoSecondPhaseActive = true;
                    currentEnemyId = BOSS_CHOSO_SECOND;
                    try {
                        if (ivEnemyYusepe != null) {
                            ivEnemyYusepe.setImageResource(R.drawable.choso_boss_second_phase);
                            ivEnemyYusepe.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            ivEnemyYusepe.setScaleX(0.9f);
                            ivEnemyYusepe.setScaleY(0.9f);
                            ivEnemyYusepe.setTranslationY(-dpToPx(4));
                            ivEnemyOriginalRes = R.drawable.choso_boss_second_phase;
                            originalEnemyScaleTypeOrdinal = android.widget.ImageView.ScaleType.CENTER_INSIDE.ordinal();
                        }
                    } catch (Exception ignored) {}
                    if (!activeBleedRunnables.isEmpty()) { for (BleedTask bt : activeBleedRunnables) try { mainHandler.removeCallbacks(bt.r); } catch (Exception ignored) {} activeBleedRunnables.clear(); }
                    try { if (getActivity() != null && getActivity() instanceof MainActivity) { MainActivity m = (MainActivity) getActivity(); m.getGameDataManager().saveCurrentEnemyState(enemyLevel, true, currentEnemyId); m.getGameDataManager().saveEnemyProgress(enemyCurrentHp, armorAfter, chosoSecondPhaseActive, mahitoTransformed); } } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        } else {
            enemyCurrentHp = Math.max(0, enemyCurrentHp - effectiveAmount);
        }

        // Persistir el progreso del enemigo inmediatamente para evitar reset al cambiar de pestañas
        try {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                MainActivity m = (MainActivity) getActivity();
                int armorNow = (hpBar != null) ? hpBar.getArmorCurrent() : currentArmorValue;
                m.getGameDataManager().saveEnemyProgress(enemyCurrentHp, armorNow, chosoSecondPhaseActive, mahitoTransformed);
            }
        } catch (Exception ignored) {}

        // --- Registrar daño total para estadísticas y alimentar cálculo de DPS ---
        try {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).getGameDataManager().addTotalDamage(effectiveAmount);
            }
            synchronized (dpsLog) {
                dpsLog.add(new long[]{System.currentTimeMillis(), effectiveAmount});
            }
        } catch (Exception ignored) {}

        // Animación y popup
        playEnemyDamageAnimation(300);
        showDamagePopup(amount);

        // Si murió, ejecutar la lógica de derrota
        if (enemyCurrentHp <= 0) {
            onEnemyDefeated();
        }

        // Detectar ruptura de armadura para Choso y activar segunda fase
        //        try {
        //            if (isBossEnemy && BOSS_CHOSO.equals(currentEnemyId)) {
        //                int armorAfter = (hpBar != null) ? hpBar.getArmorCurrent() : 0;
        //                // Si la armadura quedó a 0 y antes tenía valor >0 -> segunda fase
        //                if (!chosoSecondPhaseActive && currentArmorValue > 0 && armorAfter == 0) {
        //                    chosoSecondPhaseActive = true;
        //                    // Cambiar imagen a la segunda fase
        //                    try {
        //                        if (ivEnemyYusepe != null) {
        //                            ivEnemyYusepe.setImageResource(R.drawable.choso_boss_second_phase);
        //                            // Mantener la presentación centrada y más discreta en segunda fase
        //                            try {
        //                                ivEnemyYusepe.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        //                                ivEnemyYusepe.setScaleX(0.9f);
        //                                ivEnemyYusepe.setScaleY(0.9f);
        //                                ivEnemyYusepe.setTranslationY(-dpToPx(4));
        //                                ivEnemyOriginalRes = R.drawable.choso_boss_second_phase;
        //                                originalEnemyScaleTypeOrdinal = android.widget.ImageView.ScaleType.CENTER_INSIDE.ordinal();
        //                            } catch (Exception ignored) {
        //                                ivEnemyOriginalRes = R.drawable.choso_boss_second_phase;
        //                            }
        //                            // ligero pulso visual al entrar en segunda fase (muy sutil)
        //                            ivEnemyYusepe.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140).withEndAction(() -> {
        //                                try { ivEnemyYusepe.animate().scaleX(0.95f).scaleY(0.95f).setDuration(120); } catch (Exception ignored) {}
        //                            }).start();
        //                        }
        //                    } catch (Exception ignored) {}
        //                    // Cancelar cualquier DoT activo (Choso segunda fase es inmune a sangrados)
        //                    if (!activeBleedRunnables.isEmpty()) {
        //                        for (Runnable r : activeBleedRunnables) mainHandler.removeCallbacks(r);
        //                        activeBleedRunnables.clear();
        //                    }
        //                    // Persistir inmediatamente el cambio de fase para que no se pierda al cambiar de pestañas
        //                    try {
        //                        if (getActivity() != null && getActivity() instanceof MainActivity) {
        //                            MainActivity m = (MainActivity) getActivity();
        //                            int armorNow = (hpBar != null) ? hpBar.getArmorCurrent() : 0;
        //                            // Actualizar el id actual del enemigo a la fase concreta y guardar también eso para persistencia
        //                            currentEnemyId = BOSS_CHOSO_SECOND;
        //                            android.util.Log.d("CampaignFragment", "save on choso phase change: hp=" + enemyCurrentHp + " armor=" + armorNow + " chosoPhase=" + chosoSecondPhaseActive + " mahitoTrans=" + mahitoTransformed + " currentEnemyId=" + currentEnemyId);
        //                            m.getGameDataManager().saveCurrentEnemyState(enemyLevel, true, currentEnemyId);
        //                            m.getGameDataManager().saveEnemyProgress(enemyCurrentHp, armorNow, chosoSecondPhaseActive, mahitoTransformed);
        //                        }
        //                    } catch (Exception ignored) {}
        //                    // No mostrar mensaje, ya que el cambio de fase es instantáneo y no debe interrumpir el flujo
        //                }
        //            }
        //        } catch (Exception ignored) {}
    }

    private void onEnemyDefeated() {
        // Calcular recompensa de energía basada en el nivel
        int energyReward = 10 + (enemyLevel * 5);

        // Dar recompensa al jugador
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            // En lugar de añadir inmediatamente, reproducimos una animación visual
            // que muestra monedas cayendo hacia el contador y luego actualiza el valor.
            try { animateEnergyDrop(energyReward); } catch (Exception e) { mainActivity.addCursedEnergy(energyReward); }

             // XP del personaje por enemigo
             int xpReward = 20 + (enemyLevel * 2);
             boolean leveledUp = mainActivity.getGameDataManager().addCharacterXp(xpReward);
             if (leveledUp) {
                 // Actualizar habilidades desbloqueadas si el personaje subió de nivel
                 int characterLevel = mainActivity.getGameDataManager().getCharacterLevel();

                 Skill cleave = skillManager.getSkillById("cleave");
                 if (characterLevel >= 5 && cleave != null && !cleave.isUnlocked()) {
                     skillManager.unlockSkill("cleave");
                 }

                 Skill dismantle = skillManager.getSkillById("dismantle");
                 if (characterLevel >= 15 && dismantle != null && !dismantle.isUnlocked()) {
                     skillManager.unlockSkill("dismantle");
                 }

                 Skill fuga = skillManager.getSkillById("fuga");
                 if (characterLevel >= 30 && fuga != null && !fuga.isUnlocked()) {
                     skillManager.unlockSkill("fuga");
                 }

                 Skill domain = skillManager.getSkillById("domain");
                 if (characterLevel >= 80 && domain != null && !domain.isUnlocked()) {
                     skillManager.unlockSkill("domain");
                 }

                 // Actualizar botones inmediatamente
                 updateSkillButtons();
             }

             // Registrar enemigo derrotado en el gestor de datos (SharedPreferences + SQLite si existe)
             try {
                 mainActivity.getGameDataManager().incrementEnemiesDefeated();

                 // ── Insertar score en la tabla scores para el leaderboard ──
                 try {
                     String playerName = mainActivity.getCurrentUsername();
                     if (playerName == null || playerName.isEmpty()) playerName = "player1";
                     String enemyType = isBossEnemy ? "boss" : "normal";
                     String extraJson = "{\"game\":\"kaisen_clicker\",\"enemy_type\":\"" + enemyType + "\",\"enemy_level\":" + enemyLevel + "}";
                     mainActivity.getGameDataManager().getRepository().insertScore(
                             null, playerName, "Kaisen Clicker", enemyLevel, extraJson);
                 } catch (Exception ignored) {}

                 // Si era un boss, registrar detalle del boss derrotado (mahito)
                 if (isBossEnemy) {
                     try {
                         // Registrar con el id real del boss (p. ej. 'choso')
                         String bossIdToRecord = (currentEnemyId != null) ? currentEnemyId : BOSS_CHOSO;
                         mainActivity.getGameDataManager().recordBossDefeat(enemyLevel, bossIdToRecord);
                     } catch (Exception ignored) {}
                     // Además, el boss droppea un cofre: animar drop y registrar en la base de datos
                     try {
                         spawnChestDrop();
                         int newCount = mainActivity.getGameDataManager().incrementChestCount();
                         android.util.Log.d("CampaignFragment", "Boss defeated: chest incremented -> " + newCount);
                     } catch (Exception e) {
                         android.util.Log.w("CampaignFragment", "spawnChestDrop/incrementChestCount failed", e);
                     }
                 } else {
                     // Enemigos normales tienen un 10% de probabilidad de dropear un cofre
                     if (Math.random() < 0.10) {
                         try {
                             spawnChestDrop();
                             int newCount = mainActivity.getGameDataManager().incrementChestCount();
                             android.util.Log.d("CampaignFragment", "Normal enemy dropped chest! -> " + newCount);
                         } catch (Exception e) {
                             android.util.Log.w("CampaignFragment", "Normal enemy chest drop failed", e);
                         }
                     }
                 }
             } catch (Exception e) {
                 // Log en caso de fallo, pero no romper la lógica de recompensa
                 android.util.Log.e("CampaignFragment", "Failed to increment enemies defeated", e);
             }
         }

         // Mostrar mensaje de victoria

        // Limpiar progreso del enemigo guardado: ya está derrotado
        try {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                try { ((MainActivity) getActivity()).getGameDataManager().clearSavedEnemyProgress(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

         // Si acabábamos de derrotar a un boss, asegurarnos de volver a enemigos comunes (Yusepe)
         try {
             if (isBossEnemy) {
                 // Resetear flags y estado relacionado con Mahito
                 mahitoTransformed = false;
                 // Detener adaptación de Mahoraga si estaba activa
                 try { stopMahoragaAdaptation(); } catch (Exception ignored) {}
                  // Forzar siguiente enemigo a Yusepe en la UI mientras se prepara el siguiente spawn
                  currentEnemyId = "yusepe";
                  ivEnemyOriginalRes = R.drawable.yusepe;
                  try {
                      if (ivEnemyYusepe != null) {
                          ivEnemyYusepe.setScaleX(1f);
                          ivEnemyYusepe.setScaleY(1f);
                          ivEnemyYusepe.setTranslationY(0f);
                          ivEnemyYusepe.setImageResource(R.drawable.yusepe);
                      }
                  } catch (Exception ignored) {}
                  try {
                      if (hpBar != null) {
                          hpBar.setBossVisible(false);
                          hpBar.setEnemyName("Yusepe");
                          // Reiniciar barras visuales (serán ajustadas por spawnNewEnemy)
                      }
                  } catch (Exception ignored) {}
             }
         } catch (Exception ignored) {}

        // Clear saved enemy progress when defeated so next spawn doesn't accidentally restore it
        try {
            if (enemyCurrentHp <= 0) {
                if (getActivity() != null && getActivity() instanceof MainActivity) {
                    try { ((MainActivity) getActivity()).getGameDataManager().clearSavedEnemyProgress(); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

         // Subir de nivel
        enemyLevel++; // Subir de nivel

        // Guardar nivel del enemigo
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.getGameDataManager().saveEnemyLevel(enemyLevel);
            // Recalcular daño con bonus de nivel
            damagePerTap = mainActivity.getGameDataManager().calculateTotalDamage();
        }

         // Esperar a que la animación de daño termine (600ms) antes de reiniciar
         if (getView() != null) {
             getView().postDelayed(() -> {
                 spawnNewEnemy();
                 updateEnergyDisplay();
             }, 700); // 700ms para dar tiempo a la animación de 600ms
         } else {
             // Si no hay vista, hacerlo inmediatamente
             spawnNewEnemy();
             updateEnergyDisplay();
         }
     }

    // Anima monedas de energía desde el enemigo hacia el contador y actualiza el valor mostrado
    private void animateEnergyDrop(int amount) {
        android.util.Log.d("CampaignFragment", "animateEnergyDrop: amount=" + amount);
        if (getActivity() == null || getView() == null) {
            try { ((MainActivity) getActivity()).addCursedEnergy(amount); } catch (Exception ignored) {}
            if (getView() != null) updateEnergyDisplay();
            return;
        }

        final ViewGroup root = (ViewGroup) getView();
        final MainActivity mainActivity = (MainActivity) getActivity();
        final int startVal = mainActivity.getCursedEnergy();
        final int endVal = startVal + amount;

        // Spawn a few coins (max 6) and animate them towards the energy display
        int coins = Math.min(6, Math.max(1, amount));
        final int totalDuration = 700;
        int coinSize = dpToPx(28);

        // Calcular posiciones relativas al root usando coordenadas en pantalla (más robusto)
        int[] rootLoc = new int[2];
        root.getLocationOnScreen(rootLoc);

        int[] enemyLoc = new int[2];
        enemyCard.getLocationOnScreen(enemyLoc);
        float startX = (enemyLoc[0] - rootLoc[0]) + enemyCard.getWidth() / 2f - coinSize / 2f;
        float startY = (enemyLoc[1] - rootLoc[1]) + enemyCard.getHeight() / 2f - coinSize / 2f;

        // Usar la tarjeta de energía (a la derecha) como objetivo de la animación
        float endX;
        float endY;
        if (energyDisplayCard != null) {
            int[] targetLoc = new int[2];
            energyDisplayCard.getLocationOnScreen(targetLoc);
            // Apuntar a la esquina derecha/centro de la tarjeta para que las monedas suban hacia el icono
            endX = (targetLoc[0] - rootLoc[0]) + energyDisplayCard.getWidth() - coinSize - dpToPx(6);
            endY = (targetLoc[1] - rootLoc[1]) + (energyDisplayCard.getHeight() / 2f) - coinSize / 2f - dpToPx(4);
        } else {
            int[] tvLoc = new int[2];
            tvPlayerEnergy.getLocationOnScreen(tvLoc);
            endX = (tvLoc[0] - rootLoc[0]) + tvPlayerEnergy.getWidth() / 2f - coinSize / 2f;
            endY = (tvLoc[1] - rootLoc[1]) + tvPlayerEnergy.getHeight() / 2f - coinSize / 2f;
        }

        for (int i = 0; i < coins; i++) {
            final ImageView coin = new ImageView(getContext());
            coin.setImageResource(R.drawable.energy_coin);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(coinSize, coinSize);
            coin.setLayoutParams(lp);
            coin.setX(startX);
            coin.setY(startY);
            root.addView(coin);

            ObjectAnimator tx = ObjectAnimator.ofFloat(coin, "x", startX, endX + (i - coins/2f) * 6);
            ObjectAnimator ty = ObjectAnimator.ofFloat(coin, "y", startY, endY - 20f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(coin, "scaleX", 1f, 0.6f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(coin, "scaleY", 1f, 0.6f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(coin, "alpha", 1f, 0f);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(tx, ty, scaleX, scaleY, alpha);
            set.setStartDelay(i * 80);
            set.setDuration(totalDuration - i * 60);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    try { root.removeView(coin); } catch (Exception ignored) {}
                }
            });
            set.start();
        }

        // Animación numérica del contador
        ValueAnimator val = ValueAnimator.ofInt(startVal, endVal);
        val.setDuration(totalDuration + 150);
        val.addUpdateListener(animation -> {
            int v = (int) animation.getAnimatedValue();
            if (tvPlayerEnergy != null) tvPlayerEnergy.setText(String.valueOf(v));
        });
        val.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                try {
                    mainActivity.addCursedEnergy(amount); // actualiza el valor persistente
                } catch (Exception ignored) {}
                if (tvPlayerEnergy != null) tvPlayerEnergy.setText(String.valueOf(mainActivity.getCursedEnergy()));
                android.util.Log.d("CampaignFragment", "animateEnergyDrop: finished, persisted new energy=" + mainActivity.getCursedEnergy());
            }
        });
        val.start();
    }

    private int dpToPx(int dp) {
        float dens = getResources().getDisplayMetrics().density;
        return (int) (dp * dens + 0.5f);
    }

    private void updateEnergyDisplay() {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            int energy = mainActivity.getCursedEnergy();
            tvPlayerEnergy.setText(String.valueOf(energy));
        }
    }

    private void setUltimateCharge(int value) {
        ultimateCharge = Math.max(0, Math.min(ULTIMATE_CHARGE_MAX, value));
        if (ultimateProgressBar != null) ultimateProgressBar.setProgress(ultimateCharge);
        if (tvUltimatePercent != null) tvUltimatePercent.setText(ultimateCharge + "%");
        if (btnSkillUltimate != null) btnSkillUltimate.setEnabled(ultimateCharge >= ULTIMATE_CHARGE_MAX);
        // Persistir el progreso de la ulti en el GameDataManager
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            try {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.getGameDataManager().saveUltiProgress(ultimateCharge);
            } catch (Exception ignored) {}
        }
    }

    // Ejemplo: cargar ulti al hacer daño
    private void addUltimateCharge(int amount) {
        setUltimateCharge(ultimateCharge + amount);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEnergyDisplay();
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            // Asegurarse de sincronizar el nivel del enemigo con lo guardado
            try { enemyLevel = mainActivity.getGameDataManager().getEnemyLevel(); } catch (Exception ignored) {}
            damagePerTap = mainActivity.getGameDataManager().calculateTotalDamage();
            int characterLevel = mainActivity.getGameDataManager().getCharacterLevel();
            com.example.kaisenclicker.persistence.save.GameDataManager gdm = mainActivity.getGameDataManager();
            int charId = com.example.kaisenclicker.persistence.save.GameDataManager.CHAR_ID_RYOMEN_SUKUNA;

            // DEBUG LOG: imprimir estado actual de personaje/habilidades para depuración
            try {
                android.util.Log.d("CampaignFragment", "DEBUG onResume: characterLevel=" + characterLevel + " charId=" + charId);
                com.example.kaisenclicker.model.skill.Skill sf = skillManager.getSkillById("fuga");
                android.util.Log.d("CampaignFragment", "DEBUG onResume: skill fuga obj=" + (sf==null?"null":"present") + " unlocked=" + (sf!=null?sf.isUnlocked():"-") + " level=" + (sf!=null?sf.getLevel():"-"));
            } catch (Exception ignored) {}

            Skill cleave = skillManager.getSkillById("cleave");
            if (characterLevel >= 5 && cleave != null && !cleave.isUnlocked()) {
                skillManager.unlockSkill("cleave");
                try { gdm.saveSkillLevel("cleave", charId, Math.max(1, gdm.getSkillLevel("cleave", charId))); gdm.setSkillUnlocked("cleave", true, charId); } catch (Exception ignored) {}
            }
            Skill dismantle = skillManager.getSkillById("dismantle");
            if (characterLevel >= 15 && dismantle != null && !dismantle.isUnlocked()) {
                skillManager.unlockSkill("dismantle");
                try { gdm.saveSkillLevel("dismantle", charId, Math.max(1, gdm.getSkillLevel("dismantle", charId))); gdm.setSkillUnlocked("dismantle", true, charId); } catch (Exception ignored) {}
            }
            // Fuga
            Skill fuga = skillManager.getSkillById("fuga");
            if (characterLevel >= 30 && fuga != null && !fuga.isUnlocked()) {
                skillManager.unlockSkill("fuga");
                try { gdm.saveSkillLevel("fuga", charId, Math.max(1, gdm.getSkillLevel("fuga", charId))); gdm.setSkillUnlocked("fuga", true, charId); } catch (Exception ignored) {}
            }
            // Domain (ultimate)
            Skill domain = skillManager.getSkillById("domain");
            if (characterLevel >= 80 && domain != null && !domain.isUnlocked()) {
                skillManager.unlockSkill("domain");
                try { gdm.saveSkillLevel("domain", charId, Math.max(1, gdm.getSkillLevel("domain", charId))); gdm.setSkillUnlocked("domain", true, charId); } catch (Exception ignored) {}
            }
            // Restaurar progreso de ulti al reanudar (por si fue modificado en otra pestaña)
            try {
                int persisted = mainActivity.getGameDataManager().getUltiProgress();
                setUltimateCharge(persisted);
            } catch (Exception ignored) {}

            // FORZAR sincronización desde GameDataManager a skillManager (asegura que los cambios hechos en inventario se reflejen aquí)
            try {
                com.example.kaisenclicker.persistence.save.GameDataManager gdm2 = mainActivity.getGameDataManager();
                if (gdm2 != null) {
                    syncSkillsFromGameDataManager(gdm2, com.example.kaisenclicker.persistence.save.GameDataManager.CHAR_ID_RYOMEN_SUKUNA);
                    android.util.Log.d("CampaignFragment", "onResume: synced skills from GameDataManager");
                    updateSkillButtons();
                }
            } catch (Exception ignored) {}
        }
        // Asegurar que la UI refleje el (posible) nuevo nivel
        spawnNewEnemy();

        // Reiniciar autoclicker (por si se compró/mejoró en la tienda)
        try { setupAutoClickerButton(); } catch (Exception ignored) {}
    }

    /**
     * Permite que código externo (p. ej. CharacterInventoryFragment) fuerce que
     * el fragment refresque su SkillManager desde GameDataManager y actualice
     * la UI de botones de habilidades inmediatamente.
     */
    public void refreshSkillManagerAndUI() {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            try {
                CharacterSkillManager mgr = mainActivity.getGameDataManager().getSkillManager();
                if (mgr != null) skillManager = mgr;
            } catch (Exception ignored) {}
        }
        try { initializeSkills(); } catch (Exception ignored) {}
        try { updateSkillButtons(); } catch (Exception ignored) {}
    }

    @Override
    public void onPause() {
        super.onPause();
        // Guardar progreso de la ulti al pausar la pantalla
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            try { ((MainActivity) getActivity()).getGameDataManager().saveUltiProgress(ultimateCharge); } catch (Exception ignored) {}
            // Guardar progreso del enemigo actual para que al volver no se reinicie
            try {
                MainActivity m = (MainActivity) getActivity();
                int armor = (hpBar != null) ? hpBar.getArmorCurrent() : currentArmorValue;
                android.util.Log.d("CampaignFragment", "onPause save: hp=" + enemyCurrentHp + " armor=" + armor + " chosoPhase=" + chosoSecondPhaseActive + " mahitoTrans=" + mahitoTransformed + " currentEnemyId=" + currentEnemyId);
                m.getGameDataManager().saveEnemyProgress(enemyCurrentHp, armor, chosoSecondPhaseActive, mahitoTransformed);
                // También persistir el estado actual del enemigo para evitar perder la imagen de la segunda fase
                try { m.getGameDataManager().saveCurrentEnemyState(enemyLevel, isBossEnemy, currentEnemyId); } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        }
        cooldownHandler.removeCallbacks(cooldownRefresher);
        mainHandler.removeCallbacks(dpsRefresher);
        // Detener autoclicker al pausar
        stopAutoClicker();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Guardar también en onStop por si onPause no fue llamado antes de destruir la vista
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            try { ((MainActivity) getActivity()).getGameDataManager().saveUltiProgress(ultimateCharge); } catch (Exception ignored) {}
            // Guardar progreso del enemigo actual
            try {
                MainActivity m = (MainActivity) getActivity();
                int armor = (hpBar != null) ? hpBar.getArmorCurrent() : currentArmorValue;
                android.util.Log.d("CampaignFragment", "onStop save: hp=" + enemyCurrentHp + " armor=" + armor + " chosoPhase=" + chosoSecondPhaseActive + " mahitoTrans=" + mahitoTransformed + " currentEnemyId=" + currentEnemyId);
                m.getGameDataManager().saveEnemyProgress(enemyCurrentHp, armor, chosoSecondPhaseActive, mahitoTransformed);
                try { m.getGameDataManager().saveCurrentEnemyState(enemyLevel, isBossEnemy, currentEnemyId); } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Respaldar temporalmente el progreso a la instancia también
        outState.putInt("campaign_ulti_progress", ultimateCharge);
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            try { ((MainActivity) getActivity()).getGameDataManager().saveUltiProgress(ultimateCharge); } catch (Exception ignored) {}
        }
    }

    // Métodos autoclicker
    private Runnable autoClickerRunnable = null;
    private long autoClickerIntervalMs = 1000;

    private void startAutoClicker(int level) {
        stopAutoClicker();
        if (level <= 0) return;
        autoClickerEnabled = true;
        // Intervalo: nivel 1 = cada 2s, nivel 2 = cada 1s, nivel 3 = 666ms, etc.
        autoClickerIntervalMs = Math.max(200, 2000 / level);
        autoClickerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!autoClickerEnabled || getActivity() == null || getView() == null) return;
                try {
                    if (enemyCurrentHp > 0) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        if (mainActivity != null) {
                            try { mainActivity.getGameDataManager().addClick(); } catch (Exception ignored) {}
                            applyDamageToEnemy(damagePerTap, false);
                        }
                    }
                } catch (Exception ignored) {}
                // Reprogramar el siguiente tick
                if (autoClickerEnabled) {
                    mainHandler.postDelayed(this, autoClickerIntervalMs);
                }
            }
        };
        mainHandler.postDelayed(autoClickerRunnable, autoClickerIntervalMs);
    }

    private void stopAutoClicker() {
        autoClickerEnabled = false;
        if (autoClickerRunnable != null) {
            mainHandler.removeCallbacks(autoClickerRunnable);
            autoClickerRunnable = null;
        }
    }

    /**
     * Reproduce una animación breve y cambia temporalmente la imagen del enemigo
     * durationMs: duración total en milisegundos antes de volver a la imagen original
     */
    private void playEnemyDamageAnimation(int durationMs) {
        if (ivEnemyYusepe == null) return;

        // Si el enemigo actual es Choso en segunda fase, no reproducir la animación de daño
        try {
            if (isBossEnemy && chosoSecondPhaseActive && (BOSS_CHOSO.equals(currentEnemyId) || BOSS_CHOSO_SECOND.equals(currentEnemyId))) {
                // En segunda fase Choso no muestra la animación de recibir daño
                return;
            }
        } catch (Exception ignored) {}

        // Cambiar a la imagen de daño
        boolean changedImage = false;
        try {
            if (isBossEnemy) {
                int dmgRes = getBossDamagedDrawableRes(currentEnemyId);
                if (dmgRes != 0) {
                    ivEnemyYusepe.setImageResource(dmgRes);
                    changedImage = true;
                }
                // Si dmgRes == 0 (ej. Mahoraga), no cambiar imagen, solo animar
            } else {
                ivEnemyYusepe.setImageResource(R.drawable.damage_yusepe);
                changedImage = true;
            }
        } catch (Exception ignored) {}

        // Crear animación: sacudida + pequeña escala + ligera atenuación
        TranslateAnimation shake = new TranslateAnimation(-12f, 12f, 0f, 0f);
        shake.setDuration(durationMs);
        shake.setInterpolator(new CycleInterpolator(6));

        ScaleAnimation scale = new ScaleAnimation(
                1f, 1.06f, 1f, 1.06f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(durationMs);

        AlphaAnimation alpha = new AlphaAnimation(1f, 0.92f);
        alpha.setDuration(durationMs);

        AnimationSet set = new AnimationSet(true);
        set.addAnimation(shake);
        set.addAnimation(scale);
        set.addAnimation(alpha);

        ivEnemyYusepe.startAnimation(set);

        // Revertir a la imagen original poco después (solo si cambiamos la imagen)
        if (revertImageRunnable != null) {
            mainHandler.removeCallbacks(revertImageRunnable);
        }
        final boolean didChangeImage = changedImage;
        Runnable r = () -> {
            try {
                ivEnemyYusepe.clearAnimation();
                if (didChangeImage) {
                    ivEnemyYusepe.setImageResource(ivEnemyOriginalRes);
                    try { ivEnemyYusepe.setScaleX(1f); ivEnemyYusepe.setScaleY(1f); ivEnemyYusepe.setTranslationY(0f); } catch (Exception ignored) {}
                    try { ivEnemyYusepe.setScaleType(android.widget.ImageView.ScaleType.values()[originalEnemyScaleTypeOrdinal]); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {
            }
        };
        revertImageRunnable = r;
        mainHandler.postDelayed(r, Math.max(150, durationMs));
    }

    // Crea y anima un icono de cofre en pantalla cuando un boss droppea uno.
    private void spawnChestDrop() {
        if (getView() == null || getActivity() == null) return;
        final ViewGroup root = (ViewGroup) getView();
        final ImageView chest = new ImageView(getContext());
        chest.setImageResource(R.drawable.chest);
        int size = dpToPx(48);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(size, size);
        chest.setLayoutParams(lp);

        // Position chest at the enemyCard center
        int[] rootLoc = new int[2];
        root.getLocationOnScreen(rootLoc);
        int[] enemyLoc = new int[2];
        enemyCard.getLocationOnScreen(enemyLoc);
        float startX = (enemyLoc[0] - rootLoc[0]) + enemyCard.getWidth() / 2f - size / 2f;
        float startY = (enemyLoc[1] - rootLoc[1]) + enemyCard.getHeight() / 2f - size / 2f;

        chest.setX(startX);
        chest.setY(startY);
        root.addView(chest);

        // Target: bottom-right area where chests/loot UI would be. If chest UI exists, animate there; otherwise float up.
        float endX = startX;
        float endY = startY - dpToPx(80);
        int duration = 800;

        ObjectAnimator tx = ObjectAnimator.ofFloat(chest, "x", startX, endX);
        ObjectAnimator ty = ObjectAnimator.ofFloat(chest, "y", startY, endY);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(chest, "scaleX", 1f, 0.8f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(chest, "scaleY", 1f, 0.8f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(chest, "alpha", 1f, 0f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(tx, ty, scaleX, scaleY, alpha);
        set.setDuration(duration);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                try { root.removeView(chest); } catch (Exception ignored) {}
            }
        });
        set.start();
    }

    /**
     * Muestra un GIF/imagen a pantalla completa antes de aplicar daño.
     * Si el recurso es un GIF, Glide lo anima automáticamente.
     * @param gifResId  recurso drawable (GIF o imagen estática) a mostrar
     * @param durationMs  duración de la animación en pantalla (ms)
     * @param onComplete  callback que se ejecuta al terminar (para aplicar el daño)
     */
    private void showSkillGifAnimation(int gifResId, int durationMs, Runnable onComplete) {
        if (skillGifOverlay == null || getContext() == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        // Mostrar overlay
        skillGifOverlay.setVisibility(View.VISIBLE);
        skillGifOverlay.setAlpha(0f);

        // Cargar con Glide (soporta GIF animados)
        try {
            com.bumptech.glide.Glide.with(this)
                    .asGif()
                    .load(gifResId)
                    .into(skillGifOverlay);
        } catch (Exception e) {
            // Fallback: cargar como imagen estática
            skillGifOverlay.setImageResource(gifResId);
        }

        // Fade in rápido
        skillGifOverlay.animate()
                .alpha(1f)
                .setDuration(200)
                .start();

        // Después de la duración, fade out y ejecutar callback de daño
        mainHandler.postDelayed(() -> {
            if (skillGifOverlay == null) {
                if (onComplete != null) onComplete.run();
                return;
            }
            skillGifOverlay.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        skillGifOverlay.setVisibility(View.GONE);
                        // Limpiar Glide para liberar memoria
                        try {
                            if (getContext() != null) {
                                com.bumptech.glide.Glide.with(this).clear(skillGifOverlay);
                            }
                        } catch (Exception ignored) {}
                        if (onComplete != null) onComplete.run();
                    })
                    .start();
        }, durationMs);
    }

    // Muestra un efecto visual para la habilidad Cleave: varios slashes rotando/fading sobre el enemigo
    private void showCleaveEffect() {
        if (getView() == null || getActivity() == null || enemyCard == null) return;
        try { android.util.Log.d("CampaignFragment", "showCleaveEffect() called"); } catch (Exception ignored) {}
         final ViewGroup root = (ViewGroup) getView();

        int slashCount = 3; // número de trazos
        int size = dpToPx(200);

        int[] rootLoc = new int[2];
        root.getLocationOnScreen(rootLoc);
        int[] enemyLoc = new int[2];
        enemyCard.getLocationOnScreen(enemyLoc);
        float baseX = (enemyLoc[0] - rootLoc[0]) + enemyCard.getWidth() / 2f - size / 2f;
        float baseY = (enemyLoc[1] - rootLoc[1]) + enemyCard.getHeight() / 2f - size / 2f;

        for (int i = 0; i < slashCount; i++) {
            final ImageView slash = new ImageView(getContext());
            // Usar la imagen 'cleave_effect.png' existente en res/drawable en lugar del vector genérico
            slash.setImageResource(R.drawable.cleave_effect);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(size, size);
            slash.setLayoutParams(lp);
            // Slight offset for each slash
            float offsetX = (i - slashCount/2f) * dpToPx(12);
            float offsetY = (i * dpToPx(6)) - dpToPx(6);
            slash.setX(baseX + offsetX);
            slash.setY(baseY + offsetY);
            slash.setAlpha(0f);
            slash.setScaleX(0.6f);
            slash.setScaleY(0.6f);
            // Rotation variation
            slash.setRotation((i - 1) * 15f);
            root.addView(slash);

            // Animations: fade in quickly, scale up + rotate a bit, then fade out and translate
            ObjectAnimator aIn = ObjectAnimator.ofFloat(slash, "alpha", 0f, 0.95f);
            ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(slash, "scaleX", 0.6f, 1.05f);
            ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(slash, "scaleY", 0.6f, 1.05f);
            ObjectAnimator rot = ObjectAnimator.ofFloat(slash, "rotation", slash.getRotation(), slash.getRotation() + (i%2==0 ? 10f : -10f));
            ObjectAnimator ty = ObjectAnimator.ofFloat(slash, "y", slash.getY(), slash.getY() - dpToPx(24));
            ObjectAnimator aOut = ObjectAnimator.ofFloat(slash, "alpha", 0.95f, 0f);

            AnimatorSet set = new AnimatorSet();
            int delay = i * 80;
            set.playTogether(aIn, scaleXAnim, scaleYAnim, rot, ty, aOut);
            set.setStartDelay(delay);
            set.setDuration(500);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    try { root.removeView(slash); } catch (Exception ignored) {}
                }
            });
            set.start();
        }
    }

    // Muestra un efecto visual para la habilidad Dismantle: cortes grandes sobre el enemigo
    private void showDismantleEffect() {
        if (getView() == null || getActivity() == null || enemyCard == null) return;
        try { android.util.Log.d("CampaignFragment", "showDismantleEffect() called"); } catch (Exception ignored) {}
        final ViewGroup root = (ViewGroup) getView();

        int slashCount = 4; // más cortes que cleave
        int size = dpToPx(250); // más grande que cleave

        int[] rootLoc = new int[2];
        root.getLocationOnScreen(rootLoc);
        int[] enemyLoc = new int[2];
        enemyCard.getLocationOnScreen(enemyLoc);
        float baseX = (enemyLoc[0] - rootLoc[0]) + enemyCard.getWidth() / 2f - size / 2f;
        float baseY = (enemyLoc[1] - rootLoc[1]) + enemyCard.getHeight() / 2f - size / 2f;

        for (int i = 0; i < slashCount; i++) {
            final ImageView slash = new ImageView(getContext());
            slash.setImageResource(R.drawable.cleave_effect);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(size, size);
            slash.setLayoutParams(lp);
            // Mayor dispersión que cleave
            float offsetX = (i - slashCount / 2f) * dpToPx(18);
            float offsetY = (i * dpToPx(10)) - dpToPx(15);
            slash.setX(baseX + offsetX);
            slash.setY(baseY + offsetY);
            slash.setAlpha(0f);
            slash.setScaleX(0.5f);
            slash.setScaleY(0.5f);
            // Rotaciones más agresivas
            slash.setRotation((i - 1.5f) * 25f);
            root.addView(slash);

            ObjectAnimator aIn = ObjectAnimator.ofFloat(slash, "alpha", 0f, 1f);
            ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(slash, "scaleX", 0.5f, 1.2f);
            ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(slash, "scaleY", 0.5f, 1.2f);
            ObjectAnimator rot = ObjectAnimator.ofFloat(slash, "rotation", slash.getRotation(), slash.getRotation() + (i % 2 == 0 ? 15f : -15f));
            ObjectAnimator ty = ObjectAnimator.ofFloat(slash, "y", slash.getY(), slash.getY() - dpToPx(30));
            ObjectAnimator aOut = ObjectAnimator.ofFloat(slash, "alpha", 1f, 0f);

            AnimatorSet set = new AnimatorSet();
            int delay = i * 70;
            set.playTogether(aIn, scaleXAnim, scaleYAnim, rot, ty, aOut);
            set.setStartDelay(delay);
            set.setDuration(550);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    try { root.removeView(slash); } catch (Exception ignored) {}
                }
            });
            set.start();
        }
    }

    /**
     * Recalcula el DPS en tiempo real con ventana deslizante y persiste el pico si es nuevo récord.
     */
    private void recalculateRealtimeDps() {
        try {
            long now = System.currentTimeMillis();
            long cutoff = now - DPS_WINDOW_MS;
            long totalInWindow = 0;
            synchronized (dpsLog) {
                java.util.Iterator<long[]> it = dpsLog.iterator();
                while (it.hasNext()) {
                    long[] entry = it.next();
                    if (entry[0] < cutoff) {
                        it.remove();
                    } else {
                        totalInWindow += entry[1];
                    }
                }
            }
            currentRealtimeDps = totalInWindow / (DPS_WINDOW_MS / 1000.0);
            // Guardar peak DPS si es récord
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).getGameDataManager().updatePeakDpsIfHigher(currentRealtimeDps);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Copia los niveles y estado 'unlocked' desde GameDataManager al skillManager en memoria.
     */
    private void syncSkillsFromGameDataManager(com.example.kaisenclicker.persistence.save.GameDataManager gdm, int characterId) {
        if (gdm == null || skillManager == null) return;
        String[] known = new String[]{"cleave", "dismantle", "fuga", "domain"};
        for (String sid : known) {
            try {
                int lvl = gdm.getSkillLevel(sid, Integer.valueOf(characterId));
                boolean unlocked = gdm.isSkillUnlocked(sid, Integer.valueOf(characterId));
                com.example.kaisenclicker.model.skill.Skill s = skillManager.getSkillById(sid);
                if (s != null) {
                    if (lvl > 0) s.setLevel(lvl);
                    if (unlocked) s.setUnlocked(true);
                }
            } catch (Exception e) {
                android.util.Log.w("CampaignFragment", "syncSkillsFromGameDataManager failed for " + sid, e);
            }
        }
    }

    /**
     * Muestra un diálogo con las habilidades/mecánicas especiales del boss actual.
     */
    private void showBossAbilitiesDialog(String bossId) {
        if (getContext() == null || bossId == null) return;

        String bossName = getBossDisplayName(bossId);
        String abilities = getBossAbilities(bossId);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                .setTitle("⚔️ " + bossName)
                .setMessage(abilities)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    /**
     * Devuelve una descripción de las habilidades/mecánicas del boss.
     */
    private String getBossAbilities(String bossId) {
        if (bossId == null) return "Sin información.";
        switch (bossId) {
            case BOSS_CHOSO:
                return "🛡️ Armadura de Sangre\n" +
                       "Choso posee una armadura que absorbe daño antes de que afecte a su vida.\n\n" +
                       "🩸 Segunda Fase\n" +
                       "Al romper su armadura, Choso entra en su segunda fase:\n" +
                       "• Recibe solo el 60% del daño.\n" +
                       "• Es inmune a efectos de sangrado (Cleave).\n" +
                       "• No muestra animación de daño.";
            case BOSS_CHOSO_SECOND:
                return "🩸 Segunda Fase (activa)\n" +
                       "• Recibe solo el 60% del daño.\n" +
                       "• Inmune a efectos de sangrado (Cleave).\n" +
                       "• No muestra animación de daño.";
            case BOSS_MAHITO:
                return "🎭 Transfiguración del Alma\n" +
                       "Cuando la vida de Mahito baja al 50%, se transforma en su Forma Verdadera:\n" +
                       "• Se cura completamente.\n" +
                       "• Su armadura se restaura.\n" +
                       "• Cambia de apariencia.\n\n" +
                       "⚠️ El golpe que provoca la transformación no hace daño.";
            case BOSS_MAHORAGA:
                return "🔄 Rueda de la Adaptación\n" +
                       "Mahoraga se adapta progresivamente al daño de tus habilidades:\n" +
                       "• Cada 5 segundos gana 1 stack de adaptación.\n" +
                       "• Cada stack reduce el daño de habilidades un 8%.\n" +
                       "• Máximo 5 stacks (40% de reducción a habilidades).\n\n" +
                       "🛡️ Durabilidad Sobrenatural\n" +
                       "Por cada golpe recibido (taps y habilidades), Mahoraga reduce el daño entrante:\n" +
                       "• 0.5% menos daño por cada golpe recibido.\n" +
                       "• Máximo 35% de reducción acumulada.\n\n" +
                       "💡 Mahoraga es el boss con más durabilidad. Usa habilidades potentes rápidamente.";
            default:
                return "Sin información disponible.";
        }
    }

    /**
     * Resetea el progreso de los enemigos y vuelve a empezar desde el nivel 1.
     * Persiste el nivel en GameDataManager y limpia progreso guardado del enemigo actual.
     */
    public void resetEnemiesToLevelOne() {
        enemyLevel = 1;
        enemyMaxHp = calculateEnemyMaxHp(enemyLevel);
        enemyCurrentHp = enemyMaxHp;
        isBossEnemy = false;
        chosoSecondPhaseActive = false;
        mahitoTransformed = false;
        currentEnemyId = "yusepe";
        currentArmorValue = 0;

        if (hpBar != null) {
            try {
                hpBar.setMaxHealth(enemyMaxHp);
                hpBar.setHealth(enemyCurrentHp);
                hpBar.setEnemyLevel(enemyLevel);
                hpBar.setEnemyName("Yusepe");
                hpBar.setBossVisible(false);
                hpBar.setArmorMaxAndCurrent(0, 0);
            } catch (Exception ignored) {}
        }

        // Persistir nivel y limpiar progreso guardado
        try {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                MainActivity m = (MainActivity) getActivity();
                try { m.getGameDataManager().saveEnemyLevel(enemyLevel); } catch (Exception ignored) {}
                try { m.getGameDataManager().clearSavedEnemyProgress(); } catch (Exception ignored) {}
                try { m.getGameDataManager().saveCurrentEnemyState(enemyLevel, false, currentEnemyId); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // Respawnear enemigo inmediatamente
        try { spawnNewEnemy(); } catch (Exception ignored) {}
        try { updateEnergyDisplay(); } catch (Exception ignored) {}
    }

    // Inicia el proceso periódico por el que Mahoraga gana stacks de adaptación (reduce daño recibido por habilidades)
    private void startMahoragaAdaptation() {
        try {
            stopMahoragaAdaptation();
            mahoragaAdaptationActive = true;
            mahoragaAdaptationStacks = 0; // empezar desde 0
            mahoragaHitsReceived = 0; // resetear golpes recibidos
            mahoragaAdaptRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!mahoragaAdaptationActive) return;
                        // Incrementar stacks hasta el tope
                        if (mahoragaAdaptationStacks < MAHORAGA_MAX_ADAPT_STACKS) {
                            mahoragaAdaptationStacks++;
                            // Actualizar UI runtime si existe
                            try { if (hpBar != null) hpBar.setMahoragaStacks(mahoragaAdaptationStacks); } catch (Exception ignored) {}
                            try { android.util.Log.d("CampaignFragment", "Mahoraga adaptation stacks=" + mahoragaAdaptationStacks); } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}
                    try { if (mahoragaAdaptationActive) mainHandler.postDelayed(this, MAHORAGA_ADAPT_TICK_MS); } catch (Exception ignored) {}
                }
            };

            // Inicializar indicador junto a la barra de HP si existe
            try { if (hpBar != null) hpBar.setMahoragaStacks(0); } catch (Exception ignored) {}

            mainHandler.postDelayed(mahoragaAdaptRunnable, MAHORAGA_ADAPT_TICK_MS);
        } catch (Exception ignored) {}
    }

    // Detiene y limpia la adaptación de Mahoraga
    private void stopMahoragaAdaptation() {
        try {
            mahoragaAdaptationActive = false;
            if (mahoragaAdaptRunnable != null) mainHandler.removeCallbacks(mahoragaAdaptRunnable);
            mahoragaAdaptRunnable = null;
            mahoragaAdaptationStacks = 0;
            mahoragaHitsReceived = 0;
            // Clear indicator from hpBar if present
            try { if (hpBar != null) hpBar.clearMahoragaStacks(); } catch (Exception ignored) {}
            mahoragaAdaptIcon = null;
            mahoragaAdaptText = null;
        } catch (Exception ignored) {}
    }
}
