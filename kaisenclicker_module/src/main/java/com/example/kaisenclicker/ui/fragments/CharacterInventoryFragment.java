package com.example.kaisenclicker.ui.fragments;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.kaisenclicker.R;
import com.example.kaisenclicker.persistence.save.GameDataManager;
import com.google.android.material.button.MaterialButton;

public class CharacterInventoryFragment extends Fragment {

    // Character info views
    private ImageView characterImage;
    private TextView characterName;
    private TextView characterRarity;
    private TextView characterLevel;
    private TextView characterPower;

    // Navigation
    private View btnBack;

    // Skill 1 views
    // private MaterialCardView skill1Card; // no usado
    private ImageView skill1Icon;
    private TextView skill1Name;
    private TextView skill1Level;
    private TextView skill1Description;
    private MaterialButton btnUpgradeSkill1;
    private TextView skill1CostAmount;

    // Skill 2 views
    // private MaterialCardView skill2Card; // no usado
    private ImageView skill2Icon;
    private TextView skill2Name;
    private TextView skill2Level;
    private TextView skill2Description;
    private MaterialButton btnUpgradeSkill2;
    private TextView skill2CostAmount;

    // Skill 3 views
    // private MaterialCardView skill3Card; // no usado
    private ImageView skill3Icon;
    private TextView skill3Name;
    private TextView skill3Level;
    private TextView skill3Description;
    private MaterialButton btnUpgradeSkill3;
    private TextView skill3CostAmount;

    // Ultimate skill views
    private TextView ultimateName;
    private TextView ultimateLevel;
    private TextView ultimateDescription;
    private ProgressBar ultimateChargeProgress;
    private TextView chargePercentage;
    private MaterialButton btnUpgradeUltimate;
    private TextView ultimateCostAmount1;
    private TextView ultimateCostAmount2;

    // Character switching
    private MaterialButton btnSwitchCharacter;
    private int currentCharacterId = 1; // por defecto Ryomen Sukuna
    private java.util.List<Integer> availableCharacterIds = new java.util.ArrayList<>();

    // Skill levels and costs (state)
    private int skill1LevelValue = 1;
    private int skill2LevelValue = 1;
    private int skill3LevelValue = 1;
    private int ultimateLevelValue = 1;
    private int skill1CostValue = 500;
    private int skill2CostValue = 750;
    private int skill3CostValue = 900;
    private int ultimateCostValue1 = 50;
    private int ultimateCostValue2 = 10;

    // Character stats (state)
    private static final int BASE_POWER = 12450;
    private static final int POWER_PER_LEVEL = 50;
    private static final int SKILL1_UNLOCK_LEVEL = 5;
    private static final int SKILL2_UNLOCK_LEVEL = 15;
    private static final int SKILL3_UNLOCK_LEVEL = 30;
    private static final int ULTIMATE_UNLOCK_LEVEL = 80;
    private int powerFromSkillUpgrades = 0;

    // XP UI
    private TextView characterXpText;
    private ProgressBar characterXpBar;

    // Skill views for lock overlay
    private View skill1View;
    private View skill2View;
    private View skill3View;
    private View ultimateView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_character_inventory_new, container, false);

        // Leer el personaje seleccionado persistido (si existe) para mostrar el personaje correcto
        try {
            if (getActivity() != null && getActivity() instanceof com.example.kaisenclicker.ui.activities.MainActivity) {
                com.example.kaisenclicker.persistence.save.GameDataManager gdm = ((com.example.kaisenclicker.ui.activities.MainActivity) getActivity()).getGameDataManager();
                try { currentCharacterId = gdm.getSelectedCharacterId(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        initializeViews(view);
        setupClickListeners();
        loadCharacterData();
        updateAllSkillUI();

        // Ejecutar acción solicitada: poner todos los personajes al nivel máximo con todas las habilidades desbloqueadas
        try {
            setAllCharactersMax();
            // Además desbloquear todas las skills y sincronizar al SkillManager (obtenemos gdm desde MainActivity)
            try {
                if (getActivity() != null && getActivity() instanceof com.example.kaisenclicker.ui.activities.MainActivity) {
                    GameDataManager gdm = ((com.example.kaisenclicker.ui.activities.MainActivity) getActivity()).getGameDataManager();
                    try { if (gdm != null) gdm.unlockAllSkillsForAllCharacters(); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        return view;
    }

    /**
     * Debug / helper: pone a todos los personajes disponibles al nivel máximo y desbloquea/eleva todas sus habilidades.
     * Esta operación persiste los cambios usando GameDataManager (SQLite o SharedPreferences según configuración).
     */
    private void setAllCharactersMax() {
        if (getActivity() == null) return;
        com.example.kaisenclicker.ui.activities.MainActivity main = (com.example.kaisenclicker.ui.activities.MainActivity) getActivity();
        GameDataManager gdm = main.getGameDataManager();

        final int MAX_CHAR_LEVEL = 99;
        final int MAX_SKILL_LEVEL = 10;

        int[] characterIds = new int[]{GameDataManager.CHAR_ID_RYOMEN_SUKUNA, GameDataManager.CHAR_ID_SATORU_GOJO};
        // skills known across characters
        String[] allSkills = new String[]{"cleave", "dismantle", "fuga", "domain", "amplificacion_azul", "ritual_inverso_rojo", "vacio_purpura"};

        for (int cid : characterIds) {
            try {
                // Unlock character (legacy + SQL upsert)
                gdm.unlockCharacterById(cid);

                // If repository present, ensure character record has the desired level
                try {
                    if (gdm.getRepository() != null) {
                        gdm.getRepository().upsertCharacter(cid, true, MAX_CHAR_LEVEL, 0);
                    } else {
                        // fallback: save global character level (best-effort)
                        gdm.saveCharacterLevel(MAX_CHAR_LEVEL);
                        gdm.saveCharacterXp(0);
                    }
                } catch (Exception e) {
                    android.util.Log.w("CharacterInventory", "setAllCharactersMax: upsertCharacter failed for " + cid, e);
                }

                // Set all relevant skills to max and unlocked for this character
                for (String sid : allSkills) {
                    try {
                        gdm.saveSkillLevel(sid, cid, MAX_SKILL_LEVEL);
                        gdm.setSkillUnlocked(sid, true, cid);
                        // Also update in-memory SkillManager if present
                        try {
                            if (gdm.getSkillManager() != null) {
                                com.example.kaisenclicker.model.skill.Skill s = gdm.getSkillManager().getSkillById(sid);
                                if (s != null) {
                                    s.setLevel(MAX_SKILL_LEVEL);
                                    s.setUnlocked(true);
                                }
                            }
                        } catch (Exception ignored) {}
                    } catch (Exception e) {
                        android.util.Log.w("CharacterInventory", "setAllCharactersMax: saveSkill failed " + sid + " for " + cid, e);
                    }
                }
            } catch (Exception e) {
                android.util.Log.w("CharacterInventory", "setAllCharactersMax: failed for character " + cid, e);
            }
        }

        // Refresh UI for current character
        try { loadCharacterDataForId(currentCharacterId); updateAllSkillUI(); updateCharacterPowerUI(); updateCharacterXpUI(); } catch (Exception ignored) {}

        // Notify CampaignFragment (si está presente) para que refresque su SkillManager y muestre las skills desbloqueadas en combate
        try {
            if (getActivity() != null) {
                java.util.List<androidx.fragment.app.Fragment> frags = getActivity().getSupportFragmentManager().getFragments();
                if (frags != null) {
                    for (androidx.fragment.app.Fragment f : frags) {
                        if (f instanceof com.example.kaisenclicker.ui.fragments.CampaignFragment) {
                            try { ((com.example.kaisenclicker.ui.fragments.CampaignFragment) f).refreshSkillManagerAndUI(); } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        if (getContext() != null) android.widget.Toast.makeText(getContext(), "Todos los personajes puestos al nivel máximo", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void initializeViews(View view) {
        // Navigation
        btnBack = view.findViewById(R.id.btn_back);
        // Character full-width image
        characterImage = view.findViewById(R.id.character_image);
        // Button to switch characters (if present in layout)
        btnSwitchCharacter = view.findViewById(R.id.btn_switch_character);

        // Character info
        // characterImage = view.findViewById(R.id.character_image);
        characterName = view.findViewById(R.id.character_name);
        characterRarity = view.findViewById(R.id.character_rarity);
        characterLevel = view.findViewById(R.id.character_level);
        characterPower = view.findViewById(R.id.character_power);
        characterXpText = view.findViewById(R.id.character_xp_text);
        characterXpBar = view.findViewById(R.id.character_xp_bar);

        // Skill 1 (Normal)
        skill1View = view.findViewById(R.id.skill_normal_1);
        skill1Icon = skill1View.findViewById(R.id.skill_icon);
        skill1Name = skill1View.findViewById(R.id.skill_name);
        skill1Level = skill1View.findViewById(R.id.skill_level);
        skill1Description = skill1View.findViewById(R.id.skill_description);
        btnUpgradeSkill1 = skill1View.findViewById(R.id.btn_upgrade_skill);
        skill1CostAmount = skill1View.findViewById(R.id.cost_amount);

        // Skill 2 (Normal)
        skill2View = view.findViewById(R.id.skill_normal_2);
        skill2Icon = skill2View.findViewById(R.id.skill_icon);
        skill2Name = skill2View.findViewById(R.id.skill_name);
        skill2Level = skill2View.findViewById(R.id.skill_level);
        skill2Description = skill2View.findViewById(R.id.skill_description);
        btnUpgradeSkill2 = skill2View.findViewById(R.id.btn_upgrade_skill);
        skill2CostAmount = skill2View.findViewById(R.id.cost_amount);

        // Skill 3 (Normal)
        skill3View = view.findViewById(R.id.skill_normal_3);
        skill3Icon = skill3View.findViewById(R.id.skill_icon);
        skill3Name = skill3View.findViewById(R.id.skill_name);
        skill3Level = skill3View.findViewById(R.id.skill_level);
        skill3Description = skill3View.findViewById(R.id.skill_description);
        btnUpgradeSkill3 = skill3View.findViewById(R.id.btn_upgrade_skill);
        skill3CostAmount = skill3View.findViewById(R.id.cost_amount);

        // Ultimate skill
        ultimateView = view.findViewById(R.id.skill_ultimate);
        ultimateName = ultimateView.findViewById(R.id.skill_name);
        ultimateLevel = ultimateView.findViewById(R.id.skill_level);
        ultimateDescription = ultimateView.findViewById(R.id.skill_description);
        ultimateChargeProgress = ultimateView.findViewById(R.id.ultimate_charge_progress);
        chargePercentage = ultimateView.findViewById(R.id.charge_percentage);
        btnUpgradeUltimate = ultimateView.findViewById(R.id.btn_upgrade_ultimate);
        ultimateCostAmount1 = ultimateView.findViewById(R.id.cost_amount_1);
        ultimateCostAmount2 = ultimateView.findViewById(R.id.cost_amount_2);
    }

    private void setupClickListeners() {
        // Back button - regresa al juego principal (Campaign)
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }

        // Switch character button
        if (btnSwitchCharacter != null) {
            btnSwitchCharacter.setOnClickListener(v -> cycleCharacter());
        }

        if (btnUpgradeSkill1 != null) {
            btnUpgradeSkill1.setOnClickListener(v -> upgradeSkill(1, skill1Level));
        }

        if (btnUpgradeSkill2 != null) {
            btnUpgradeSkill2.setOnClickListener(v -> upgradeSkill(2, skill2Level));
        }

        if (btnUpgradeSkill3 != null) {
            btnUpgradeSkill3.setOnClickListener(v -> upgradeSkill(3, skill3Level));
        }

        if (btnUpgradeUltimate != null) {
            btnUpgradeUltimate.setOnClickListener(v -> upgradeUltimateSkill());
        }
    }

    private void loadCharacterData() {
        // load default character data for currentCharacterId
        if (currentCharacterId <= 0) currentCharacterId = 1;
        loadCharacterDataForId(currentCharacterId);

        // UI de habilidades y ultimate se configura dentro de loadCharacterDataForId para cada personaje
    }

    private void cycleCharacter() {
        // Ensure we have a list of available character ids
        if (availableCharacterIds.isEmpty()) {
            // try load from repository via GameDataManager
            if (getActivity() != null && getActivity() instanceof com.example.kaisenclicker.ui.activities.MainActivity) {
                GameDataManager gdm = ((com.example.kaisenclicker.ui.activities.MainActivity) getActivity()).getGameDataManager();
                try {
                    if (gdm.getRepository() != null) {
                        java.util.Map<Integer, com.example.kaisenclicker.persistence.save.SqlRepository.CharacterRecord> all = gdm.getRepository().getAllCharacters();
                        if (all != null && !all.isEmpty()) {
                            availableCharacterIds.addAll(all.keySet());
                        }
                    }
                } catch (Exception ignored) {}
            }
            // fallback to known ids
            if (availableCharacterIds.isEmpty()) {
                availableCharacterIds.add(1); // Ryomen Sukuna
                availableCharacterIds.add(2); // Satoru Gojo
            }
        }

        // find index of currentCharacterId
        int idx = availableCharacterIds.indexOf(currentCharacterId);
        if (idx < 0) idx = 0;
        idx = (idx + 1) % availableCharacterIds.size();
        currentCharacterId = availableCharacterIds.get(idx);
        // reload UI for the new character
        loadCharacterDataForId(currentCharacterId);

        // Persistir la selección del personaje para que otras pantallas la lean
        try {
            if (getActivity() != null && getActivity() instanceof com.example.kaisenclicker.ui.activities.MainActivity) {
                GameDataManager gdm = ((com.example.kaisenclicker.ui.activities.MainActivity) getActivity()).getGameDataManager();
                if (gdm != null) gdm.setSelectedCharacterId(currentCharacterId);
            }
        } catch (Exception ignored) {}

        // Notificar al CampaignFragment (si existe) para que refresque inmediatamente su SkillManager/UI
        try {
            if (getActivity() != null) {
                java.util.List<androidx.fragment.app.Fragment> frags = getActivity().getSupportFragmentManager().getFragments();
                for (androidx.fragment.app.Fragment f : frags) {
                    if (f instanceof com.example.kaisenclicker.ui.fragments.CampaignFragment) {
                        try { ((com.example.kaisenclicker.ui.fragments.CampaignFragment) f).refreshSkillManagerAndUI(); } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Carga los datos del personaje especificado por id.
     */
    private void loadCharacterDataForId(int characterId) {
        // Update displayed name
        if (characterName != null) {
            if (characterId == GameDataManager.CHAR_ID_RYOMEN_SUKUNA) {
                characterName.setText(getString(R.string.ryomen_sukuna));
            } else {
                // default to sample name (Satoru Gojo) for id=2
                characterName.setText(getString(R.string.sample_character_name));
            }
        }

        // Set the main character image depending on the character id
        try {
            if (characterImage != null) {
                if (characterId == GameDataManager.CHAR_ID_RYOMEN_SUKUNA) {
                    characterImage.setImageResource(R.drawable.sukunapfp);
                    characterImage.setAlpha(0.25f);
                } else if (characterId == GameDataManager.CHAR_ID_SATORU_GOJO) {
                    // Associate the Gojo drawable with the Gojo character
                    characterImage.setImageResource(R.drawable.gojo_character);
                    characterImage.setAlpha(0.25f);
                } else {
                    characterImage.setImageResource(R.drawable.sukunapfp);
                    characterImage.setAlpha(0.25f);
                }
            }
        } catch (Exception ignored) {}

        // Load skill levels from GameDataManager for this character using character-specific skill keys
        int characterIdToUse = characterId;
        if (getActivity() != null && getActivity() instanceof com.example.kaisenclicker.ui.activities.MainActivity) {
            GameDataManager gdm = ((com.example.kaisenclicker.ui.activities.MainActivity) getActivity()).getGameDataManager();
            try {
                boolean isGojo = (characterIdToUse == GameDataManager.CHAR_ID_SATORU_GOJO);
                String key1 = isGojo ? "amplificacion_azul" : "cleave";
                String key2 = isGojo ? "ritual_inverso_rojo" : "dismantle";
                String key3 = isGojo ? "vacio_purpura" : "fuga";
                String keyU = "domain";
                int saved1 = gdm.getSkillLevel(key1, characterIdToUse);
                skill1LevelValue = saved1 > 0 ? saved1 : 1;
                int saved2 = gdm.getSkillLevel(key2, characterIdToUse);
                skill2LevelValue = saved2 > 0 ? saved2 : 1;
                int saved3 = gdm.getSkillLevel(key3, characterIdToUse);
                skill3LevelValue = saved3 > 0 ? saved3 : 1;
                int savedU = gdm.getSkillLevel(keyU, characterIdToUse);
                ultimateLevelValue = savedU > 0 ? savedU : 1;

                // Ensure unlock state
                if (skill1LevelValue > 0) gdm.setSkillUnlocked(key1, true, characterIdToUse);
                if (skill2LevelValue > 0) gdm.setSkillUnlocked(key2, true, characterIdToUse);
                if (skill3LevelValue > 0) gdm.setSkillUnlocked(key3, true, characterIdToUse);
                if (ultimateLevelValue > 0) gdm.setSkillUnlocked(keyU, true, characterIdToUse);
            } catch (Exception ignored) {}
        }

        // Update UI fields for skill levels
        updateAllSkillUI();
        updateCharacterPowerUI();
        updateCharacterXpUI();

        // Configure skill names, descriptions and icons based on character
        if (characterId == GameDataManager.CHAR_ID_RYOMEN_SUKUNA) {
            // Sukuna
            if (skill1Name != null) skill1Name.setText(getString(R.string.skill_cleave_name));
            if (skill1Description != null) skill1Description.setText(getString(R.string.skill_cleave_desc));
            if (skill1CostAmount != null) skill1CostAmount.setText(String.valueOf(skill1CostValue));
            if (skill1Icon != null) skill1Icon.setImageResource(R.drawable.cleave_image);

            if (skill2Name != null) skill2Name.setText(getString(R.string.skill_dismantle_name));
            if (skill2Description != null) skill2Description.setText(getString(R.string.skill_dismantle_desc));
            if (skill2CostAmount != null) skill2CostAmount.setText(String.valueOf(skill2CostValue));
            if (skill2Icon != null) skill2Icon.setImageResource(R.drawable.dismanteal);

            if (skill3Name != null) skill3Name.setText(getString(R.string.skill_fuga_name));
            if (skill3Description != null) skill3Description.setText(getString(R.string.skill_fuga_desc));
            if (skill3CostAmount != null) skill3CostAmount.setText(String.valueOf(skill3CostValue));
            if (skill3Icon != null) skill3Icon.setImageResource(R.drawable.fuga_image);

            if (ultimateName != null) ultimateName.setText(getString(R.string.ultimate_name_local));
            if (ultimateDescription != null) ultimateDescription.setText(getString(R.string.ultimate_desc_local));
            if (ultimateChargeProgress != null) ultimateChargeProgress.setProgress(75);
            if (chargePercentage != null) chargePercentage.setText(getString(R.string.charge_percentage_local, 75));
            if (ultimateCostAmount1 != null) ultimateCostAmount1.setText(String.valueOf(ultimateCostValue1));
            if (ultimateCostAmount2 != null) ultimateCostAmount2.setText(String.valueOf(ultimateCostValue2));
        } else if (characterId == GameDataManager.CHAR_ID_SATORU_GOJO) {
            // Satoru Gojo - nuevas habilidades
            if (skill1Name != null) skill1Name.setText(getString(R.string.skill_amplificacion_azul_name));
            if (skill1Description != null) skill1Description.setText(getString(R.string.skill_amplificacion_azul_desc));
            if (skill1CostAmount != null) skill1CostAmount.setText(String.valueOf(skill1CostValue));
            if (skill1Icon != null) skill1Icon.setImageResource(R.drawable.blue_skill);

            if (skill2Name != null) skill2Name.setText(getString(R.string.skill_ritual_inverso_rojo_name));
            if (skill2Description != null) skill2Description.setText(getString(R.string.skill_ritual_inverso_rojo_desc));
            if (skill2CostAmount != null) skill2CostAmount.setText(String.valueOf(skill2CostValue));
            if (skill2Icon != null) skill2Icon.setImageResource(R.drawable.red_skill);

            if (skill3Name != null) skill3Name.setText(getString(R.string.skill_vacio_purpura_name));
            if (skill3Description != null) skill3Description.setText(getString(R.string.skill_vacio_purpura_desc));
            if (skill3CostAmount != null) skill3CostAmount.setText(String.valueOf(skill3CostValue));
            if (skill3Icon != null) skill3Icon.setImageResource(R.drawable.hollow_purple);

            if (ultimateName != null) ultimateName.setText(getString(R.string.ultimate_name_local));
            if (ultimateDescription != null) ultimateDescription.setText(getString(R.string.ultimate_desc_gojo));
            if (ultimateChargeProgress != null) ultimateChargeProgress.setProgress(75);
            if (chargePercentage != null) chargePercentage.setText(getString(R.string.charge_percentage_local, 75));
            if (ultimateCostAmount1 != null) ultimateCostAmount1.setText(String.valueOf(ultimateCostValue1));
            if (ultimateCostAmount2 != null) ultimateCostAmount2.setText(String.valueOf(ultimateCostValue2));
        }

        // Asignación única del icono de la ultimate según el personaje seleccionado (evita comprobaciones redundantes)
        try {
            if (ultimateView != null) {
                ImageView ultimateIcon = ultimateView.findViewById(R.id.skill_icon);
                if (ultimateIcon != null) {
                    if (characterId == GameDataManager.CHAR_ID_SATORU_GOJO) ultimateIcon.setImageResource(R.drawable.gojo_domain);
                    else ultimateIcon.setImageResource(R.drawable.sukuna_domain);
                }
            }
        } catch (Exception ignored) {}
    }

    private void upgradeSkill(int skillNumber, TextView levelView) {
        int level = getCharacterLevel();
        int requiredLevel = (skillNumber == 1) ? SKILL1_UNLOCK_LEVEL
            : (skillNumber == 2) ? SKILL2_UNLOCK_LEVEL
            : SKILL3_UNLOCK_LEVEL;

        if (level < requiredLevel) {
            Toast.makeText(getContext(), getString(R.string.locked_requirement, requiredLevel), Toast.LENGTH_SHORT).show();
            updateAllSkillUI();
            return;
        }

        // If skill is already unlocked, upgrade it
        animateUpgrade(levelView);

        String skillKey;
        boolean isGojoCurrent = (currentCharacterId == GameDataManager.CHAR_ID_SATORU_GOJO);
        if (skillNumber == 1) skillKey = isGojoCurrent ? "amplificacion_azul" : "cleave";
        else if (skillNumber == 2) skillKey = isGojoCurrent ? "ritual_inverso_rojo" : "dismantle";
        else if (skillNumber == 3) skillKey = isGojoCurrent ? "vacio_purpura" : "fuga";
        else skillKey = "skill_unknown";

        if (skillNumber == 1) {
             skill1LevelValue++;
             skill1CostValue = (int) (skill1CostValue * 1.2);
             // persist
             if (getActivity() != null && getActivity() instanceof com.example.kaisenclicker.ui.activities.MainActivity) {
                 GameDataManager gdm = ((com.example.kaisenclicker.ui.activities.MainActivity) getActivity()).getGameDataManager();
                 int characterIdToSave = currentCharacterId > 0 ? currentCharacterId : 1;
                 try { gdm.saveSkillLevel(skillKey, characterIdToSave, skill1LevelValue); gdm.setSkillUnlocked(skillKey, true, characterIdToSave); } catch (Exception ignored) {}
             }
         } else if (skillNumber == 2) {
             skill2LevelValue++;
             skill2CostValue = (int) (skill2CostValue * 1.2);
             if (getActivity() != null && getActivity() instanceof com.example.kaisenclicker.ui.activities.MainActivity) {
                 GameDataManager gdm = ((com.example.kaisenclicker.ui.activities.MainActivity) getActivity()).getGameDataManager();
                 int characterIdToSave = currentCharacterId > 0 ? currentCharacterId : 1;
                 try { gdm.saveSkillLevel(skillKey, characterIdToSave, skill2LevelValue); gdm.setSkillUnlocked(skillKey, true, characterIdToSave); } catch (Exception ignored) {}
             }
         } else if (skillNumber == 3) {
             skill3LevelValue++;
             skill3CostValue = (int) (skill3CostValue * 1.2);
             if (getActivity() != null && getActivity() instanceof com.example.kaisenclicker.ui.activities.MainActivity) {
                 GameDataManager gdm = ((com.example.kaisenclicker.ui.activities.MainActivity) getActivity()).getGameDataManager();
                 int characterIdToSave = currentCharacterId > 0 ? currentCharacterId : 1;
                 try { gdm.saveSkillLevel(skillKey, characterIdToSave, skill3LevelValue); gdm.setSkillUnlocked(skillKey, true, characterIdToSave); } catch (Exception ignored) {}
             }
         }

        // Actualizar poder del personaje
        powerFromSkillUpgrades += 150;
        updateCharacterPowerUI();

        int newLevel = (skillNumber == 1) ? skill1LevelValue : (skillNumber == 2) ? skill2LevelValue : skill3LevelValue;
        Toast.makeText(getContext(), getString(R.string.skill_upgraded_to, skillNumber, newLevel), Toast.LENGTH_SHORT).show();
        updateAllSkillUI();
    }

    private void upgradeUltimateSkill() {
        int level = getCharacterLevel();
        if (level < ULTIMATE_UNLOCK_LEVEL) {
            Toast.makeText(getContext(), getString(R.string.locked_requirement, ULTIMATE_UNLOCK_LEVEL), Toast.LENGTH_SHORT).show();
            updateAllSkillUI();
            return;
        }

        // If ultimate is already unlocked, upgrade it
        // Animación épica para la ultimate
        animateUpgradeEpic(ultimateLevel);

        // Actualizar nivel y costos
        ultimateLevelValue++;
        ultimateCostValue1 = (int) (ultimateCostValue1 * 1.5);
        ultimateCostValue2 = (int) (ultimateCostValue2 * 1.3);

        // persist ultimate level for current character
        int characterIdToSave = currentCharacterId > 0 ? currentCharacterId : 1;
        if (getActivity() != null && getActivity() instanceof com.example.kaisenclicker.ui.activities.MainActivity) {
            GameDataManager gdm = ((com.example.kaisenclicker.ui.activities.MainActivity) getActivity()).getGameDataManager();
            try { gdm.saveSkillLevel("domain", characterIdToSave, ultimateLevelValue); gdm.setSkillUnlocked("domain", true, characterIdToSave); } catch (Exception ignored) {}
        }

        // Actualizar poder del personaje (mayor incremento)
        powerFromSkillUpgrades += 500;
        updateCharacterPowerUI();

        Toast.makeText(getContext(), getString(R.string.ultimate_upgraded, ultimateLevelValue), Toast.LENGTH_LONG).show();
        updateAllSkillUI();
    }

    private void updateCharacterPowerUI() {
        if (characterPower == null) return;
        int level = getCharacterLevel();
        int totalPower = BASE_POWER + ((Math.max(level - 1, 0)) * POWER_PER_LEVEL) + powerFromSkillUpgrades;
        java.text.NumberFormat formatter = java.text.NumberFormat.getInstance();
        characterPower.setText(getString(R.string.power_format, formatter.format(totalPower)));
    }

    private void updateCharacterXpUI() {
        if (characterXpText == null || characterXpBar == null) return;
        if (getActivity() != null) {
            com.example.kaisenclicker.ui.activities.MainActivity mainActivity =
                (com.example.kaisenclicker.ui.activities.MainActivity) getActivity();
            int level = mainActivity.getGameDataManager().getCharacterLevel();
            int xp = mainActivity.getGameDataManager().getCharacterXp();
            int max = mainActivity.getGameDataManager().getCharacterXpMax(level);
            characterXpText.setText(getString(R.string.xp_format, xp, max));
            characterXpBar.setMax(max);
            characterXpBar.setProgress(xp);
        }
    }

    private int getCharacterLevel() {
        if (getActivity() != null) {
            com.example.kaisenclicker.ui.activities.MainActivity mainActivity =
                (com.example.kaisenclicker.ui.activities.MainActivity) getActivity();
            return mainActivity.getGameDataManager().getCharacterLevel();
        }
        return 1;
    }

    private void updateAllSkillUI() {
        int level = getCharacterLevel();
        int cid = currentCharacterId > 0 ? currentCharacterId : GameDataManager.CHAR_ID_RYOMEN_SUKUNA;
        // escoger descripciones/nombres dinámicos según personaje
        String desc1 = (cid == GameDataManager.CHAR_ID_SATORU_GOJO) ? getString(R.string.skill_amplificacion_azul_desc) : getString(R.string.skill_cleave_desc);
        String desc2 = (cid == GameDataManager.CHAR_ID_SATORU_GOJO) ? getString(R.string.skill_ritual_inverso_rojo_desc) : getString(R.string.skill_dismantle_desc);
        String desc3 = (cid == GameDataManager.CHAR_ID_SATORU_GOJO) ? getString(R.string.skill_vacio_purpura_desc) : getString(R.string.skill_fuga_desc);
        String ultimateDesc = (cid == GameDataManager.CHAR_ID_SATORU_GOJO) ? getString(R.string.ultimate_desc_gojo) : getString(R.string.ultimate_desc_local);
        com.example.kaisenclicker.model.character.CharacterSkillManager skillManager = null;
        if (getActivity() != null && getActivity() instanceof com.example.kaisenclicker.ui.activities.MainActivity) {
            GameDataManager gdm = ((com.example.kaisenclicker.ui.activities.MainActivity) getActivity()).getGameDataManager();
            skillManager = gdm.getSkillManager();
        }

        // Intentar usar el SkillManager para obtener el estado real de cada habilidad
        com.example.kaisenclicker.model.skill.Skill cleave = null;
        com.example.kaisenclicker.model.skill.Skill dismantle = null;
        com.example.kaisenclicker.model.skill.Skill fuga = null;
        com.example.kaisenclicker.model.skill.Skill domain = null;

        if (skillManager != null) {
            cleave = skillManager.getSkillById("cleave");
            dismantle = skillManager.getSkillById("dismantle");
            fuga = skillManager.getSkillById("fuga");
            domain = skillManager.getSkillById("domain");

            // Desbloquear si cumple nivel y aún no está desbloqueada en el manager
            if (level >= SKILL1_UNLOCK_LEVEL && cleave != null && !cleave.isUnlocked()) {
                skillManager.unlockSkill("cleave");
                cleave = skillManager.getSkillById("cleave"); // refrescar referencia
            }
            if (level >= SKILL2_UNLOCK_LEVEL && dismantle != null && !dismantle.isUnlocked()) {
                skillManager.unlockSkill("dismantle");
                dismantle = skillManager.getSkillById("dismantle");
            }
            if (level >= SKILL3_UNLOCK_LEVEL && fuga != null && !fuga.isUnlocked()) {
                skillManager.unlockSkill("fuga");
                fuga = skillManager.getSkillById("fuga");
            }
            if (level >= ULTIMATE_UNLOCK_LEVEL && domain != null && !domain.isUnlocked()) {
                skillManager.unlockSkill("domain");
                domain = skillManager.getSkillById("domain");
            }
        }

        // Determinar estado desbloqueado with fallback por nivel si no hay manager
        boolean cleaveUnlocked = (cleave != null) ? cleave.isUnlocked() : (level >= SKILL1_UNLOCK_LEVEL);
        boolean dismantleUnlocked = (dismantle != null) ? dismantle.isUnlocked() : (level >= SKILL2_UNLOCK_LEVEL);
        boolean fugaUnlocked = (fuga != null) ? fuga.isUnlocked() : (level >= SKILL3_UNLOCK_LEVEL);
        boolean domainUnlocked = (domain != null) ? domain.isUnlocked() : (level >= ULTIMATE_UNLOCK_LEVEL);

        // Skill 1 (Cleave)
        if (!cleaveUnlocked) {
            skill1View.setAlpha(0.4f);
            skill1Icon.setAlpha(0.4f);
            skill1Name.setAlpha(0.4f);
            skill1Level.setAlpha(0.4f);
            skill1Description.setAlpha(0.4f);
            if (btnUpgradeSkill1 != null) {
                btnUpgradeSkill1.setEnabled(false);
                btnUpgradeSkill1.setText(getString(R.string.btn_locked));
            }
            skill1Level.setText("-");
            skill1Description.setText(getString(R.string.locked_requirement, SKILL1_UNLOCK_LEVEL));
        } else {
            skill1View.setAlpha(1f);
            skill1Icon.setAlpha(1f);
            skill1Name.setAlpha(1f);
            skill1Level.setAlpha(1f);
            skill1Description.setAlpha(1f);
            if (btnUpgradeSkill1 != null) {
                btnUpgradeSkill1.setEnabled(true);
                btnUpgradeSkill1.setText(getString(R.string.btn_upgrade));
            }
            skill1Level.setText(getString(R.string.lvl_format, skill1LevelValue));
            skill1Description.setText(desc1);
        }

        // Skill 2 (Dismantle)
        if (!dismantleUnlocked) {
            skill2View.setAlpha(0.4f);
            skill2Icon.setAlpha(0.4f);
            skill2Name.setAlpha(0.4f);
            skill2Level.setAlpha(0.4f);
            skill2Description.setAlpha(0.4f);
            if (btnUpgradeSkill2 != null) {
                btnUpgradeSkill2.setEnabled(false);
                btnUpgradeSkill2.setText(getString(R.string.btn_locked));
            }
            skill2Level.setText("-");
            skill2Description.setText(getString(R.string.locked_requirement, SKILL2_UNLOCK_LEVEL));
        } else {
            skill2View.setAlpha(1f);
            skill2Icon.setAlpha(1f);
            skill2Name.setAlpha(1f);
            skill2Level.setAlpha(1f);
            skill2Description.setAlpha(1f);
            if (btnUpgradeSkill2 != null) {
                btnUpgradeSkill2.setEnabled(true);
                btnUpgradeSkill2.setText(getString(R.string.btn_upgrade));
            }
            skill2Level.setText(getString(R.string.lvl_format, skill2LevelValue));
            skill2Description.setText(desc2);
        }

        // Skill 3 (Fuga)
        if (!fugaUnlocked) {
            skill3View.setAlpha(0.4f);
            skill3Icon.setAlpha(0.4f);
            skill3Name.setAlpha(0.4f);
            skill3Level.setAlpha(0.4f);
            skill3Description.setAlpha(0.4f);
            if (btnUpgradeSkill3 != null) {
                btnUpgradeSkill3.setEnabled(false);
                btnUpgradeSkill3.setText(getString(R.string.btn_locked));
            }
            skill3Level.setText("-");
            skill3Description.setAlpha(0.4f);
            skill3Description.setText(getString(R.string.locked_requirement, SKILL3_UNLOCK_LEVEL));
        } else {
            skill3View.setAlpha(1f);
            skill3Icon.setAlpha(1f);
            skill3Name.setAlpha(1f);
            skill3Level.setAlpha(1f);
            skill3Description.setAlpha(1f);
            if (btnUpgradeSkill3 != null) {
                btnUpgradeSkill3.setEnabled(true);
                btnUpgradeSkill3.setText(getString(R.string.btn_upgrade));
            }
            skill3Level.setText(getString(R.string.lvl_format, skill3LevelValue));
            skill3Description.setText(desc3);
        }

        // Ultimate
        if (!domainUnlocked) {
            ultimateView.setAlpha(0.4f);
            ultimateName.setAlpha(0.4f);
            ultimateLevel.setAlpha(0.4f);
            ultimateDescription.setAlpha(0.4f);
            if (btnUpgradeUltimate != null) {
                btnUpgradeUltimate.setEnabled(false);
                btnUpgradeUltimate.setText(getString(R.string.btn_locked));
            }
            ultimateLevel.setText("-");
            ultimateDescription.setAlpha(0.4f);
            ultimateDescription.setText(getString(R.string.locked_requirement, ULTIMATE_UNLOCK_LEVEL));
        } else {
            ultimateView.setAlpha(1f);
            ultimateName.setAlpha(1f);
            ultimateLevel.setAlpha(1f);
            ultimateDescription.setAlpha(1f);
            if (btnUpgradeUltimate != null) {
                btnUpgradeUltimate.setEnabled(true);
                btnUpgradeUltimate.setText(getString(R.string.btn_upgrade));
            }
            ultimateLevel.setText(getString(R.string.lvl_format, ultimateLevelValue));
            ultimateDescription.setText(ultimateDesc);
        }
    }

    private void updateSkillUI(boolean isUnlocked, TextView nameView, TextView descriptionView,
                                TextView levelView, TextView costView, int costValue, int levelValue,
                                View skillView, MaterialButton button, String description, int requiredLevel) {
        if (!isUnlocked) {
            // Mostrar estado bloqueado
            skillView.setVisibility(View.VISIBLE);
            nameView.setAlpha(0.5f);
            descriptionView.setAlpha(0.5f);
            levelView.setAlpha(0.5f);
            levelView.setText(getString(R.string.lvl_unknown));
            descriptionView.setText(getString(R.string.locked_requirement, requiredLevel));
            button.setText(getString(R.string.btn_locked));
            button.setEnabled(false);
            button.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null));
        } else {
            // Mostrar habilidad desbloqueada
            skillView.setVisibility(View.VISIBLE);
            nameView.setAlpha(1f);
            descriptionView.setAlpha(1f);
            levelView.setAlpha(1f);
            levelView.setText(getString(R.string.lvl_format, levelValue));
            descriptionView.setText(description);
            button.setText(getString(R.string.btn_upgrade));
            button.setEnabled(true);
            button.setBackgroundColor(getResources().getColor(R.color.purple_500, null));
        }
        costView.setText(String.valueOf(costValue));
    }

    private void updateUltimateUI() {
        int level = getCharacterLevel();
        boolean ultimateUnlocked = level >= ULTIMATE_UNLOCK_LEVEL;

        if (ultimateName == null || ultimateDescription == null || ultimateLevel == null ||
            btnUpgradeUltimate == null || ultimateCostAmount1 == null || ultimateCostAmount2 == null) {
            return;
        }

        if (!ultimateUnlocked) {
            // Mostrar estado bloqueado
            if (ultimateView != null) {
                ultimateView.setVisibility(View.VISIBLE);
            }
            ultimateName.setAlpha(0.5f);
            ultimateDescription.setAlpha(0.5f);
            ultimateLevel.setAlpha(0.5f);
            ultimateLevel.setText(getString(R.string.lvl_unknown));
            ultimateDescription.setText(getString(R.string.locked_requirement, ULTIMATE_UNLOCK_LEVEL));
            btnUpgradeUltimate.setText(getString(R.string.btn_locked));
            btnUpgradeUltimate.setEnabled(false);
            btnUpgradeUltimate.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null));
        } else {
            // Mostrar habilidad ultimate desbloqueada
            if (ultimateView != null) {
                ultimateView.setVisibility(View.VISIBLE);
            }
            ultimateName.setAlpha(1f);
            ultimateDescription.setAlpha(1f);
            ultimateLevel.setAlpha(1f);
            ultimateLevel.setText(getString(R.string.lvl_format, ultimateLevelValue));
            ultimateDescription.setText(currentCharacterId == GameDataManager.CHAR_ID_SATORU_GOJO
                ? getString(R.string.ultimate_desc_gojo)
                : getString(R.string.ultimate_desc_local));
            btnUpgradeUltimate.setText(getString(R.string.btn_upgrade));
            btnUpgradeUltimate.setEnabled(true);
            btnUpgradeUltimate.setBackgroundColor(getResources().getColor(R.color.purple_500, null));
        }

        ultimateCostAmount1.setText(String.valueOf(ultimateCostValue1));
        ultimateCostAmount2.setText(String.valueOf(ultimateCostValue2));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCharacterXpUI();
        updateCharacterPowerUI();
        updateAllSkillUI();
    }

    private void animateUpgrade(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f);
        scaleX.setDuration(300);
        scaleY.setDuration(300);
        scaleX.start();
        scaleY.start();
    }

    private void animateUpgradeEpic(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.5f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f);
        scaleX.setDuration(500);
        scaleY.setDuration(500);
        rotation.setDuration(500);
        scaleX.start();
        scaleY.start();
        rotation.start();
    }
}
