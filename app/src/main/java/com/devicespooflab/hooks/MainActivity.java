package com.devicespooflab.hooks;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.devicespooflab.hooks.data.AppSettingsStore;
import com.devicespooflab.hooks.data.ConfigFileManager;
import com.devicespooflab.hooks.data.DevicePreset;
import com.devicespooflab.hooks.data.DevicePresetCatalog;
import com.devicespooflab.hooks.databinding.ActivityMainBinding;
import com.devicespooflab.hooks.ui.AppSettingsFragment;
import com.devicespooflab.hooks.ui.DeviceSettingsFragment;
import com.devicespooflab.hooks.ui.HomeFragment;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_HOME = "home";
    private static final String TAG_DEVICE_SETTINGS = "device_settings";
    private static final String TAG_APP_SETTINGS = "app_settings";
    private static final String STATE_SELECTED_TAB = "selected_tab";

    private ActivityMainBinding binding;
    private ConfigFileManager configFileManager;
    private List<DevicePreset> presets;
    private ConfigFileManager.LoadedConfig loadedConfig;

    private HomeFragment homeFragment;
    private DeviceSettingsFragment settingsFragment;
    private AppSettingsFragment appSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettingsStore.applyActivityTheme(this);
        AppSettingsStore.apply(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.topAppBar);
        configureTopBarAppearance();
        configureBottomNavigationAppearance();

        configFileManager = new ConfigFileManager();
        presets = new DevicePresetCatalog().load(this);
        loadedConfig = loadInitialConfig();

        setupFragments(savedInstanceState);
        setupBottomNavigation(savedInstanceState);
        binding.saveFab.setOnClickListener(view -> saveFromEditor());
    }

    public List<DevicePreset> getPresets() {
        return presets;
    }

    public ConfigFileManager.LoadedConfig getLoadedConfigState() {
        return loadedConfig;
    }

    public String getPresetLabel(String presetId) {
        if (presetId == null) {
            return getString(R.string.preset_unknown);
        }
        for (DevicePreset preset : presets) {
            if (presetId.equals(preset.getId())) {
                return preset.getDisplayName();
            }
        }
        return getString(R.string.preset_unknown);
    }

    public boolean isModuleActivated() {
        return false;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, binding.bottomNavigation.getSelectedItemId());
    }

    private void configureBottomNavigationAppearance() {
        int backgroundColor = MaterialColors.getColor(
            binding.bottomNavigation,
            android.R.attr.colorBackground
        );
        int onBackgroundColor = MaterialColors.getColor(
            binding.bottomNavigation,
            com.google.android.material.R.attr.colorOnBackground
        );
        int navigationBarColor = ColorUtils.blendARGB(backgroundColor, onBackgroundColor, 0.08f);
        int indicatorColor = ColorUtils.blendARGB(backgroundColor, onBackgroundColor, 0.18f);

        binding.bottomNavigation.setElevation(0f);
        binding.bottomNavigation.setBackgroundTintList(ColorStateList.valueOf(navigationBarColor));
        binding.bottomNavigation.setItemActiveIndicatorColor(ColorStateList.valueOf(indicatorColor));
    }

    private void configureTopBarAppearance() {
        int backgroundColor = MaterialColors.getColor(
            binding.topAppBar,
            android.R.attr.colorBackground
        );
        int onBackgroundColor = MaterialColors.getColor(
            binding.topAppBar,
            com.google.android.material.R.attr.colorOnBackground
        );
        int topBarColor = ColorUtils.blendARGB(backgroundColor, onBackgroundColor, 0.08f);

        binding.topAppBarLayout.setBackgroundTintList(ColorStateList.valueOf(topBarColor));
        binding.topAppBarLayout.setLiftOnScroll(false);
        binding.topAppBar.setElevation(0f);
        binding.topAppBar.setBackgroundTintList(ColorStateList.valueOf(topBarColor));
    }

    private ConfigFileManager.LoadedConfig loadInitialConfig() {
        try {
            return configFileManager.ensureLoaded(this, presets);
        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
            try {
                DevicePreset fallback = presets.get(0);
                return configFileManager.save(
                    this,
                    fallback.getProfile(),
                    null,
                    fallback.getId(),
                    false
                );
            } catch (Exception innerException) {
                throw new IllegalStateException("Unable to initialize configuration", innerException);
            }
        }
    }

    private void setupFragments(Bundle savedInstanceState) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        homeFragment = (HomeFragment) fragmentManager.findFragmentByTag(TAG_HOME);
        settingsFragment = (DeviceSettingsFragment) fragmentManager.findFragmentByTag(TAG_DEVICE_SETTINGS);
        appSettingsFragment = (AppSettingsFragment) fragmentManager.findFragmentByTag(TAG_APP_SETTINGS);

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
            settingsFragment = new DeviceSettingsFragment();
            appSettingsFragment = new AppSettingsFragment();

            fragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment, TAG_HOME)
                .add(R.id.fragment_container, settingsFragment, TAG_DEVICE_SETTINGS)
                .hide(settingsFragment)
                .add(R.id.fragment_container, appSettingsFragment, TAG_APP_SETTINGS)
                .hide(appSettingsFragment)
                .commitNow();
        }
    }

    private void setupBottomNavigation(Bundle savedInstanceState) {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            switchTab(item.getItemId());
            return true;
        });

        int initialTab = savedInstanceState == null
            ? R.id.navigation_home
            : savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.navigation_home);
        binding.bottomNavigation.setSelectedItemId(initialTab);
        switchTab(initialTab);
    }

    private void switchTab(int itemId) {
        Fragment target;
        String title;
        boolean showSave;

        if (itemId == R.id.navigation_settings) {
            target = settingsFragment;
            title = getString(R.string.toolbar_settings);
            showSave = true;
            settingsFragment.refreshFromHost(false);
        } else if (itemId == R.id.navigation_app_settings) {
            target = appSettingsFragment;
            title = getString(R.string.toolbar_app_settings);
            showSave = false;
            appSettingsFragment.refreshFromHost();
        } else {
            target = homeFragment;
            title = getString(R.string.toolbar_home);
            showSave = false;
            homeFragment.refresh();
        }

        getSupportFragmentManager().beginTransaction()
            .hide(homeFragment)
            .hide(settingsFragment)
            .hide(appSettingsFragment)
            .show(target)
            .commit();

        binding.topAppBar.setTitle(title);
        binding.saveFab.setVisibility(showSave ? View.VISIBLE : View.GONE);
    }

    private void saveFromEditor() {
        DeviceSettingsFragment.Draft draft = settingsFragment.buildDraft();
        if (draft == null) {
            return;
        }

        try {
            loadedConfig = configFileManager.save(
                this,
                draft.profile,
                loadedConfig.getExtraProperties(),
                draft.selectedPresetId,
                draft.customMode
            );
            settingsFragment.refreshFromHost(true);
            homeFragment.refresh();
            appSettingsFragment.refreshFromHost();
            Snackbar.make(binding.getRoot(), R.string.save_success, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.saveFab)
                .show();
        } catch (Exception exception) {
            Snackbar.make(binding.getRoot(), getString(R.string.save_failed) + " " + exception.getMessage(), Snackbar.LENGTH_LONG)
                .setAnchorView(binding.saveFab)
                .show();
        }
    }

    public void openRealInfo() {
        startActivity(new Intent(this, RealInfoActivity.class));
    }

    public boolean isScreenMetricsSpoofEnabled() {
        Map<String, String> extraProperties = loadedConfig.getExtraProperties();
        String value = extraProperties.get(ConfigManager.KEY_APPLY_SCREEN_METRICS);
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    public boolean updateScreenMetricsSpoofEnabled(boolean enabled) {
        try {
            Map<String, String> extraProperties = new LinkedHashMap<>(loadedConfig.getExtraProperties());
            extraProperties.put(ConfigManager.KEY_APPLY_SCREEN_METRICS, Boolean.toString(enabled));
            loadedConfig = configFileManager.save(
                this,
                loadedConfig.getProfile(),
                extraProperties,
                loadedConfig.getSelectedPresetId(),
                loadedConfig.isCustomMode()
            );
            if (homeFragment != null) {
                homeFragment.refresh();
            }
            Snackbar.make(binding.getRoot(), R.string.settings_saved, Snackbar.LENGTH_SHORT).show();
            return true;
        } catch (Exception exception) {
            Snackbar.make(binding.getRoot(), getString(R.string.save_failed) + " " + exception.getMessage(), Snackbar.LENGTH_LONG).show();
            return false;
        }
    }
}
