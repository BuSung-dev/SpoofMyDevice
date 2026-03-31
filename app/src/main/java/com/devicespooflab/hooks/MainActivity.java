package com.devicespooflab.hooks;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.devicespooflab.hooks.data.AppSettingsStore;
import com.devicespooflab.hooks.data.ConfigFileManager;
import com.devicespooflab.hooks.data.DevicePreset;
import com.devicespooflab.hooks.data.DevicePresetCatalog;
import com.devicespooflab.hooks.databinding.ActivityMainBinding;
import com.devicespooflab.hooks.ui.AppSettingsFragment;
import com.devicespooflab.hooks.ui.DeviceSettingsFragment;
import com.devicespooflab.hooks.ui.HomeFragment;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.Snackbar;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_HOME = "home";
    private static final String TAG_DEVICE_SETTINGS = "device_settings";
    private static final String TAG_APP_SETTINGS = "app_settings";
    private static final String STATE_SELECTED_TAB = "selected_tab";

    private ActivityMainBinding binding;
    private ConfigFileManager configFileManager;
    private DevicePresetCatalog presetCatalog;
    private List<DevicePreset> presets;
    private ConfigFileManager.LoadedConfig loadedConfig;

    private HomeFragment homeFragment;
    private DeviceSettingsFragment settingsFragment;
    private AppSettingsFragment appSettingsFragment;
    private boolean tabletNavigation;
    private boolean syncingNavigationSelection;
    private int selectedNavigationItemId = R.id.navigation_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettingsStore.applyActivityTheme(this);
        AppSettingsStore.apply(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        tabletNavigation = isTabletNavigation();
        configureNavigationMode();
        configureNavigationAppearance();

        configFileManager = new ConfigFileManager();
        presetCatalog = new DevicePresetCatalog();
        presets = presetCatalog.load(this);
        loadedConfig = loadInitialConfig();

        setupFragments(savedInstanceState);
        setupBottomNavigation(savedInstanceState);
        binding.saveFab.setOnClickListener(view -> saveFromEditor());
        refreshRemotePresets(false);
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
        outState.putInt(STATE_SELECTED_TAB, selectedNavigationItemId);
    }

    private void configureNavigationMode() {
        binding.navigationRail.setVisibility(tabletNavigation ? View.VISIBLE : View.GONE);
        binding.bottomNavigation.setVisibility(tabletNavigation ? View.GONE : View.VISIBLE);
        binding.navigationRail.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_SELECTED);
        binding.bottomNavigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_SELECTED);
        updateSaveFabPosition();
    }

    private void configureNavigationAppearance() {
        int navigationBarColor = resolveChromeColor();
        int indicatorColor = resolveIndicatorColor();
        int railIndicatorColor = resolveIndicatorColor();

        binding.bottomNavigation.setElevation(0f);
        binding.bottomNavigation.setBackgroundTintList(ColorStateList.valueOf(navigationBarColor));
        binding.bottomNavigation.setItemActiveIndicatorColor(ColorStateList.valueOf(indicatorColor));
        binding.navigationRail.setElevation(0f);
        binding.navigationRail.setBackgroundColor(Color.TRANSPARENT);
        binding.navigationRail.setItemActiveIndicatorColor(ColorStateList.valueOf(railIndicatorColor));
    }

    private ConfigFileManager.LoadedConfig loadInitialConfig() {
        try {
            return configFileManager.ensureLoaded(this, presets);
        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
            throw new IllegalStateException("Unable to initialize configuration", exception);
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
        applyFragmentHeaderChrome(homeFragment);
        applyFragmentHeaderChrome(settingsFragment);
        applyFragmentHeaderChrome(appSettingsFragment);
    }

    private void setupBottomNavigation(Bundle savedInstanceState) {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (syncingNavigationSelection) {
                return true;
            }
            handleNavigationSelection(item.getItemId());
            return true;
        });
        binding.navigationRail.setOnItemSelectedListener(item -> {
            if (syncingNavigationSelection) {
                return true;
            }
            handleNavigationSelection(item.getItemId());
            return true;
        });

        selectedNavigationItemId = savedInstanceState == null
            ? R.id.navigation_home
            : savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.navigation_home);
        syncNavigationSelection();
        switchTab(selectedNavigationItemId);
    }

    private void handleNavigationSelection(int itemId) {
        selectedNavigationItemId = itemId;
        syncNavigationSelection();
        switchTab(itemId);
    }

    private void syncNavigationSelection() {
        syncingNavigationSelection = true;
        if (binding.bottomNavigation.getSelectedItemId() != selectedNavigationItemId) {
            binding.bottomNavigation.setSelectedItemId(selectedNavigationItemId);
        }
        if (binding.navigationRail.getSelectedItemId() != selectedNavigationItemId) {
            binding.navigationRail.setSelectedItemId(selectedNavigationItemId);
        }
        syncingNavigationSelection = false;
    }

    private void switchTab(int itemId) {
        Fragment target;
        boolean showSave;

        if (itemId == R.id.navigation_settings) {
            target = settingsFragment;
            showSave = true;
            settingsFragment.refreshFromHost(false);
        } else if (itemId == R.id.navigation_app_settings) {
            target = appSettingsFragment;
            showSave = false;
            appSettingsFragment.refreshFromHost();
        } else {
            target = homeFragment;
            showSave = false;
            homeFragment.refresh();
        }

        getSupportFragmentManager().beginTransaction()
            .hide(homeFragment)
            .hide(settingsFragment)
            .hide(appSettingsFragment)
            .show(target)
            .commitNow();

        binding.saveFab.setVisibility(showSave ? View.VISIBLE : View.GONE);
        applyFragmentHeaderChrome(target);
        binding.fragmentContainer.post(() -> scrollFragmentToTop(target));
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
                draft.extraProperties,
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

    public String getPresetSourceUrl() {
        return AppSettingsStore.getPresetSourceUrl(this);
    }

    public boolean updatePresetSourceUrl(String value) {
        try {
            AppSettingsStore.setPresetSourceUrl(this, value);
            refreshRemotePresets(true);
            return true;
        } catch (Exception exception) {
            Snackbar.make(binding.getRoot(), getString(R.string.settings_preset_source_failed) + " " + exception.getMessage(), Snackbar.LENGTH_LONG).show();
            return false;
        }
    }

    public Set<String> getSafeModePackages() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String raw = loadedConfig.getExtraProperties().get(ConfigManager.KEY_SAFE_MODE_PACKAGES);
        if (raw == null || raw.trim().isEmpty()) {
            return result;
        }
        String[] parts = raw.split("[,\\n]");
        for (String part : parts) {
            String normalized = part.trim();
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return result;
    }

    public boolean updateSafeModePackages(Set<String> packageNames) {
        try {
            Map<String, String> extraProperties = new LinkedHashMap<>(loadedConfig.getExtraProperties());
            if (packageNames == null || packageNames.isEmpty()) {
                extraProperties.remove(ConfigManager.KEY_SAFE_MODE_PACKAGES);
            } else {
                extraProperties.put(
                    ConfigManager.KEY_SAFE_MODE_PACKAGES,
                    String.join(",", new LinkedHashSet<>(packageNames))
                );
            }
            loadedConfig = configFileManager.save(
                this,
                loadedConfig.getProfile(),
                extraProperties,
                loadedConfig.getSelectedPresetId(),
                loadedConfig.isCustomMode()
            );
            settingsFragment.refreshFromHost(true);
            appSettingsFragment.refreshFromHost();
            return true;
        } catch (Exception exception) {
            Snackbar.make(binding.getRoot(), getString(R.string.settings_safe_mode_failed) + " " + exception.getMessage(), Snackbar.LENGTH_LONG).show();
            return false;
        }
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

    private boolean isTabletNavigation() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    private void updateSaveFabPosition() {
        ViewGroup.LayoutParams rawLayoutParams = binding.saveFab.getLayoutParams();
        if (!(rawLayoutParams instanceof CoordinatorLayout.LayoutParams)) {
            return;
        }
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) rawLayoutParams;
        int density = getResources().getDisplayMetrics().densityDpi;
        int marginEnd = Math.round(20f * density / 160f);
        int marginBottom = Math.round((tabletNavigation ? 24f : 92f) * density / 160f);
        layoutParams.setMarginEnd(marginEnd);
        layoutParams.bottomMargin = marginBottom;
        binding.saveFab.setLayoutParams(layoutParams);
    }

    private void scrollFragmentToTop(Fragment fragment) {
        if (fragment == null || fragment.getView() == null) {
            return;
        }
        expandFirstAppBar(fragment.getView());
        scrollViewToTop(fragment.getView());
    }

    private void expandFirstAppBar(View root) {
        if (root instanceof AppBarLayout) {
            ((AppBarLayout) root).setExpanded(true, false);
            return;
        }
        if (!(root instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof AppBarLayout) {
                ((AppBarLayout) child).setExpanded(true, false);
                return;
            }
            expandFirstAppBar(child);
        }
    }

    private boolean scrollViewToTop(View root) {
        if (root instanceof NestedScrollView) {
            ((NestedScrollView) root).scrollTo(0, 0);
            return true;
        }
        if (root instanceof ScrollView) {
            ((ScrollView) root).scrollTo(0, 0);
            return true;
        }
        if (root instanceof RecyclerView) {
            ((RecyclerView) root).scrollToPosition(0);
            return true;
        }
        if (!(root instanceof ViewGroup)) {
            return false;
        }
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (scrollViewToTop(group.getChildAt(i))) {
                return true;
            }
        }
        return false;
    }

    private void applyFragmentHeaderChrome(Fragment fragment) {
        if (fragment == null || fragment.getView() == null) {
            return;
        }
        CollapsingToolbarLayout collapsingToolbarLayout =
            fragment.getView().findViewById(R.id.fragment_header_collapsing_toolbar);
        if (collapsingToolbarLayout != null) {
            collapsingToolbarLayout.setContentScrimColor(resolveChromeColor());
        }
    }

    private int resolveChromeColor() {
        return MaterialColors.getColor(
            binding.getRoot(),
            com.google.android.material.R.attr.colorSurfaceVariant
        );
    }

    private int resolveIndicatorColor() {
        int chromeColor = resolveChromeColor();
        int onSurfaceColor = MaterialColors.getColor(
            binding.getRoot(),
            com.google.android.material.R.attr.colorOnSurface
        );
        return ColorUtils.blendARGB(chromeColor, onSurfaceColor, 0.08f);
    }

    private void refreshRemotePresets(boolean userInitiated) {
        new Thread(() -> {
            List<DevicePreset> remotePresets = presetCatalog.refreshRemote(this, AppSettingsStore.getPresetSourceUrl(this));
            if (remotePresets == null || remotePresets.isEmpty()) {
                if (userInitiated) {
                    runOnUiThread(() ->
                        Snackbar.make(binding.getRoot(), R.string.settings_preset_source_empty, Snackbar.LENGTH_LONG).show()
                    );
                }
                return;
            }

            runOnUiThread(() -> {
                presets = remotePresets;
                if (settingsFragment != null) {
                    settingsFragment.refreshFromHost(true);
                }
                if (homeFragment != null) {
                    homeFragment.refresh();
                }
                if (appSettingsFragment != null) {
                    appSettingsFragment.refreshFromHost();
                }
                if (userInitiated) {
                    Snackbar.make(binding.getRoot(), R.string.settings_preset_source_updated, Snackbar.LENGTH_SHORT).show();
                }
            });
        }, "spoofmydevice-preset-sync").start();
    }
}
