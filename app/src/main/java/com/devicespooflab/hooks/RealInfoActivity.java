package com.devicespooflab.hooks;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.content.pm.PackageInfo;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;

import com.devicespooflab.hooks.data.AppSettingsStore;
import com.devicespooflab.hooks.databinding.ActivityRealInfoBinding;
import com.google.android.material.color.MaterialColors;

public class RealInfoActivity extends AppCompatActivity {

    private static final String GITHUB_URL = "https://github.com/BuSung-dev/SpoofMyDevice";

    private ActivityRealInfoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettingsStore.applyActivityTheme(this);
        AppSettingsStore.apply(this);
        super.onCreate(savedInstanceState);
        binding = ActivityRealInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.topAppBar);
        configureTopBarAppearance();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.toolbar_real_info);
        }
        binding.topAppBar.setNavigationOnClickListener(v -> finish());
        binding.openGithubRow.setOnClickListener(v -> openGithub());
        bindAppInfo();
    }

    private void bindAppInfo() {
        String versionName = "";
        long versionCode = 0L;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName == null ? "" : packageInfo.versionName;
            versionCode = packageInfo.getLongVersionCode();
        } catch (Exception ignored) {
        }

        binding.appIconImage.setImageDrawable(getApplicationInfo().loadIcon(getPackageManager()));
        binding.appNameText.setText(getString(R.string.app_name));
        binding.appVersionText.setText(getString(R.string.app_info_version_format, versionName, versionCode));
        binding.appDescriptionText.setText(getString(R.string.xposed_description));
        binding.appPackageText.setText(getPackageName());
        binding.appCreditsText.setText(getString(R.string.app_info_credits_body));
        binding.appLinksText.setText(getString(R.string.app_info_github_summary));
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

    private void openGithub() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)));
        } catch (Exception ignored) {
            Toast.makeText(this, R.string.app_info_link_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
