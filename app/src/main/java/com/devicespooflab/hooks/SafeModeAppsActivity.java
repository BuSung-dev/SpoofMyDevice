package com.devicespooflab.hooks;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.devicespooflab.hooks.data.AppSettingsStore;
import com.devicespooflab.hooks.databinding.ActivitySafeModeAppsBinding;
import com.devicespooflab.hooks.databinding.ItemSafeModeAppBinding;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SafeModeAppsActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_PACKAGES = "selected_packages";
    public static final String EXTRA_RESULT_SELECTED_PACKAGES = "result_selected_packages";
    private static final String STATE_HIDE_SYSTEM_APPS = "hide_system_apps";

    private ActivitySafeModeAppsBinding binding;
    private final LinkedHashSet<String> selectedPackages = new LinkedHashSet<>();
    private final ArrayList<AppEntry> allApps = new ArrayList<>();
    private final ArrayList<AppEntry> visibleApps = new ArrayList<>();
    private SafeModeAppsAdapter adapter;
    private boolean hideSystemApps;

    public static Intent createIntent(Context context, Set<String> selectedPackages) {
        Intent intent = new Intent(context, SafeModeAppsActivity.class);
        intent.putStringArrayListExtra(
            EXTRA_SELECTED_PACKAGES,
            new ArrayList<>(selectedPackages == null ? Collections.emptySet() : selectedPackages)
        );
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppSettingsStore.applyActivityTheme(this);
        AppSettingsStore.apply(this);
        super.onCreate(savedInstanceState);
        binding = ActivitySafeModeAppsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.topAppBar);
        configureTopBarAppearance();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings_safe_mode_title);
        }

        ArrayList<String> incoming = getIntent().getStringArrayListExtra(EXTRA_SELECTED_PACKAGES);
        if (incoming != null) {
            selectedPackages.addAll(incoming);
        }
        hideSystemApps = savedInstanceState == null
            ? true
            : savedInstanceState.getBoolean(STATE_HIDE_SYSTEM_APPS, true);

        adapter = new SafeModeAppsAdapter();
        binding.appsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.appsRecyclerView.setAdapter(adapter);
        binding.topAppBar.setNavigationOnClickListener(v -> finish());

        binding.appsRecyclerView.post(() -> loadAppsAsync());
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

        binding.topAppBarLayout.setBackgroundColor(topBarColor);
        binding.topAppBarLayout.setLiftOnScroll(false);
        binding.topAppBar.setElevation(0f);
        binding.topAppBar.setBackgroundColor(topBarColor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.safe_mode_apps_menu, menu);
        MenuItem hideSystemAppsItem = menu.findItem(R.id.action_hide_system_apps);
        if (hideSystemAppsItem != null) {
            hideSystemAppsItem.setChecked(hideSystemApps);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_hide_system_apps) {
            hideSystemApps = !item.isChecked();
            item.setChecked(hideSystemApps);
            applyVisibleApps(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_HIDE_SYSTEM_APPS, hideSystemApps);
    }

    private void loadAppsAsync() {
        binding.progressIndicator.show();
        binding.appsRecyclerView.setAlpha(0f);
        new Thread(() -> {
            List<AppEntry> loadedApps = queryInstalledApps();
            runOnUiThread(() -> {
                allApps.clear();
                allApps.addAll(loadedApps);
                applyVisibleApps(true);
                binding.progressIndicator.hide();
                binding.appsRecyclerView.animate().alpha(1f).setDuration(120L).start();
            });
        }, "spoofmydevice-safe-mode-loader").start();
    }

    private void applyVisibleApps(boolean prioritizeSelected) {
        visibleApps.clear();
        for (AppEntry appEntry : allApps) {
            if (hideSystemApps && appEntry.isSystemApp && !selectedPackages.contains(appEntry.packageName)) {
                continue;
            }
            visibleApps.add(appEntry);
        }
        if (prioritizeSelected) {
            visibleApps.sort(Comparator
                .comparing((AppEntry entry) -> !selectedPackages.contains(entry.packageName))
                .thenComparing(entry -> entry.label.toLowerCase(Locale.getDefault()))
                .thenComparing(entry -> entry.packageName.toLowerCase(Locale.US)));
        } else {
            visibleApps.sort(Comparator
                .comparing((AppEntry entry) -> entry.label.toLowerCase(Locale.getDefault()))
                .thenComparing(entry -> entry.packageName.toLowerCase(Locale.US)));
        }
        adapter.notifyDataSetChanged();
        binding.emptyText.setVisibility(visibleApps.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private List<AppEntry> queryInstalledApps() {
        ArrayList<AppEntry> result = new ArrayList<>();
        PackageManager packageManager = getPackageManager();

        addFrameworkEntry(result, packageManager);

        List<PackageInfo> installedPackages;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                installedPackages = packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0));
            } else {
                installedPackages = packageManager.getInstalledPackages(0);
            }
        } catch (Throwable throwable) {
            return result;
        }

        for (PackageInfo packageInfo : installedPackages) {
            if (packageInfo.applicationInfo == null) {
                continue;
            }

            String packageName = packageInfo.packageName;
            if (packageName == null || packageName.trim().isEmpty() || "android".equals(packageName)) {
                continue;
            }

            CharSequence loadedLabel = packageInfo.applicationInfo.loadLabel(packageManager);
            String label = loadedLabel == null ? packageName : loadedLabel.toString().trim();
            if (label.isEmpty()) {
                label = packageName;
            }

            String versionName = packageInfo.versionName == null ? "" : packageInfo.versionName.trim();
            result.add(new AppEntry(
                label,
                packageName,
                versionName,
                packageInfo.applicationInfo.loadIcon(packageManager),
                isSystemPackage(packageInfo.applicationInfo)
            ));
        }

        result.sort(Comparator
            .comparing((AppEntry entry) -> entry.label.toLowerCase(Locale.getDefault()))
            .thenComparing(entry -> entry.packageName.toLowerCase(Locale.US)));
        return result;
    }

    private void addFrameworkEntry(List<AppEntry> result, PackageManager packageManager) {
        try {
            PackageInfo frameworkInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                frameworkInfo = packageManager.getPackageInfo("android", PackageManager.PackageInfoFlags.of(0));
            } else {
                frameworkInfo = packageManager.getPackageInfo("android", 0);
            }
            String versionName = frameworkInfo.versionName == null ? String.valueOf(Build.VERSION.SDK_INT) : frameworkInfo.versionName;
            result.add(new AppEntry(
                getString(R.string.safe_mode_system_framework),
                "android",
                versionName,
                ContextCompat.getDrawable(this, R.drawable.ic_settings_resolution),
                true
            ));
        } catch (Throwable ignored) {
        }
    }

    private boolean isSystemPackage(ApplicationInfo applicationInfo) {
        if (applicationInfo == null) {
            return false;
        }
        int flags = applicationInfo.flags;
        return (flags & ApplicationInfo.FLAG_SYSTEM) != 0
            || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putStringArrayListExtra(EXTRA_RESULT_SELECTED_PACKAGES, new ArrayList<>(selectedPackages));
        setResult(RESULT_OK, data);
        super.finish();
    }

    private final class SafeModeAppsAdapter extends RecyclerView.Adapter<SafeModeAppsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSafeModeAppBinding itemBinding = ItemSafeModeAppBinding.inflate(
                getLayoutInflater(),
                parent,
                false
            );
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(visibleApps.get(position));
        }

        @Override
        public int getItemCount() {
            return visibleApps.size();
        }

        final class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemSafeModeAppBinding itemBinding;

            ViewHolder(ItemSafeModeAppBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }

            void bind(AppEntry appEntry) {
                itemBinding.appIcon.setImageDrawable(appEntry.icon);
                itemBinding.appLabel.setText(appEntry.label);
                itemBinding.appPackage.setText(appEntry.packageName);
                itemBinding.appVersion.setText(
                    appEntry.versionName.isEmpty()
                        ? getString(R.string.safe_mode_version_unknown)
                        : getString(R.string.safe_mode_version_format, appEntry.versionName)
                );

                boolean checked = selectedPackages.contains(appEntry.packageName);
                itemBinding.appCheckbox.setOnCheckedChangeListener(null);
                itemBinding.appCheckbox.setChecked(checked);
                itemBinding.getRoot().setOnClickListener(v -> toggle(appEntry));
                itemBinding.appCheckbox.setOnClickListener(v -> toggle(appEntry));
            }
        }
    }

    private void toggle(AppEntry appEntry) {
        if (appEntry == null) {
            return;
        }
        int position = visibleApps.indexOf(appEntry);
        boolean wasSelected = selectedPackages.contains(appEntry.packageName);
        if (wasSelected) {
            selectedPackages.remove(appEntry.packageName);
        } else {
            selectedPackages.add(appEntry.packageName);
        }

        if (hideSystemApps && appEntry.isSystemApp && wasSelected) {
            if (position >= 0) {
                visibleApps.remove(position);
                adapter.notifyItemRemoved(position);
            } else {
                applyVisibleApps(false);
            }
            binding.emptyText.setVisibility(visibleApps.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        if (position >= 0) {
            adapter.notifyItemChanged(position);
        } else {
            applyVisibleApps(false);
        }
    }

    private static final class AppEntry {
        private final String label;
        private final String packageName;
        private final String versionName;
        private final android.graphics.drawable.Drawable icon;
        private final boolean isSystemApp;

        AppEntry(String label, String packageName, String versionName, android.graphics.drawable.Drawable icon, boolean isSystemApp) {
            this.label = label;
            this.packageName = packageName;
            this.versionName = versionName;
            this.icon = icon;
            this.isSystemApp = isSystemApp;
        }
    }
}
