package com.devicespooflab.hooks.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaDrm;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.devicespooflab.hooks.MainActivity;
import com.devicespooflab.hooks.R;
import com.devicespooflab.hooks.data.ConfigFileManager;
import com.devicespooflab.hooks.data.DevicePreset;
import com.devicespooflab.hooks.data.DeviceProfile;
import com.devicespooflab.hooks.databinding.FragmentDeviceSettingsBinding;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.utils.RandomGenerator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
public class DeviceSettingsFragment extends Fragment {

    private static final UUID WIDEVINE_UUID = new UUID(
        0xedef8ba979d64aceL,
        0xa3c827dcd51d21edL
    );

    private FragmentDeviceSettingsBinding binding;
    private final List<DevicePreset> presets = new ArrayList<>();
    private final ActivityResultLauncher<String[]> phonePermissionsLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (binding == null) {
                return;
            }
            populateAdvancedDefaultsIfNeeded();
        });
    private String selectedPresetId;
    private boolean customMode;
    private boolean initialized;
    private boolean applying;
    private boolean advancedExpanded;
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
        updateAdvancedSectionState();
    }

    @Override
    public void onResume() {
        super.onResume();
        advancedExpanded = false;
        updateAdvancedSectionState();
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
        advancedExpanded = false;
        updateAdvancedSectionState();
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
        bindAdvancedProperties(loadedConfig.getExtraProperties());
        populateAdvancedDefaultsIfNeeded();
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
        return new Draft(draft, selectedPresetId, customMode, buildExtraProperties());
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

        binding.advancedToggleHeader.setOnClickListener(v -> {
            advancedExpanded = !advancedExpanded;
            updateAdvancedSectionState();
            if (advancedExpanded) {
                maybeRequestPhonePermissions();
                populateAdvancedDefaultsIfNeeded();
            }
        });

        binding.buttonAdvancedClearAll.setOnClickListener(v -> clearAdvancedFields());

        binding.layoutAdvancedImei.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedImei, RandomGenerator.generateIMEI())
        );
        binding.layoutAdvancedMeid.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedMeid, RandomGenerator.generateMEID())
        );
        binding.layoutAdvancedImsi.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedImsi, RandomGenerator.generateIMSI())
        );
        binding.layoutAdvancedIccid.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedIccid, RandomGenerator.generateICCID())
        );
        binding.layoutAdvancedPhoneNumber.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedPhoneNumber, RandomGenerator.generatePhoneNumber())
        );
        binding.layoutAdvancedGaid.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedGaid, RandomGenerator.generateGAID())
        );
        binding.layoutAdvancedGsfId.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedGsfId, RandomGenerator.generateGSFId())
        );
        binding.layoutAdvancedMediaDrmId.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedMediaDrmId, toHex(RandomGenerator.generateMediaDrmId()))
        );
        binding.layoutAdvancedAppSetId.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedAppSetId, RandomGenerator.generateGAID())
        );
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

    private void bindAdvancedProperties(Map<String, String> extraProperties) {
        setText(binding.inputAdvancedImei, extraProperties.get(ConfigManager.KEY_SPOOF_IMEI));
        setText(binding.inputAdvancedMeid, extraProperties.get(ConfigManager.KEY_SPOOF_MEID));
        setText(binding.inputAdvancedImsi, extraProperties.get(ConfigManager.KEY_SPOOF_IMSI));
        setText(binding.inputAdvancedIccid, extraProperties.get(ConfigManager.KEY_SPOOF_ICCID));
        setText(binding.inputAdvancedPhoneNumber, extraProperties.get(ConfigManager.KEY_SPOOF_PHONE_NUMBER));
        setText(binding.inputAdvancedGaid, extraProperties.get(ConfigManager.KEY_SPOOF_GAID));
        setText(binding.inputAdvancedGsfId, extraProperties.get(ConfigManager.KEY_SPOOF_GSF_ID));
        setText(binding.inputAdvancedMediaDrmId, extraProperties.get(ConfigManager.KEY_SPOOF_MEDIA_DRM_ID));
        setText(binding.inputAdvancedAppSetId, extraProperties.get(ConfigManager.KEY_SPOOF_APP_SET_ID));
    }

    private void populateAdvancedDefaultsIfNeeded() {
        setIfBlank(binding.inputAdvancedImei, resolveCurrentImei());
        setIfBlank(binding.inputAdvancedMeid, resolveCurrentMeid());
        setIfBlank(binding.inputAdvancedImsi, resolveCurrentImsi());
        setIfBlank(binding.inputAdvancedIccid, resolveCurrentIccid());
        setIfBlank(binding.inputAdvancedPhoneNumber, resolveCurrentPhoneNumber());
        setIfBlank(binding.inputAdvancedGsfId, resolveCurrentGsfId());
        setIfBlank(binding.inputAdvancedMediaDrmId, resolveCurrentMediaDrmId());
        loadGoogleIdsIfNeeded();
    }

    private void maybeRequestPhonePermissions() {
        List<String> missingPermissions = new ArrayList<>();
        Context context = requireContext();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_PHONE_NUMBERS);
        }

        if (!missingPermissions.isEmpty()) {
            phonePermissionsLauncher.launch(missingPermissions.toArray(new String[0]));
        }
    }

    private void updateAdvancedSectionState() {
        if (binding == null) {
            return;
        }
        binding.advancedContentCard.setVisibility(advancedExpanded ? View.VISIBLE : View.GONE);
        binding.advancedToggleIcon.setRotation(advancedExpanded ? 180f : 0f);
    }

    private Map<String, String> buildExtraProperties() {
        Map<String, String> extraProperties = new LinkedHashMap<>();
        if (requireActivity() instanceof MainActivity) {
            extraProperties.putAll(((MainActivity) requireActivity()).getLoadedConfigState().getExtraProperties());
        }

        putOptional(extraProperties, ConfigManager.KEY_SPOOF_IMEI, text(binding.inputAdvancedImei));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_MEID, text(binding.inputAdvancedMeid));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_IMSI, text(binding.inputAdvancedImsi));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_ICCID, text(binding.inputAdvancedIccid));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_PHONE_NUMBER, text(binding.inputAdvancedPhoneNumber));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_GAID, text(binding.inputAdvancedGaid));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_GSF_ID, text(binding.inputAdvancedGsfId));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_MEDIA_DRM_ID, text(binding.inputAdvancedMediaDrmId));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_APP_SET_ID, text(binding.inputAdvancedAppSetId));
        return extraProperties;
    }

    private void clearAdvancedFields() {
        setText(binding.inputAdvancedImei, "");
        setText(binding.inputAdvancedMeid, "");
        setText(binding.inputAdvancedImsi, "");
        setText(binding.inputAdvancedIccid, "");
        setText(binding.inputAdvancedPhoneNumber, "");
        setText(binding.inputAdvancedGaid, "");
        setText(binding.inputAdvancedGsfId, "");
        setText(binding.inputAdvancedMediaDrmId, "");
        setText(binding.inputAdvancedAppSetId, "");
    }

    private void putOptional(Map<String, String> target, String key, String value) {
        if (value == null || value.isEmpty()) {
            target.remove(key);
            return;
        }
        target.put(key, value);
    }

    private void setIfBlank(TextInputEditText editText, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (text(editText).isEmpty()) {
            setText(editText, value);
        }
    }

    private String resolveCurrentImei() {
        try {
            TelephonyManager telephonyManager = requireContext().getSystemService(TelephonyManager.class);
            if (telephonyManager == null) {
                return null;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return telephonyManager.getImei();
            }
            return telephonyManager.getDeviceId();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveCurrentMeid() {
        try {
            TelephonyManager telephonyManager = requireContext().getSystemService(TelephonyManager.class);
            if (telephonyManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return null;
            }
            return telephonyManager.getMeid();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveCurrentImsi() {
        try {
            TelephonyManager telephonyManager = requireContext().getSystemService(TelephonyManager.class);
            if (telephonyManager == null) {
                return null;
            }
            return telephonyManager.getSubscriberId();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveCurrentIccid() {
        try {
            SubscriptionManager subscriptionManager = requireContext().getSystemService(SubscriptionManager.class);
            if (subscriptionManager == null) {
                return null;
            }
            List<SubscriptionInfo> subscriptions = subscriptionManager.getActiveSubscriptionInfoList();
            if (subscriptions == null || subscriptions.isEmpty()) {
                return null;
            }
            String iccId = subscriptions.get(0).getIccId();
            return (iccId == null || iccId.trim().isEmpty()) ? null : iccId;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveCurrentPhoneNumber() {
        try {
            TelephonyManager telephonyManager = requireContext().getSystemService(TelephonyManager.class);
            if (telephonyManager == null) {
                return null;
            }
            return telephonyManager.getLine1Number();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveCurrentGsfId() {
        Cursor cursor = null;
        try {
            cursor = requireContext().getContentResolver().query(
                Uri.parse("content://com.google.android.gsf.gservices"),
                null,
                null,
                new String[]{"android_id"},
                null
            );
            if (cursor != null && cursor.moveToFirst() && cursor.getColumnCount() >= 2) {
                String gsfId = cursor.getString(1);
                return (gsfId == null || gsfId.trim().isEmpty()) ? null : gsfId;
            }
        } catch (Throwable ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private String resolveCurrentMediaDrmId() {
        MediaDrm mediaDrm = null;
        try {
            mediaDrm = new MediaDrm(WIDEVINE_UUID);
            byte[] value = mediaDrm.getPropertyByteArray("deviceUniqueId");
            if (value == null || value.length == 0) {
                return null;
            }
            StringBuilder builder = new StringBuilder(value.length * 2);
            for (byte b : value) {
                builder.append(String.format("%02x", b & 0xff));
            }
            return builder.toString();
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (mediaDrm != null) {
                try {
                    mediaDrm.release();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private void loadGoogleIdsIfNeeded() {
        Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            String gaid = resolveCurrentGaid(appContext);
            String appSetId = resolveCurrentAppSetId(appContext);
            if (binding == null) {
                return;
            }
            binding.getRoot().post(() -> {
                if (binding == null) {
                    return;
                }
                setIfBlank(binding.inputAdvancedGaid, gaid);
                setIfBlank(binding.inputAdvancedAppSetId, appSetId);
            });
        }, "spoofmydevice-google-id-loader").start();
    }

    private String resolveCurrentGaid(Context context) {
        try {
            Class<?> clientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            Object info = clientClass.getMethod("getAdvertisingIdInfo", Context.class).invoke(null, context);
            if (info == null) {
                return null;
            }
            Object result = info.getClass().getMethod("getId").invoke(info);
            return result instanceof String ? (String) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveCurrentAppSetId(Context context) {
        try {
            Class<?> appSetClass = Class.forName("com.google.android.gms.appset.AppSet");
            Object client = appSetClass.getMethod("getClient", Context.class).invoke(null, context);
            if (client == null) {
                return null;
            }

            Object task = client.getClass().getMethod("getAppSetIdInfo").invoke(client);
            if (task == null) {
                return null;
            }

            Class<?> taskClass = Class.forName("com.google.android.gms.tasks.Task");
            Class<?> tasksClass = Class.forName("com.google.android.gms.tasks.Tasks");
            Object info = tasksClass.getMethod("await", taskClass).invoke(null, task);
            if (info == null) {
                return null;
            }

            Object result = info.getClass().getMethod("getId").invoke(info);
            return result instanceof String ? (String) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String toHex(byte[] value) {
        if (value == null || value.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte b : value) {
            builder.append(String.format("%02x", b & 0xff));
        }
        return builder.toString();
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
        public final Map<String, String> extraProperties;

        public Draft(DeviceProfile profile, String selectedPresetId, boolean customMode, Map<String, String> extraProperties) {
            this.profile = profile;
            this.selectedPresetId = selectedPresetId;
            this.customMode = customMode;
            this.extraProperties = new LinkedHashMap<>(extraProperties);
        }
    }
}
