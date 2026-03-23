package com.devicespooflab.hooks.ui;

import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.devicespooflab.hooks.MainActivity;
import com.devicespooflab.hooks.data.ConfigFileManager;
import com.devicespooflab.hooks.data.DeviceProfile;
import com.devicespooflab.hooks.databinding.FragmentRealInfoBinding;

import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

public class RealInfoFragment extends Fragment {

    private FragmentRealInfoBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRealInfoBinding.inflate(inflater, container, false);
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
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        binding.realBuildText.setText(String.format(
            Locale.US,
            "Brand: %s\nManufacturer: %s\nModel: %s\nDevice: %s\nBoard: %s\nHardware: %s\nFingerprint: %s\nRelease: %s\nSDK: %d\nSecurity patch: %s",
            Build.BRAND,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.DEVICE,
            Build.BOARD,
            Build.HARDWARE,
            Build.FINGERPRINT,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
            Build.VERSION.SECURITY_PATCH
        ));

        binding.realRuntimeText.setText(String.format(
            Locale.US,
            "Resolution: %dx%d\nDensity: %ddpi\nABIs: %s\nTimezone: %s",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            Arrays.toString(Build.SUPPORTED_ABIS),
            TimeZone.getDefault().getID()
        ));

        binding.savedProfileText.setText(String.format(
            Locale.US,
            "Preset: %s\nMode: %s\nTarget model: %s\nTarget device: %s\nTarget fingerprint: %s\nTarget resolution: %dx%d / %ddpi",
            activity.getPresetLabel(config.getSelectedPresetId()),
            config.isCustomMode() ? "Custom" : "Preset",
            profile.getDisplayName(),
            profile.getDeviceCode(),
            profile.getBuildFingerprint(),
            profile.getScreenWidth(),
            profile.getScreenHeight(),
            profile.getScreenDensity()
        ));
    }
}
