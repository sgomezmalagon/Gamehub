package com.example.kaisenclicker.ui.activities;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.kaisenclicker.R;
import com.example.kaisenclicker.persistence.save.GameDataManager;
import com.example.kaisenclicker.ui.fragments.CampaignFragment;
import com.example.kaisenclicker.ui.fragments.CharacterInventoryFragment;
import com.example.kaisenclicker.ui.fragments.ShopFragment;
import com.example.kaisenclicker.ui.fragments.ChestFragment;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import com.example.kaisenclicker.persistence.save.AppDatabaseHelper;

public class MainActivity extends AppCompatActivity {

    private MaterialCardView navShop;
    private MaterialCardView navChest;
    private MaterialCardView navCampaign;
    private MaterialCardView navInventory;
    private MaterialCardView navNotifications;

    // Game resources
    private int cursedEnergy = 0;
    private boolean characterUnlocked = false;

    // Data persistence
    private GameDataManager gameDataManager;

    // Current user name
    private String currentUsername;

    public static final String EXTRA_USERNAME = "extra_username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kaisen_main);

        // Leer el usuario actual del Hub (puede ser null si se abre standalone)
        currentUsername = getIntent().getStringExtra(EXTRA_USERNAME);

        // Inicializar sistema de guardado con datos aislados por usuario
        gameDataManager = new GameDataManager(this, currentUsername);

        // Debug helper: export DB and show repo diag in toast (only in debug builds)
        if (isDebugBuild()) {
            try {
                performDebugExportAndToast();
            } catch (Exception e) {
                Toast.makeText(this, "Debug check failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        // Cargar datos guardados
        loadGameData();

        initializeViews();
        setupClickListeners();

        // Mostrar Campaign por defecto al iniciar
        if (savedInstanceState == null) {
            showCampaign();
        }

        // Seleccionar Campaign por defecto
        selectNavItem(navCampaign);
    }

    private void loadGameData() {
        cursedEnergy = gameDataManager.getCursedEnergy();
        characterUnlocked = gameDataManager.isCharacterUnlocked();
    }

    private void initializeViews() {
        navShop = findViewById(R.id.nav_item_1);
        navChest = findViewById(R.id.nav_item_2);
        navCampaign = findViewById(R.id.nav_item_center);
        navInventory = findViewById(R.id.nav_item_4);
        navNotifications = findViewById(R.id.nav_item_5);
    }

    private void setupClickListeners() {
        navShop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectNavItem(navShop);
                openShop();
            }
        });

        navChest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectNavItem(navChest);
                openChest();
            }
        });

        navCampaign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectNavItem(navCampaign);
                showCampaign();
            }
        });

        navInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectNavItem(navInventory);
                openCharacterInventory();
            }
        });

        navNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectNavItem(navNotifications);
                // Abrir el fragmento de Estadísticas
                com.example.kaisenclicker.ui.fragments.StatisticsFragment stats = new com.example.kaisenclicker.ui.fragments.StatisticsFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, stats);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
    }

    private void selectNavItem(MaterialCardView selectedItem) {
        MaterialCardView[] allItems = {navShop, navChest, navCampaign, navInventory, navNotifications};

        float density = getResources().getDisplayMetrics().density;
        int normalSize = (int) (60 * density);
        int selectedSize = (int) (72 * density);

        for (MaterialCardView item : allItems) {
            if (item == null) continue;
            item.setSelected(false);

            // Restore normal size
            android.view.ViewGroup.LayoutParams lp = item.getLayoutParams();
            lp.width = normalSize;
            lp.height = normalSize;
            item.setLayoutParams(lp);
            item.setCardElevation(8 * density);
            item.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFF4A5F8E));
            item.setRadius(normalSize / 2f);

            // Animate back to normal scale
            item.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
        }

        // Highlight selected
        selectedItem.setSelected(true);
        android.view.ViewGroup.LayoutParams lp = selectedItem.getLayoutParams();
        lp.width = selectedSize;
        lp.height = selectedSize;
        selectedItem.setLayoutParams(lp);
        selectedItem.setCardElevation(12 * density);
        selectedItem.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFFFB74D));
        selectedItem.setRadius(selectedSize / 2f);

        // Animate pop effect
        selectedItem.setScaleX(0.9f);
        selectedItem.setScaleY(0.9f);
        selectedItem.animate().scaleX(1f).scaleY(1f).setDuration(250).setInterpolator(new android.view.animation.OvershootInterpolator(1.5f)).start();
    }

    private void openCharacterInventory() {
        CharacterInventoryFragment fragment = new CharacterInventoryFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void openShop() {
        ShopFragment fragment = new ShopFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void openChest() {
        ChestFragment fragment = new ChestFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void showCampaign() {
        // Cierra todos los fragments y muestra el Campaign
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

        CampaignFragment fragment = new CampaignFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void closeAllFragments() {
        // Cierra todos los fragments y vuelve a la pantalla principal
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    // Getters y setters para los recursos del juego
    public int getCursedEnergy() {
        return cursedEnergy;
    }

    public void addCursedEnergy(int amount) {
        cursedEnergy += amount;
        gameDataManager.saveCursedEnergy(cursedEnergy);
    }

    public boolean isCharacterUnlocked() {
        return characterUnlocked;
    }

    public void unlockCharacter() {
        characterUnlocked = true;
        gameDataManager.saveCharacterUnlocked(true);
    }

    public GameDataManager getGameDataManager() {
        return gameDataManager;
    }

    /**
     * Devuelve el nombre de usuario actual (puede ser null si se abrió standalone).
     */
    public String getCurrentUsername() {
        return currentUsername;
    }

    private void performDebugExportAndToast() {
        if (gameDataManager == null) return;
        String diag = "<no-repo>";
        try {
            if (gameDataManager.getRepository() != null) {
                diag = gameDataManager.getRepository().getString("diag_test", "<null>");
            }
        } catch (Exception e) {
            diag = "<err>" + e.getMessage();
        }
        // Build a richer debug message showing prefs vs repo values for key stats
        StringBuilder sb = new StringBuilder();
        sb.append("Repo diag: ").append(diag).append("\n");
        try {
            int prefsClicks = 0; long prefsDmg = 0L; int prefsEnemy = 1;
            prefsClicks = gameDataManager.getTotalClicks();
            prefsDmg = (long) gameDataManager.getTotalDamage();
            prefsEnemy = gameDataManager.getEnemyLevel();
            sb.append("From GDM: clicks=").append(prefsClicks).append(" dmg=").append(prefsDmg).append(" enemy=").append(prefsEnemy).append("\n");
        } catch (Exception ignored) {}
        try {
            if (gameDataManager.getRepository() != null) {
                sb.append("Repo kv: clicks=")
                    .append(gameDataManager.getRepository().getInt("total_clicks", -1))
                    .append(" dmg=")
                    .append(gameDataManager.getRepository().getLong("total_damage", -1L))
                    .append(" enemy=")
                    .append(gameDataManager.getRepository().getEnemyLevel())
                    .append("\n");
            }
        } catch (Exception ignored) {}

        // En modo debug, intentar forzar la migración de prefs -> SQLite ahora y mostrar el resultado
        try {
            boolean migratedNow = false;
            if (gameDataManager != null) {
                migratedNow = gameDataManager.forceMigratePrefsToSqlNow();
            }
            sb.append("Force migration result: ").append(migratedNow).append('\n');
            if (migratedNow && gameDataManager.getRepository() != null) {
                sb.append("Repo after migration: clicks=")
                    .append(gameDataManager.getRepository().getInt("total_clicks", -1))
                    .append(" dmg=")
                    .append(gameDataManager.getRepository().getLong("total_damage", -1L))
                    .append(" enemy=")
                    .append(gameDataManager.getRepository().getEnemyLevel())
                    .append('\n');
            }
        } catch (Exception ignored) {}

        Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();

        // Export DB file to external files dir so you can pull it without run-as
        File dbFile = getDatabasePath(AppDatabaseHelper.DATABASE_NAME);
        File out = new File(getExternalFilesDir(null), "kaisen_clicker_dump.db");
        try {
            copyFile(dbFile, out);
            Toast.makeText(this, "DB exported to: " + out.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "DB export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        if (src == null || !src.exists()) throw new IOException("Source DB not found: " + src);
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try (FileInputStream inStream = new FileInputStream(src); FileOutputStream outStream = new FileOutputStream(dst)) {
            inChannel = inStream.getChannel();
            outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    private boolean isDebugBuild() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    // Exponer públicamente para que fragments puedan saber si estamos en build debug
    public boolean isDebugBuildPublic() {
        return isDebugBuild();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemNavigationBar();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemNavigationBar();
        }
    }

    private void hideSystemNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

}
