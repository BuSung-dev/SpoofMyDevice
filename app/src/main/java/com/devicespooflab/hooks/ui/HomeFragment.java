package com.devicespooflab.hooks.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.devicespooflab.hooks.MainActivity;
import com.devicespooflab.hooks.R;
import com.devicespooflab.hooks.data.ConfigFileManager;
import com.devicespooflab.hooks.data.DeviceProfile;
import com.devicespooflab.hooks.databinding.FragmentHomeBinding;
import com.google.android.material.color.MaterialColors;

import java.io.File;
public class HomeFragment extends Fragment {

    private static final float STATUS_ICON_WIDTH_RATIO = 0.15f;
    private static final int STATUS_ICON_MIN_DP = 56;
    private static final int STATUS_ICON_MAX_DP = 88;

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void refresh() {
        if (binding == null || !(requireActivity() instanceof MainActivity)) {
            return;
        }

        MainActivity activity = (MainActivity) requireActivity();
        ConfigFileManager.LoadedConfig config = activity.getLoadedConfigState();
        DeviceProfile profile = config.getProfile();
        File configFile = config.getConfigFile();
        boolean moduleActivated = activity.isModuleActivated();

        int statusCardColor = moduleActivated
            ? ContextCompat.getColor(requireContext(), R.color.dsl_blue_primary_container)
            : MaterialColors.getColor(binding.moduleStatusCard, com.google.android.material.R.attr.colorSurfaceVariant);
        int statusForegroundColor = moduleActivated
            ? ContextCompat.getColor(requireContext(), R.color.dsl_blue_on_primary_container)
            : MaterialColors.getColor(binding.moduleStatusCard, com.google.android.material.R.attr.colorOnSurfaceVariant);
        binding.moduleStatusIcon.setImageTintList(ColorStateList.valueOf(statusForegroundColor));
        binding.moduleStatusCard.setCardBackgroundColor(statusCardColor);
        binding.moduleStatusCard.setStrokeWidth(0);
        binding.heroTitle.setTextColor(statusForegroundColor);
        binding.heroBody.setTextColor(statusForegroundColor);
        binding.moduleStatusIcon.setImageResource(moduleActivated ? R.drawable.ic_status_active : R.drawable.ic_status_inactive);
        binding.heroTitle.setText(getString(moduleActivated ? R.string.home_module_active_title : R.string.home_module_inactive_title));
        binding.heroBody.setText(getString(moduleActivated ? R.string.home_module_active_body : R.string.home_module_inactive_body));
        binding.profileCardContainer.setVisibility(moduleActivated ? View.VISIBLE : View.GONE);
        updateStatusIconSize();
        binding.profileModelValue.setText(profile.getModel());
        binding.profilePresetValue.setText(activity.getPresetLabel(config.getSelectedPresetId()));
        binding.profileModeValue.setText(getString(config.isCustomMode() ? R.string.mode_custom : R.string.mode_preset));
        binding.profileAndroidValue.setText(getString(
            R.string.home_info_android_value_format,
            profile.getBuildRelease(),
            profile.getBuildSdk()
        ));
        binding.profileDisplayValue.setText(getString(
            R.string.home_info_display_value_format,
            profile.getScreenWidth(),
            profile.getScreenHeight(),
            profile.getScreenDensity()
        ));
        binding.profileFingerprintValue.setText(profile.getBuildFingerprint());
        binding.configUpdatedValue.setText(formatTimestamp(configFile.lastModified()));
        binding.configPathValue.setText(configFile.getAbsolutePath());
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0L) {
            return getString(R.string.home_not_available);
        }
        return DateFormat.format("yyyy-MM-dd HH:mm", timestamp).toString();
    }

    private void updateStatusIconSize() {
        if (binding == null) {
            return;
        }

        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        int minSizePx = dpToPx(STATUS_ICON_MIN_DP);
        int maxSizePx = dpToPx(STATUS_ICON_MAX_DP);
        int targetSizePx = Math.round(screenWidthPx * STATUS_ICON_WIDTH_RATIO);
        int finalSizePx = Math.max(minSizePx, Math.min(maxSizePx, targetSizePx));

        ViewGroup.LayoutParams layoutParams = binding.moduleStatusIcon.getLayoutParams();
        layoutParams.width = finalSizePx;
        layoutParams.height = finalSizePx;
        binding.moduleStatusIcon.setLayoutParams(layoutParams);

        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) binding.moduleStatusIcon.getLayoutParams();
        marginLayoutParams.setMarginEnd(dpToPx(16));
        binding.moduleStatusIcon.setLayoutParams(marginLayoutParams);
    }

    private int dpToPx(int valueDp) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            valueDp,
            getResources().getDisplayMetrics()
        ));
    }
}
