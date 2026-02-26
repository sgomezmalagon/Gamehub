package com.example.kaisenclicker.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.kaisenclicker.R;
import com.example.kaisenclicker.persistence.save.GameDataManager;
import com.example.kaisenclicker.ui.activities.MainActivity;
import com.google.android.material.button.MaterialButton;

import java.util.Random;

public class ChestFragment extends Fragment {

    private static final String TAG = "ChestFragment";

    private View btnBack;
    private TextView tvCursedEnergy;
    private TextView tvChestCount;
    private MaterialButton btnOpenTestChest;

    private Random random;

    // Probabilidad de que al abrir un cofre salga el personaje (valor por defecto 30%)
    private static final double CHARACTER_DROP_CHANCE = 0.30; // 30%

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chest, container, false);

        random = new Random();

        initializeViews(view);
        setupClickListeners();
        updateEnergyDisplay();
        updateChestCountDisplay();

        return view;
    }

    private void initializeViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        tvCursedEnergy = view.findViewById(R.id.tv_cursed_energy);
        tvChestCount = view.findViewById(R.id.tv_chest_count);
        btnOpenTestChest = view.findViewById(R.id.btn_open_test_chest);
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Open test chest button
        btnOpenTestChest.setOnClickListener(v -> abrirCofre());
    }

    /**
     * Calcula la recompensa de energía maldita escalada según el nivel del enemigo.
     * Base: 50-200. Por cada 10 niveles se añade un multiplicador.
     */
    private int calculateEnergyReward(int enemyLevel) {
        int baseMin = 50;
        int baseMax = 200;
        // Bonus: +25% por cada 10 niveles de enemigo
        int tier = enemyLevel / 10; // 0 para nivel 1-9, 1 para 10-19, etc.
        double multiplier = 1.0 + (tier * 0.25);
        int scaledMin = (int) (baseMin * multiplier);
        int scaledMax = (int) (baseMax * multiplier);
        return scaledMin + random.nextInt(Math.max(1, scaledMax - scaledMin + 1));
    }

    private void abrirCofre() {
        if (getActivity() == null || !(getActivity() instanceof MainActivity)) {
            return;
        }

        MainActivity mainActivity = (MainActivity) getActivity();
        GameDataManager gdm = mainActivity.getGameDataManager();

        // Verificar que tiene cofres disponibles
        int chestCount = gdm.getChestCount();
        if (chestCount <= 0) {
            new AlertDialog.Builder(getContext())
                    .setTitle("❌ Sin cofres")
                    .setMessage("No tienes cofres disponibles.\nDerrota enemigos y bosses para conseguir más.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Consumir un cofre
        gdm.decrementChestCount();
        updateChestCountDisplay();

        // Leer el estado persistido (prefs o SQL) para evitar depender de una copia en memoria
        boolean wasUnlocked = false;
        try {
            wasUnlocked = gdm.isCharacterUnlockedById(1);
            Log.d(TAG, "abrirCofre: isCharacterUnlockedById(1) -> " + wasUnlocked);
        } catch (Exception e) {
            Log.w(TAG, "abrirCofre: failed to read persisted unlock state", e);
        }

        // Obtener nivel del enemigo para escalar recompensa
        int enemyLevel = gdm.getEnemyLevel();

        // Siempre tirar la probabilidad: si pasa, se considera un intento de dar personaje
        boolean givesCharacter = (random.nextDouble() < CHARACTER_DROP_CHANCE);

        StringBuilder rewardMessage = new StringBuilder();
        rewardMessage.append("🎁 COFRE ABIERTO!\n\n");

        if (givesCharacter) {
            // Mostrar diálogo de invocación del personaje; la persistencia se realizará al click en la imagen
            // También otorgar energía maldita
            int energyReward = calculateEnergyReward(enemyLevel);

            LayoutInflater li = LayoutInflater.from(getContext());
            View dialogView = li.inflate(R.layout.dialog_character_summon, null);
            ImageView ivChar = dialogView.findViewById(R.id.ivCharacterSummon);
            TextView tvReward = dialogView.findViewById(R.id.tvCharacterReward);
            tvReward.setVisibility(View.GONE);

            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            // Aparición animada
            ivChar.setAlpha(0f);
            ivChar.setScaleX(0.8f);
            ivChar.setScaleY(0.8f);
            ivChar.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).start();

            final boolean initiallyUnlocked = wasUnlocked;
            ivChar.setOnClickListener(vw -> {
                try {
                    // Otorgar energía maldita siempre
                    mainActivity.addCursedEnergy(energyReward);

                    if (!initiallyUnlocked) {
                        // Intentar desbloquear en persistencia
                        boolean ok = gdm.unlockCharacterById(1);
                        if (ok) {
                            tvReward.setText("✨ PERSONAJE DESBLOQUEADO!\n🎭 Ryomen Sukuna\n🔮 +" + energyReward + " Energía Maldita");
                            Log.d(TAG, "abrirCofre: character unlocked on click + energy=" + energyReward);
                        } else {
                            tvReward.setText("✨ PERSONAJE DESBLOQUEADO (ERROR AL GUARDAR)\n🎭 Ryomen Sukuna\n🔮 +" + energyReward + " Energía Maldita");
                            Log.w(TAG, "abrirCofre: unlockCharacterById returned false");
                        }
                    } else {
                        tvReward.setText("✨ PERSONAJE YA ADQUIRIDO\n🎭 Ryomen Sukuna\n🔮 +" + energyReward + " Energía Maldita");
                        Log.d(TAG, "abrirCofre: character already unlocked + energy=" + energyReward);
                    }

                    tvReward.setVisibility(View.VISIBLE);
                    ivChar.setClickable(false);
                    ivChar.animate().scaleX(0.9f).scaleY(0.9f).setDuration(200).start();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        dialog.dismiss();
                        updateEnergyDisplay();
                        updateChestCountDisplay();
                    }, 800);
                } catch (Exception e) {
                    Log.e(TAG, "abrirCofre: failed during character summon click", e);
                    dialog.dismiss();
                }
            });

            dialog.show();
        } else {
            // Otorgar energía maldita mediante la animación rare summon: mostrar diálogo con la imagen
            int energyReward = calculateEnergyReward(enemyLevel);

            // Inflate dialog view
            LayoutInflater li = LayoutInflater.from(getContext());
            View dialogView = li.inflate(R.layout.dialog_rare_summon, null);
            ImageView iv = dialogView.findViewById(R.id.ivRareSummon);
            TextView tv = dialogView.findViewById(R.id.tvCursedEnergy);
            tv.setVisibility(View.GONE);

            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            // Animación de aparición
            iv.setAlpha(0f);
            iv.setScaleX(0.8f);
            iv.setScaleY(0.8f);
            iv.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).start();

            iv.setOnClickListener(vw -> {
                // Al hacer click en la imagen, otorgar la energía y mostrar el texto dentro del diálogo
                try {
                    mainActivity.addCursedEnergy(energyReward);
                    tv.setText("🔮 +" + energyReward + " Energía Maldita");
                    tv.setVisibility(View.VISIBLE);
                    Log.d(TAG, "abrirCofre: rare summon clicked, granted=" + energyReward);

                    // Pequeña animación y cierre
                    iv.setClickable(false);
                    iv.animate().scaleX(0.8f).scaleY(0.8f).setDuration(200).start();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        dialog.dismiss();
                        updateEnergyDisplay();
                        updateChestCountDisplay();
                    }, 650);
                } catch (Exception e) {
                    Log.e(TAG, "abrirCofre: failed to grant cursed energy", e);
                    dialog.dismiss();
                }
            });

            dialog.show();
        }

        // Actualizar display de energía (si la recompensa no usó el diálogo) y en general
        updateEnergyDisplay();
    }

    private void mostrarRecompensa(String message) {
        if (getContext() == null) {
            return;
        }

        new AlertDialog.Builder(getContext())
            .setTitle("🎉 RECOMPENSA")
            .setMessage(message)
            .setPositiveButton("ACEPTAR", (dialog, which) -> dialog.dismiss())
            .setCancelable(false)
            .show();
    }

    private void updateEnergyDisplay() {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            int energy = mainActivity.getCursedEnergy();
            tvCursedEnergy.setText(String.valueOf(energy));
        }
    }

    private void updateChestCountDisplay() {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            int chests = mainActivity.getGameDataManager().getChestCount();
            if (tvChestCount != null) {
                tvChestCount.setText(String.valueOf(chests));
            }
            if (btnOpenTestChest != null) {
                btnOpenTestChest.setEnabled(chests > 0);
                btnOpenTestChest.setText(chests > 0 ? "🎁 ABRIR COFRE (" + chests + ")" : "🔒 SIN COFRES");
                btnOpenTestChest.setAlpha(chests > 0 ? 1f : 0.5f);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEnergyDisplay();
        updateChestCountDisplay();
    }
}
