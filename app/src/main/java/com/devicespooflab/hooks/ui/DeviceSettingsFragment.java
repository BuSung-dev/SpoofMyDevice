package com.devicespooflab.hooks.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.devicespooflab.hooks.MainActivity;
import com.devicespooflab.hooks.R;
import com.devicespooflab.hooks.data.ConfigFileManager;
import com.devicespooflab.hooks.data.DevicePreset;
import com.devicespooflab.hooks.data.DeviceProfile;
import com.devicespooflab.hooks.databinding.FragmentDeviceSettingsBinding;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
public class DeviceSettingsFragment extends Fragment {

    private FragmentDeviceSettingsBinding binding;
    private final List<DevicePreset> presets = new ArrayList<>();
    private String selectedPresetId;
    private boolean customMode;
    private boolean initialized;
    private boolean applying;
    private DeviceProfile workingProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFromHost(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void refreshFromHost(boolean force) {
        if (binding == null || !(requireActivity() instanceof MainActivity)) {
            return;
        }
        if (initialized && !force) {
            return;
        }

        MainActivity activity = (MainActivity) requireActivity();
        presets.clear();
        presets.addAll(activity.getPresets());
        ConfigFileManager.LoadedConfig loadedConfig = activity.getLoadedConfigState();
        selectedPresetId = loadedConfig.getSelectedPresetId();
        customMode = loadedConfig.isCustomMode();
        workingProfile = loadedConfig.getProfile();

        setupPresetDropdown();
        bindProfile(workingProfile);
        applyMode(customMode, false);
        updatePresetSummary();
        initialized = true;
    }

    @Nullable
    public Draft buildDraft() {
        if (binding == null) {
            return null;
        }

        DeviceProfile draft = workingProfile == null ? new DeviceProfile() : workingProfile.copy();
        draft.setBrand(text(binding.inputBrand));
        draft.setManufacturer(text(binding.inputManufacturer));
        draft.setModel(text(binding.inputModel));
        draft.setDeviceCode(text(binding.inputDevice));
        draft.setProductName(text(binding.inputProduct));
        draft.setBoard(text(binding.inputBoard));
        draft.setHardware(text(binding.inputHardware));
        draft.setBoardPlatform(text(binding.inputBoardPlatform));
        draft.setBuildRelease(text(binding.inputAndroidRelease));
        draft.setBuildSdk(intValue(binding.inputSdk, draft.getBuildSdk()));
        draft.setSecurityPatch(text(binding.inputSecurityPatch));
        draft.setBuildId(text(binding.inputBuildId));
        draft.setBuildDisplayId(text(binding.inputBuildId));
        draft.setBuildIncremental(text(binding.inputBuildIncremental));
        draft.setBuildFingerprint(text(binding.inputFingerprint));
        draft.setScreenWidth(intValue(binding.inputScreenWidth, draft.getScreenWidth()));
        draft.setScreenHeight(intValue(binding.inputScreenHeight, draft.getScreenHeight()));
        draft.setScreenDensity(intValue(binding.inputScreenDensity, draft.getScreenDensity()));
        draft.setOperatorAlpha(text(binding.inputOperatorAlpha));
        draft.setOperatorNumeric(text(binding.inputOperatorNumeric));
        draft.setSimOperatorAlpha(text(binding.inputOperatorAlpha));
        draft.setSimOperatorNumeric(text(binding.inputOperatorNumeric));
        draft.setSimCountryIso(text(binding.inputSimCountry));
        draft.setTimezone(text(binding.inputTimezone));
        return new Draft(draft, selectedPresetId, customMode);
    }

    private void setupListeners() {
        binding.presetDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= presets.size()) {
                return;
            }
            DevicePreset preset = presets.get(position);
            selectedPresetId = preset.getId();
            workingProfile = preset.getProfile();
            applyMode(false, true);
            updatePresetSummary();
        });

        binding.modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (applying || !isChecked) {
                return;
            }
            if (checkedId == R.id.button_mode_custom) {
                customMode = true;
                setFieldsEnabled(true);
                updatePresetSummary();
                return;
            }

            DevicePreset preset = findPresetById(selectedPresetId);
            if (preset == null && !presets.isEmpty()) {
                preset = presets.get(0);
                selectedPresetId = preset.getId();
                binding.presetDropdown.setText(preset.getDisplayName(), false);
            }
            customMode = false;
            if (preset != null) {
                workingProfile = preset.getProfile();
                bindProfile(workingProfile);
            }
            setFieldsEnabled(false);
            updatePresetSummary();
        });
    }

    private void setupPresetDropdown() {
        List<String> labels = new ArrayList<>();
        for (DevicePreset preset : presets) {
            labels.add(preset.getDisplayName());
        }
        binding.presetDropdown.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, labels));

        DevicePreset preset = findPresetById(selectedPresetId);
        if (preset != null) {
            binding.presetDropdown.setText(preset.getDisplayName(), false);
        } else if (workingProfile != null) {
            binding.presetDropdown.setText(workingProfile.getDisplayName(), false);
        }
    }

    private void applyMode(boolean enableCustom, boolean rebindProfile) {
        applying = true;
        customMode = enableCustom;
        binding.modeToggle.check(enableCustom ? R.id.button_mode_custom : R.id.button_mode_preset);
        applying = false;
        if (rebindProfile && workingProfile != null) {
            bindProfile(workingProfile);
        }
        setFieldsEnabled(enableCustom);
    }

    private void bindProfile(DeviceProfile profile) {
        if (profile == null || binding == null) {
            return;
        }
        setText(binding.inputBrand, profile.getBrand());
        setText(binding.inputManufacturer, profile.getManufacturer());
        setText(binding.inputModel, profile.getModel());
        setText(binding.inputDevice, profile.getDeviceCode());
        setText(binding.inputProduct, profile.getProductName());
        setText(binding.inputBoard, profile.getBoard());
        setText(binding.inputHardware, profile.getHardware());
        setText(binding.inputBoardPlatform, profile.getBoardPlatform());
        setText(binding.inputAndroidRelease, profile.getBuildRelease());
        setText(binding.inputSdk, String.valueOf(profile.getBuildSdk()));
        setText(binding.inputSecurityPatch, profile.getSecurityPatch());
        setText(binding.inputBuildId, profile.getBuildId());
        setText(binding.inputBuildIncremental, profile.getBuildIncremental());
        setText(binding.inputFingerprint, profile.getBuildFingerprint());
        setText(binding.inputScreenWidth, String.valueOf(profile.getScreenWidth()));
        setText(binding.inputScreenHeight, String.valueOf(profile.getScreenHeight()));
        setText(binding.inputScreenDensity, String.valueOf(profile.getScreenDensity()));
        setText(binding.inputOperatorAlpha, profile.getOperatorAlpha());
        setText(binding.inputOperatorNumeric, profile.getOperatorNumeric());
        setText(binding.inputSimCountry, profile.getSimCountryIso());
        setText(binding.inputTimezone, profile.getTimezone());
    }

    private void setFieldsEnabled(boolean enabled) {
        TextInputEditText[] fields = new TextInputEditText[] {
            binding.inputBrand,
            binding.inputManufacturer,
            binding.inputModel,
            binding.inputDevice,
            binding.inputProduct,
            binding.inputBoard,
            binding.inputHardware,
            binding.inputBoardPlatform,
            binding.inputAndroidRelease,
            binding.inputSdk,
            binding.inputSecurityPatch,
            binding.inputBuildId,
            binding.inputBuildIncremental,
            binding.inputFingerprint,
            binding.inputScreenWidth,
            binding.inputScreenHeight,
            binding.inputScreenDensity,
            binding.inputOperatorAlpha,
            binding.inputOperatorNumeric,
            binding.inputSimCountry,
            binding.inputTimezone
        };
        for (TextInputEditText field : fields) {
            field.setEnabled(enabled);
        }
    }

    private void updatePresetSummary() {
        DevicePreset preset = findPresetById(selectedPresetId);
        if (customMode) {
            if (preset != null) {
                binding.presetSummary.setText(getString(
                    R.string.settings_custom_summary,
                    preset.getDisplayName(),
                    preset.getSummary()
                ));
            } else {
                binding.presetSummary.setText(getString(R.string.preset_unknown));
            }
            return;
        }

        if (preset != null) {
            binding.presetSummary.setText(getString(
                R.string.settings_preset_summary,
                preset.getDisplayName(),
                preset.getSummary()
            ));
        } else {
            binding.presetSummary.setText(getString(R.string.preset_unknown));
        }
    }

    @Nullable
    private DevicePreset findPresetById(String presetId) {
        if (presetId == null) {
            return null;
        }
        for (DevicePreset preset : presets) {
            if (presetId.equals(preset.getId())) {
                return preset;
            }
        }
        return null;
    }

    private void setText(TextInputEditText editText, String value) {
        editText.setText(value == null ? "" : value);
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private int intValue(TextInputEditText editText, int fallback) {
        try {
            return Integer.parseInt(text(editText));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static class Draft {
        public final DeviceProfile profile;
        public final String selectedPresetId;
        public final boolean customMode;

        public Draft(DeviceProfile profile, String selectedPresetId, boolean customMode) {
            this.profile = profile;
            this.selectedPresetId = selectedPresetId;
            this.customMode = customMode;
        }
    }
}
