package com.devicespooflab.hooks.data;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ConfigFileManager {

    private static final String CONFIG_NAME = "device_profile.conf";
    private static final String META_PREFIX = "# meta.";
    private static final List<String> PARTITIONS = Arrays.asList(
        "product",
        "system",
        "system_ext",
        "vendor",
        "vendor_dlkm",
        "odm",
        "bootimage",
        "system_dlkm"
    );
    private static final Set<String> MANAGED_KEYS = buildManagedKeys();

    public LoadedConfig ensureLoaded(Context context, List<DevicePreset> presets) throws Exception {
        File configFile = getConfigFile(context);
        if (!configFile.exists()) {
            DevicePreset defaultPreset = presets.isEmpty()
                ? new DevicePresetCatalog().createFallbackPreset()
                : presets.get(0);
            return save(context, defaultPreset.getProfile(), new LinkedHashMap<String, String>(), defaultPreset.getId(), false);
        }
        return load(context, presets);
    }

    public LoadedConfig load(Context context, List<DevicePreset> presets) throws Exception {
        File configFile = getConfigFile(context);
        if (!configFile.exists()) {
            return ensureLoaded(context, presets);
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        Map<String, String> properties = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith(META_PREFIX)) {
                    int index = trimmed.indexOf('=');
                    if (index > 0) {
                        metadata.put(
                            trimmed.substring(META_PREFIX.length(), index).trim(),
                            trimmed.substring(index + 1).trim()
                        );
                    }
                    continue;
                }
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int equalIndex = trimmed.indexOf('=');
                if (equalIndex > 0) {
                    properties.put(
                        trimmed.substring(0, equalIndex).trim(),
                        trimmed.substring(equalIndex + 1).trim()
                    );
                }
            }
        }

        String selectedPresetId = metadata.get("preset_id");
        DeviceProfile baseProfile = findPresetProfile(presets, selectedPresetId);
        if (baseProfile == null) {
            baseProfile = presets.isEmpty()
                ? new DevicePresetCatalog().createFallbackPreset().getProfile()
                : presets.get(0).getProfile();
        }

        DeviceProfile profile = mergeProfile(baseProfile, properties);
        profile.applyFallbacks();

        Map<String, String> extraProperties = new LinkedHashMap<>(properties);
        for (String key : MANAGED_KEYS) {
            extraProperties.remove(key);
        }

        if (selectedPresetId == null || selectedPresetId.trim().isEmpty()) {
            selectedPresetId = findMatchingPresetId(profile, presets);
        }

        boolean customMode = "custom".equalsIgnoreCase(metadata.get("mode"));
        if (!customMode && selectedPresetId != null) {
            DeviceProfile presetProfile = findPresetProfile(presets, selectedPresetId);
            customMode = presetProfile == null || !profile.matchesPreset(presetProfile);
        } else if (!customMode && selectedPresetId == null) {
            customMode = true;
        }

        return new LoadedConfig(configFile, profile, extraProperties, selectedPresetId, customMode);
    }

    public LoadedConfig save(
        Context context,
        DeviceProfile draftProfile,
        Map<String, String> extraProperties,
        String selectedPresetId,
        boolean customMode
    ) throws Exception {
        File configFile = getConfigFile(context);
        DeviceProfile profile = draftProfile.copy();
        profile.applyFallbacks();

        StringBuilder builder = new StringBuilder();
        builder.append("# SpoofMyDevice generated profile\n");
        builder.append("# meta.preset_id=").append(selectedPresetId == null ? "" : selectedPresetId).append('\n');
        builder.append("# meta.mode=").append(customMode ? "custom" : "preset").append("\n\n");

        builder.append("# Device Identity\n");
        append(builder, "device.form_factor", profile.getBuildCharacteristics().contains("tablet") ? "tablet" : "phone");
        append(builder, "device.has_telephony", profile.getBuildCharacteristics().contains("tablet") ? "false" : "true");
        append(builder, "ro.product.brand", profile.getBrand());
        append(builder, "ro.product.manufacturer", profile.getManufacturer());
        append(builder, "ro.product.model", profile.getModel());
        append(builder, "ro.product.name", profile.getProductName());
        append(builder, "ro.product.device", profile.getDeviceCode());
        append(builder, "ro.product.board", profile.getBoard());
        append(builder, "ro.hardware", profile.getHardware());
        append(builder, "ro.board.platform", profile.getBoardPlatform());
        for (String partition : PARTITIONS) {
            append(builder, "ro.product." + partition + ".brand", profile.getBrand());
            append(builder, "ro.product." + partition + ".manufacturer", profile.getManufacturer());
            append(builder, "ro.product." + partition + ".model", profile.getModel());
            append(builder, "ro.product." + partition + ".name", profile.getProductName());
            append(builder, "ro.product." + partition + ".device", profile.getDeviceCode());
        }

        builder.append("\n# Build Information\n");
        append(builder, "ro.build.fingerprint", profile.getBuildFingerprint());
        append(builder, "ro.build.id", profile.getBuildId());
        append(builder, "ro.build.display.id", profile.getBuildDisplayId());
        append(builder, "ro.build.version.incremental", profile.getBuildIncremental());
        append(builder, "ro.build.type", "user");
        append(builder, "ro.build.tags", "release-keys");
        append(builder, "ro.build.description", profile.getBuildDescription());
        append(builder, "ro.build.product", profile.getBuildProduct());
        append(builder, "ro.build.device", profile.getDeviceCode());
        append(builder, "ro.build.characteristics", profile.getBuildCharacteristics());
        append(builder, "ro.build.flavor", profile.getBuildFlavor());
        append(builder, "ro.build.version.release", profile.getBuildRelease());
        append(builder, "ro.build.version.release_or_codename", profile.getBuildRelease());
        append(builder, "ro.build.version.release_or_preview_display", profile.getBuildRelease());
        append(builder, "ro.build.version.sdk", String.valueOf(profile.getBuildSdk()));
        append(builder, "ro.build.version.codename", "REL");
        append(builder, "ro.build.version.security_patch", profile.getSecurityPatch());
        append(builder, "ro.product.build.fingerprint", profile.getBuildFingerprint());
        append(builder, "ro.product.build.id", profile.getBuildId());
        append(builder, "ro.product.build.tags", "release-keys");
        append(builder, "ro.product.build.type", "user");
        append(builder, "ro.product.build.version.incremental", profile.getBuildIncremental());
        append(builder, "ro.product.build.version.release", profile.getBuildRelease());
        append(builder, "ro.product.build.version.release_or_codename", profile.getBuildRelease());
        append(builder, "ro.product.build.version.sdk", String.valueOf(profile.getBuildSdk()));
        for (String partition : Arrays.asList("system", "system_ext", "vendor", "odm", "bootimage", "system_dlkm", "vendor_dlkm")) {
            append(builder, "ro." + partition + ".build.fingerprint", profile.getBuildFingerprint());
        }
        for (String partition : Arrays.asList("vendor", "vendor_dlkm", "odm", "bootimage", "system_dlkm")) {
            append(builder, "ro." + partition + ".build.version.release", profile.getBuildRelease());
            append(builder, "ro." + partition + ".build.version.release_or_codename", profile.getBuildRelease());
        }

        builder.append("\n# Security\n");
        append(builder, "ro.debuggable", "0");
        append(builder, "ro.secure", "1");
        append(builder, "ro.adb.secure", "1");
        append(builder, "ro.build.selinux", "0");
        append(builder, "ro.boot.verifiedbootstate", "green");
        append(builder, "ro.boot.flash.locked", "1");
        append(builder, "ro.boot.vbmeta.device_state", "locked");
        append(builder, "ro.boot.warranty_bit", "0");
        append(builder, "sys.oem_unlock_allowed", "0");
        append(builder, "ro.boot.veritymode", "enforcing");
        append(builder, "ro.crypto.state", "encrypted");
        append(builder, "ro.kernel.qemu", "0");
        append(builder, "ro.boot.qemu", "0");
        append(builder, "ro.boot.qemu.avd_name", "");
        append(builder, "ro.boot.qemu.camera_hq_edge_processing", "0");
        append(builder, "ro.boot.qemu.camera_protocol_ver", "0");
        append(builder, "ro.boot.qemu.cpuvulkan.version", "0");
        append(builder, "ro.boot.qemu.gltransport.drawFlushInterval", "0");
        append(builder, "ro.boot.qemu.gltransport.name", "");
        append(builder, "ro.boot.qemu.hwcodec.avcdec", "0");
        append(builder, "ro.boot.qemu.hwcodec.hevcdec", "0");
        append(builder, "ro.boot.qemu.hwcodec.vpxdec", "0");
        append(builder, "ro.boot.qemu.settings.system.screen_off_timeout", "0");
        append(builder, "ro.boot.qemu.virtiowifi", "0");
        append(builder, "ro.boot.qemu.vsync", "0");

        builder.append("\n# Hardware and Display\n");
        append(builder, "ro.boot.hardware", profile.getHardware());
        append(builder, "ro.boot.hardware.vulkan", graphicsStack(profile));
        append(builder, "ro.boot.hardware.gltransport", "");
        append(builder, "ro.boot.mode", "normal");
        append(builder, "ro.product.cpu.abi", profile.getCpuAbi());
        append(builder, "ro.product.cpu.abilist", profile.getCpuAbiList());
        append(builder, "ro.product.cpu.abilist64", profile.getCpuAbiList64());
        append(builder, "ro.product.cpu.abilist32", profile.getCpuAbiList32());
        append(builder, "ro.arch", "arm64");
        append(builder, "ro.sf.lcd_density", String.valueOf(profile.getScreenDensity()));
        append(builder, "ro.treble.enabled", "true");
        append(builder, "ro.hardware.vulkan", graphicsStack(profile));
        append(builder, "ro.hardware.gralloc", profile.getBoardPlatform());
        append(builder, "ro.hardware.power", profile.getBoardPlatform() + "-power");
        append(builder, "ro.hardware.egl", graphicsStack(profile));
        append(builder, "ro.soc.model", profile.getSocModel());
        append(builder, "ro.soc.manufacturer", profile.getSocManufacturer());
        append(builder, "screen.width", String.valueOf(profile.getScreenWidth()));
        append(builder, "screen.height", String.valueOf(profile.getScreenHeight()));
        append(builder, "screen.density", String.valueOf(profile.getScreenDensity()));
        append(builder, "dalvik.vm.heapsize", "576m");
        append(builder, "dalvik.vm.heapgrowthlimit", "256m");
        append(builder, "dalvik.vm.heapmaxfree", "8m");
        append(builder, "dalvik.vm.heapminfree", "512k");
        append(builder, "dalvik.vm.heapstartsize", "8m");
        append(builder, "dalvik.vm.heaptargetutilization", "0.75");

        builder.append("\n# Identifiers\n");
        append(builder, "ro.serialno", nullable(profile.getSerialNumber()));
        append(builder, "ro.boot.serialno", nullable(profile.getSerialNumber()));
        append(builder, "ro.bootloader", nullable(profile.getBootloader()));
        append(builder, "ANDROID_ID", nullable(profile.getAndroidId()));

        builder.append("\n# Carrier\n");
        append(builder, "gsm.operator.alpha", profile.getOperatorAlpha());
        append(builder, "gsm.operator.numeric", profile.getOperatorNumeric());
        append(builder, "gsm.sim.operator.alpha", profile.getSimOperatorAlpha());
        append(builder, "gsm.sim.operator.numeric", profile.getSimOperatorNumeric());
        append(builder, "gsm.sim.operator.iso-country", profile.getSimCountryIso());
        append(builder, "persist.sys.timezone", profile.getTimezone());
        append(builder, "persist.sys.usb.config", "none");

        builder.append("\n# WebView\n");
        append(builder, "webview.user_agent", profile.getUserAgent());

        if (extraProperties != null && !extraProperties.isEmpty()) {
            List<String> keys = new ArrayList<>(extraProperties.keySet());
            keys.sort(String::compareToIgnoreCase);
            builder.append("\n# Preserved extra properties\n");
            for (String key : keys) {
                if (!MANAGED_KEYS.contains(key)) {
                    append(builder, key, extraProperties.get(key));
                }
            }
        }

        try (FileOutputStream outputStream = new FileOutputStream(configFile, false)) {
            outputStream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
        }

        Map<String, String> preserved = extraProperties == null
            ? new LinkedHashMap<String, String>()
            : new LinkedHashMap<>(extraProperties);
        return new LoadedConfig(configFile, profile, preserved, selectedPresetId, customMode);
    }

    public File getConfigFile(Context context) {
        return new File(context.getFilesDir(), CONFIG_NAME);
    }

    private DeviceProfile mergeProfile(DeviceProfile baseProfile, Map<String, String> properties) {
        DeviceProfile profile = baseProfile.copy();
        apply(profile::setBrand, properties.get("ro.product.brand"));
        apply(profile::setManufacturer, properties.get("ro.product.manufacturer"));
        apply(profile::setModel, properties.get("ro.product.model"));
        apply(profile::setProductName, properties.get("ro.product.name"));
        apply(profile::setDeviceCode, properties.get("ro.product.device"));
        apply(profile::setBoard, properties.get("ro.product.board"));
        apply(profile::setHardware, properties.get("ro.hardware"));
        apply(profile::setBoardPlatform, properties.get("ro.board.platform"));
        apply(profile::setBuildFingerprint, properties.get("ro.build.fingerprint"));
        apply(profile::setBuildId, properties.get("ro.build.id"));
        apply(profile::setBuildDisplayId, properties.get("ro.build.display.id"));
        apply(profile::setBuildIncremental, properties.get("ro.build.version.incremental"));
        apply(profile::setBuildRelease, properties.get("ro.build.version.release"));
        profile.setBuildSdk(parseInt(properties.get("ro.build.version.sdk"), profile.getBuildSdk()));
        apply(profile::setSecurityPatch, properties.get("ro.build.version.security_patch"));
        apply(profile::setBuildDescription, properties.get("ro.build.description"));
        apply(profile::setBuildFlavor, properties.get("ro.build.flavor"));
        apply(profile::setBuildProduct, properties.get("ro.build.product"));
        apply(profile::setBuildCharacteristics, properties.get("ro.build.characteristics"));
        profile.setScreenWidth(parseInt(properties.get("screen.width"), profile.getScreenWidth()));
        profile.setScreenHeight(parseInt(properties.get("screen.height"), profile.getScreenHeight()));
        profile.setScreenDensity(parseInt(properties.get("screen.density"), parseInt(properties.get("ro.sf.lcd_density"), profile.getScreenDensity())));
        apply(profile::setOperatorAlpha, properties.get("gsm.operator.alpha"));
        apply(profile::setOperatorNumeric, properties.get("gsm.operator.numeric"));
        apply(profile::setSimOperatorAlpha, properties.get("gsm.sim.operator.alpha"));
        apply(profile::setSimOperatorNumeric, properties.get("gsm.sim.operator.numeric"));
        apply(profile::setSimCountryIso, properties.get("gsm.sim.operator.iso-country"));
        apply(profile::setTimezone, properties.get("persist.sys.timezone"));
        apply(profile::setUserAgent, properties.get("webview.user_agent"));
        apply(profile::setSerialNumber, firstNonBlank(properties.get("ro.serialno"), properties.get("ro.boot.serialno")));
        apply(profile::setBootloader, properties.get("ro.bootloader"));
        apply(profile::setAndroidId, properties.get("ANDROID_ID"));
        apply(profile::setCpuAbi, properties.get("ro.product.cpu.abi"));
        apply(profile::setCpuAbiList, properties.get("ro.product.cpu.abilist"));
        apply(profile::setCpuAbiList64, properties.get("ro.product.cpu.abilist64"));
        apply(profile::setCpuAbiList32, properties.get("ro.product.cpu.abilist32"));
        apply(profile::setSocModel, properties.get("ro.soc.model"));
        apply(profile::setSocManufacturer, properties.get("ro.soc.manufacturer"));
        return profile;
    }

    private DeviceProfile findPresetProfile(List<DevicePreset> presets, String presetId) {
        if (presetId == null) {
            return null;
        }
        for (DevicePreset preset : presets) {
            if (presetId.equals(preset.getId())) {
                return preset.getProfile();
            }
        }
        return null;
    }

    private String findMatchingPresetId(DeviceProfile profile, List<DevicePreset> presets) {
        for (DevicePreset preset : presets) {
            if (profile.matchesPreset(preset.getProfile())) {
                return preset.getId();
            }
        }
        return null;
    }

    private void append(StringBuilder builder, String key, String value) {
        builder.append(key).append('=').append(value == null ? "" : value).append('\n');
    }

    private void apply(ValueSetter setter, String value) {
        if (value != null) {
            setter.set(value);
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String graphicsStack(DeviceProfile profile) {
        String manufacturer = nullable(profile.getManufacturer()).toLowerCase(Locale.US);
        String platform = firstNonBlank(profile.getBoardPlatform(), profile.getHardware()).toLowerCase(Locale.US);
        if (manufacturer.contains("google") || platform.contains("gs") || platform.contains("tensor")) {
            return "mali";
        }
        return "adreno";
    }

    private static String nullable(String value) {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static Set<String> buildManagedKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add("ro.product.brand");
        keys.add("device.form_factor");
        keys.add("device.has_telephony");
        keys.add("ro.product.manufacturer");
        keys.add("ro.product.model");
        keys.add("ro.product.name");
        keys.add("ro.product.device");
        keys.add("ro.product.board");
        keys.add("ro.hardware");
        keys.add("ro.board.platform");
        for (String partition : PARTITIONS) {
            keys.add("ro.product." + partition + ".brand");
            keys.add("ro.product." + partition + ".manufacturer");
            keys.add("ro.product." + partition + ".model");
            keys.add("ro.product." + partition + ".name");
            keys.add("ro.product." + partition + ".device");
        }
        keys.addAll(Arrays.asList(
            "ro.build.fingerprint",
            "ro.build.id",
            "ro.build.display.id",
            "ro.build.version.incremental",
            "ro.build.type",
            "ro.build.tags",
            "ro.build.description",
            "ro.build.product",
            "ro.build.device",
            "ro.build.characteristics",
            "ro.build.flavor",
            "ro.build.version.release",
            "ro.build.version.release_or_codename",
            "ro.build.version.release_or_preview_display",
            "ro.build.version.sdk",
            "ro.build.version.codename",
            "ro.build.version.security_patch",
            "ro.product.build.fingerprint",
            "ro.product.build.id",
            "ro.product.build.tags",
            "ro.product.build.type",
            "ro.product.build.version.incremental",
            "ro.product.build.version.release",
            "ro.product.build.version.release_or_codename",
            "ro.product.build.version.sdk"
        ));
        for (String partition : Arrays.asList("system", "system_ext", "vendor", "odm", "bootimage", "system_dlkm", "vendor_dlkm")) {
            keys.add("ro." + partition + ".build.fingerprint");
        }
        for (String partition : Arrays.asList("vendor", "vendor_dlkm", "odm", "bootimage", "system_dlkm")) {
            keys.add("ro." + partition + ".build.version.release");
            keys.add("ro." + partition + ".build.version.release_or_codename");
        }
        keys.addAll(Arrays.asList(
            "ro.debuggable",
            "ro.secure",
            "ro.adb.secure",
            "ro.build.selinux",
            "ro.boot.verifiedbootstate",
            "ro.boot.flash.locked",
            "ro.boot.vbmeta.device_state",
            "ro.boot.warranty_bit",
            "sys.oem_unlock_allowed",
            "ro.boot.veritymode",
            "ro.crypto.state",
            "ro.kernel.qemu",
            "ro.boot.qemu",
            "ro.boot.qemu.avd_name",
            "ro.boot.qemu.camera_hq_edge_processing",
            "ro.boot.qemu.camera_protocol_ver",
            "ro.boot.qemu.cpuvulkan.version",
            "ro.boot.qemu.gltransport.drawFlushInterval",
            "ro.boot.qemu.gltransport.name",
            "ro.boot.qemu.hwcodec.avcdec",
            "ro.boot.qemu.hwcodec.hevcdec",
            "ro.boot.qemu.hwcodec.vpxdec",
            "ro.boot.qemu.settings.system.screen_off_timeout",
            "ro.boot.qemu.virtiowifi",
            "ro.boot.qemu.vsync",
            "ro.boot.hardware",
            "ro.boot.hardware.vulkan",
            "ro.boot.hardware.gltransport",
            "ro.boot.mode",
            "ro.product.cpu.abi",
            "ro.product.cpu.abilist",
            "ro.product.cpu.abilist64",
            "ro.product.cpu.abilist32",
            "ro.arch",
            "ro.sf.lcd_density",
            "ro.treble.enabled",
            "ro.hardware.vulkan",
            "ro.hardware.gralloc",
            "ro.hardware.power",
            "ro.hardware.egl",
            "ro.soc.model",
            "ro.soc.manufacturer",
            "screen.width",
            "screen.height",
            "screen.density",
            "dalvik.vm.heapsize",
            "dalvik.vm.heapgrowthlimit",
            "dalvik.vm.heapmaxfree",
            "dalvik.vm.heapminfree",
            "dalvik.vm.heapstartsize",
            "dalvik.vm.heaptargetutilization",
            "ro.serialno",
            "ro.boot.serialno",
            "ro.bootloader",
            "ANDROID_ID",
            "gsm.operator.alpha",
            "gsm.operator.numeric",
            "gsm.sim.operator.alpha",
            "gsm.sim.operator.numeric",
            "gsm.sim.operator.iso-country",
            "persist.sys.timezone",
            "persist.sys.usb.config",
            "webview.user_agent"
        ));
        return keys;
    }

    private interface ValueSetter {
        void set(String value);
    }

    public static class LoadedConfig {
        private final File configFile;
        private final DeviceProfile profile;
        private final Map<String, String> extraProperties;
        private final String selectedPresetId;
        private final boolean customMode;

        public LoadedConfig(
            File configFile,
            DeviceProfile profile,
            Map<String, String> extraProperties,
            String selectedPresetId,
            boolean customMode
        ) {
            this.configFile = configFile;
            this.profile = profile.copy();
            this.extraProperties = new LinkedHashMap<>(extraProperties);
            this.selectedPresetId = selectedPresetId;
            this.customMode = customMode;
        }

        public File getConfigFile() {
            return configFile;
        }

        public DeviceProfile getProfile() {
            return profile.copy();
        }

        public Map<String, String> getExtraProperties() {
            return new LinkedHashMap<>(extraProperties);
        }

        public String getSelectedPresetId() {
            return selectedPresetId;
        }

        public boolean isCustomMode() {
            return customMode;
        }
    }
}
