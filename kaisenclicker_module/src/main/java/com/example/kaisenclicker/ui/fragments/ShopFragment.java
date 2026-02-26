package com.example.kaisenclicker.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.kaisenclicker.R;
import com.example.kaisenclicker.ui.activities.MainActivity;

public class ShopFragment extends Fragment {

    // Header
    private View btnBack;
    private TextView currentEnergy;

    // Upgrades - Tap Damage
    private TextView tapDamageLevel;
    private TextView tapDamageCost;
    private ImageButton btnBuyTapDamage;

    // Upgrades - Auto Clicker
    private TextView autoClickerLevel;
    private TextView autoClickerCost;
    private ImageButton btnBuyAutoClicker;

    // Upgrades - Black Flash
    private TextView criticalDamageLevel;
    private TextView criticalDamageCost;
    private ImageButton btnBuyCriticalDamage;

    // Upgrades - Energy Boost
    private TextView energyBoostLevel;
    private TextView energyBoostCost;
    private ImageButton btnBuyEnergyBoost;

    // Upgrade levels and costs
    private int tapDamageLevel_val = 1;
    private int autoClickerLevel_val = 1;
    private int criticalDamageLevel_val = 1;
    private int energyBoostLevel_val = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop, container, false);

        // Cargar niveles guardados desde GameDataManager (no sobrescribirlos)
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            try {
                tapDamageLevel_val = mainActivity.getGameDataManager().getTapDamageLevel();
            } catch (Exception ignored) { tapDamageLevel_val = 1; }
            try {
                autoClickerLevel_val = mainActivity.getGameDataManager().getAutoClickerLevel();
            } catch (Exception ignored) { autoClickerLevel_val = 0; }
            try {
                criticalDamageLevel_val = mainActivity.getGameDataManager().getCriticalDamageLevel();
            } catch (Exception ignored) { criticalDamageLevel_val = 1; }
            try {
                energyBoostLevel_val = mainActivity.getGameDataManager().getEnergyBoostLevel();
            } catch (Exception ignored) { energyBoostLevel_val = 1; }
        } else {
            // Fallback a valores por defecto si no hay Activity
            tapDamageLevel_val = 1;
            autoClickerLevel_val = 0;
            criticalDamageLevel_val = 1;
            energyBoostLevel_val = 1;
        }

        initializeViews(view);
        setupClickListeners();
        updateEnergyDisplay();

        return view;
    }

    private void initializeViews(View view) {
        // Header
        btnBack = view.findViewById(R.id.btn_back);
        currentEnergy = view.findViewById(R.id.current_gold);

        // Tap Damage Upgrade
        tapDamageLevel = view.findViewById(R.id.upgrade_level_1);
        tapDamageCost = view.findViewById(R.id.upgrade_cost_1);
        btnBuyTapDamage = view.findViewById(R.id.btn_buy_upgrade_1);

        // Auto Clicker Upgrade
        autoClickerLevel = view.findViewById(R.id.upgrade_level_2);
        autoClickerCost = view.findViewById(R.id.upgrade_cost_2);
        btnBuyAutoClicker = view.findViewById(R.id.btn_buy_upgrade_2);

        // Critical Damage Upgrade
        criticalDamageLevel = view.findViewById(R.id.upgrade_level_3);
        criticalDamageCost = view.findViewById(R.id.upgrade_cost_3);
        btnBuyCriticalDamage = view.findViewById(R.id.btn_buy_upgrade_3);

        // Energy Boost Upgrade
        energyBoostLevel = view.findViewById(R.id.upgrade_level_4);
        energyBoostCost = view.findViewById(R.id.upgrade_cost_4);
        btnBuyEnergyBoost = view.findViewById(R.id.btn_buy_upgrade_4);

        // Set initial values
        updateUpgradeUI();
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Tap Damage
        btnBuyTapDamage.setOnClickListener(v -> buyUpgrade("Reforzamiento EM", tapDamageLevel_val, 100,
            () -> {
                tapDamageLevel_val++;
                if (getActivity() != null && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).getGameDataManager().saveTapDamageLevel(tapDamageLevel_val);
                }
                updateUpgradeUI();
            }));

        // Auto Clicker
        btnBuyAutoClicker.setOnClickListener(v -> buyUpgrade("Auto Clicker", autoClickerLevel_val, 500,
            () -> {
                autoClickerLevel_val++;
                if (getActivity() != null && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).getGameDataManager().saveAutoClickerLevel(autoClickerLevel_val);
                }
                updateUpgradeUI();
            }));

        // Black Flash
        btnBuyCriticalDamage.setOnClickListener(v -> buyUpgrade("Black Flash", criticalDamageLevel_val, 300,
            () -> {
                criticalDamageLevel_val++;
                if (getActivity() != null && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).getGameDataManager().saveCriticalDamageLevel(criticalDamageLevel_val);
                }
                updateUpgradeUI();
            }));

        // Energy Boost
        btnBuyEnergyBoost.setOnClickListener(v -> buyUpgrade("Energy Boost", energyBoostLevel_val, 200,
            () -> {
                energyBoostLevel_val++;
                if (getActivity() != null && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).getGameDataManager().saveEnergyBoostLevel(energyBoostLevel_val);
                }
                updateUpgradeUI();
            }));
    }

    private void buyUpgrade(String upgradeName, int level, int baseCost, Runnable onSuccess) {
        if (getActivity() == null || !(getActivity() instanceof MainActivity)) {
            return;
        }

        MainActivity mainActivity = (MainActivity) getActivity();
        int cost = baseCost * Math.max(1, level);

        if (mainActivity.getCursedEnergy() >= cost) {
            mainActivity.addCursedEnergy(-cost); // Restar energía
            Toast.makeText(getContext(), "✅ " + upgradeName + " Level " + level + " purchased!", Toast.LENGTH_SHORT).show();
            onSuccess.run();
            updateEnergyDisplay();
        } else {
            Toast.makeText(getContext(), "❌ Not enough energy! Need: " + cost + ", Have: " + mainActivity.getCursedEnergy(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUpgradeUI() {
        // Tap Damage - Mostrar el daño progresivo
        int tapDamagePerLevel = 15;
        int tapDamageBonus = tapDamagePerLevel * tapDamageLevel_val;
        tapDamageLevel.setText("Level " + tapDamageLevel_val + "\n+" + tapDamageBonus + " damage per level");
        int tapCost = 100 * tapDamageLevel_val;
        tapDamageCost.setText(Integer.toString(tapCost));

        // Auto Clicker - Mostrar clicks por segundo
        if (autoClickerLevel_val <= 0) {
            autoClickerLevel.setText("No comprado\nCompra para activar auto-click");
        } else {
            double clicksPerSec = (double) autoClickerLevel_val / 2.0;
            autoClickerLevel.setText("Level " + autoClickerLevel_val + "\n" + String.format("%.1f", clicksPerSec) + " clicks/sec");
        }
        int autoCost = 500 * Math.max(1, autoClickerLevel_val);
        autoClickerCost.setText(Integer.toString(autoCost));

        // Critical Damage (Black Flash) - Mostrar porcentaje progresivo
        int criticalDamageBonus = 10 * criticalDamageLevel_val;
        criticalDamageLevel.setText("Level " + criticalDamageLevel_val + "\n+" + criticalDamageBonus + "% damage");
        int critCost = 300 * criticalDamageLevel_val;
        criticalDamageCost.setText(Integer.toString(critCost));

        // Energy Boost - Mostrar boost progresivo
        int energyBoostPercentage = 10 * energyBoostLevel_val;
        energyBoostLevel.setText("Level " + energyBoostLevel_val + "\n+" + energyBoostPercentage + "% energy");
        int energyCost = 200 * energyBoostLevel_val;
        energyBoostCost.setText(Integer.toString(energyCost));
    }

    private void updateEnergyDisplay() {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            int energy = mainActivity.getCursedEnergy();
            currentEnergy.setText(String.valueOf(energy));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEnergyDisplay();
    }
}
