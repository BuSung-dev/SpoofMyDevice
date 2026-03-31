package com.devicespooflab.hooks.utils;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;

import com.devicespooflab.hooks.ConfigProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages configuration for spoofed values.
 *
 * Important detail:
 * - The module app itself can read its private config file directly.
 * - Target apps cannot read another app's private storage, so they must read through
 *   the exported ConfigProvider. If that fails, we fall back to the legacy file paths
 *   and finally to embedded defaults.
 */
public class ConfigManager {

    public static final String KEY_APPLY_SCREEN_METRICS = "device.apply_screen_metrics";
    public static final String KEY_SPOOF_IMEI = "device.imei";
    public static final String KEY_SPOOF_MEID = "device.meid";
    public static final String KEY_SPOOF_IMSI = "device.imsi";
    public static final String KEY_SPOOF_ICCID = "device.iccid";
    public static final String KEY_SPOOF_PHONE_NUMBER = "device.phone_number";
    public static final String KEY_SPOOF_GAID = "device.gaid";
    public static final String KEY_SPOOF_GSF_ID = "device.gsf_id";
    public static final String KEY_SPOOF_MEDIA_DRM_ID = "device.media_drm_id";
    public static final String KEY_SPOOF_APP_SET_ID = "device.app_set_id";
    public static final String KEY_SAFE_MODE_PACKAGES = "safe_mode.packages";
    public static final String KEY_SPOOF_TOGGLE_PREFIX = "spoof.enabled.";

    public static final String FIELD_BRAND = "brand";
    public static final String FIELD_MANUFACTURER = "manufacturer";
    public static final String FIELD_MODEL = "model";
    public static final String FIELD_DEVICE = "device";
    public static final String FIELD_PRODUCT = "product";
    public static final String FIELD_BOARD = "board";
    public static final String FIELD_HARDWARE = "hardware";
    public static final String FIELD_BOARD_PLATFORM = "board_platform";
    public static final String FIELD_ANDROID_RELEASE = "android_release";
    public static final String FIELD_SDK = "sdk";
    public static final String FIELD_SECURITY_PATCH = "security_patch";
    public static final String FIELD_BUILD_ID = "build_id";
    public static final String FIELD_BUILD_INCREMENTAL = "build_incremental";
    public static final String FIELD_FINGERPRINT = "fingerprint";
    public static final String FIELD_SCREEN_WIDTH = "screen_width";
    public static final String FIELD_SCREEN_HEIGHT = "screen_height";
    public static final String FIELD_SCREEN_DENSITY = "screen_density";
    public static final String FIELD_OPERATOR_ALPHA = "operator_alpha";
    public static final String FIELD_OPERATOR_NUMERIC = "operator_numeric";
    public static final String FIELD_SIM_COUNTRY = "sim_country";
    public static final String FIELD_TIMEZONE = "timezone";
    public static final String FIELD_IMEI = "imei";
    public static final String FIELD_MEID = "meid";
    public static final String FIELD_IMSI = "imsi";
    public static final String FIELD_ICCID = "iccid";
    public static final String FIELD_PHONE_NUMBER = "phone_number";
    public static final String FIELD_GAID = "gaid";
    public static final String FIELD_GSF_ID = "gsf_id";
    public static final String FIELD_MEDIA_DRM_ID = "media_drm_id";
    public static final String FIELD_APP_SET_ID = "app_set_id";

    private static final String[] CONFIG_PATHS = {
        "/data/data/com.spoofmydevice/files/device_profile.conf",
        "/data/data/com.devicespooflab.hooks/files/device_profile.conf",
        "/sdcard/SpoofMyDevice/device_profile.conf",
        "/sdcard/DeviceSpoofLab-Hooks/device_profile.conf"
    };
    private static final long RETRY_INTERVAL_MS = 1500L;

    private static Map<String, String> allProperties = null;
    private static boolean usingEmbeddedDefaults = true;
    private static long lastReloadAttemptElapsed = 0L;

    public static synchronized void init() {
        reload(false);
    }

    public static synchronized void forceReload() {
        reload(true, null);
    }

    public static synchronized void forceReload(Context context) {
        reload(true, context);
    }

    public static synchronized boolean isUsingEmbeddedDefaults() {
        ensureFreshConfig();
        return usingEmbeddedDefaults;
    }

    private static void reload(boolean force) {
        reload(force, null);
    }

    private static void reload(boolean force, Context context) {
        long now = SystemClock.elapsedRealtime();
        if (!force && allProperties != null && !usingEmbeddedDefaults) {
            return;
        }
        if (!force && allProperties != null && usingEmbeddedDefaults && (now - lastReloadAttemptElapsed) < RETRY_INTERVAL_MS) {
            return;
        }

        lastReloadAttemptElapsed = now;
        LoadedProperties loadedProperties = readConfig(context);
        allProperties = loadedProperties.properties;
        usingEmbeddedDefaults = loadedProperties.fromEmbeddedDefaults;
        resetGeneratedCaches();
    }

    private static void ensureFreshConfig() {
        if (allProperties == null || usingEmbeddedDefaults) {
            reload(false);
        }
    }

    private static LoadedProperties readConfig(Context preferredContext) {
        Map<String, String> configFromProvider = readFromProvider(preferredContext);
        if (!configFromProvider.isEmpty()) {
            return new LoadedProperties(configFromProvider, false);
        }

        for (String configPath : CONFIG_PATHS) {
            Map<String, String> config = readFromFile(configPath);
            if (!config.isEmpty()) {
                return new LoadedProperties(config, false);
            }
        }

        return new LoadedProperties(getEmbeddedDefaults(), true);
    }

    private static Map<String, String> readFromProvider(Context preferredContext) {
        try {
            Context context = preferredContext != null ? preferredContext : resolveAnyContext();
            if (context == null) {
                return new HashMap<>();
            }

            Uri configUri = ConfigProvider.CONFIG_URI;
            try (InputStream inputStream = context.getContentResolver().openInputStream(configUri)) {
                if (inputStream != null) {
                    return parseConfigStream(inputStream);
                }
            }
        } catch (Exception ignored) {
        }
        return new HashMap<>();
    }

    private static Map<String, String> readFromFile(String path) {
        try {
            File configFile = new File(path);
            if (!configFile.exists() || !configFile.canRead()) {
                return new HashMap<>();
            }

            try (InputStream inputStream = new FileInputStream(configFile)) {
                return parseConfigStream(inputStream);
            }
        } catch (Exception ignored) {
        }
        return new HashMap<>();
    }

    private static Map<String, String> parseConfigStream(InputStream inputStream) {
        Map<String, String> config = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int equalIndex = line.indexOf('=');
                if (equalIndex > 0) {
                    String key = line.substring(0, equalIndex).trim();
                    String value = line.substring(equalIndex + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    config.put(key, value);
                }
            }
        } catch (Exception ignored) {
        }
        return config;
    }

    private static Context resolveAnyContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object currentThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            if (currentThread == null) {
                return null;
            }

            Object currentApplication = activityThreadClass.getMethod("currentApplication").invoke(null);
            if (currentApplication instanceof Context) {
                return (Context) currentApplication;
            }

            Object systemContext = activityThreadClass.getMethod("getSystemContext").invoke(currentThread);
            if (systemContext instanceof Context) {
                return (Context) systemContext;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Map<String, String> getEmbeddedDefaults() {
        Map<String, String> defaults = new HashMap<>();

        defaults.put("device.form_factor", "phone");
        defaults.put("device.has_telephony", "true");
        defaults.put("ro.product.brand", "google");
        defaults.put("ro.product.manufacturer", "Google");
        defaults.put("ro.product.model", "Pixel 7 Pro");
        defaults.put("ro.product.name", "cheetah");
        defaults.put("ro.product.device", "cheetah");
        defaults.put("ro.product.board", "cheetah");
        defaults.put("ro.hardware", "cheetah");
        defaults.put("ro.board.platform", "gs201");

        String[] partitions = {"product", "system", "system_ext", "vendor", "vendor_dlkm", "odm", "bootimage", "system_dlkm"};
        for (String partition : partitions) {
            defaults.put("ro.product." + partition + ".brand", "google");
            defaults.put("ro.product." + partition + ".manufacturer", "Google");
            defaults.put("ro.product." + partition + ".model", "Pixel 7 Pro");
            defaults.put("ro.product." + partition + ".name", "cheetah");
            defaults.put("ro.product." + partition + ".device", "cheetah");
        }

        defaults.put("ro.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.build.id", "AP4A.241205.013");
        defaults.put("ro.build.display.id", "AP4A.241205.013");
        defaults.put("ro.build.version.incremental", "12621605");
        defaults.put("ro.build.type", "user");
        defaults.put("ro.build.tags", "release-keys");
        defaults.put("ro.build.description", "cheetah-user 15 AP4A.241205.013 12621605 release-keys");
        defaults.put("ro.build.product", "cheetah");
        defaults.put("ro.build.device", "cheetah");
        defaults.put("ro.build.characteristics", "nosdcard");
        defaults.put("ro.build.flavor", "cheetah-user");
        defaults.put("ro.build.version.release", "15");
        defaults.put("ro.build.version.release_or_codename", "15");
        defaults.put("ro.build.version.release_or_preview_display", "15");
        defaults.put("ro.build.version.sdk", "35");
        defaults.put("ro.build.version.codename", "REL");
        defaults.put("ro.build.version.security_patch", "2024-12-05");
        defaults.put("ro.product.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.product.build.id", "AP4A.241205.013");
        defaults.put("ro.product.build.tags", "release-keys");
        defaults.put("ro.product.build.type", "user");
        defaults.put("ro.product.build.version.incremental", "12621605");
        defaults.put("ro.product.build.version.release", "15");
        defaults.put("ro.product.build.version.release_or_codename", "15");
        defaults.put("ro.product.build.version.sdk", "35");
        defaults.put("ro.system.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.system_ext.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.vendor.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.odm.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.bootimage.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.system_dlkm.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.vendor_dlkm.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.vendor.build.version.release", "15");
        defaults.put("ro.vendor.build.version.release_or_codename", "15");
        defaults.put("ro.vendor_dlkm.build.version.release", "15");
        defaults.put("ro.vendor_dlkm.build.version.release_or_codename", "15");
        defaults.put("ro.odm.build.version.release", "15");
        defaults.put("ro.odm.build.version.release_or_codename", "15");
        defaults.put("ro.bootimage.build.version.release", "15");
        defaults.put("ro.bootimage.build.version.release_or_codename", "15");
        defaults.put("ro.system_dlkm.build.version.release", "15");
        defaults.put("ro.system_dlkm.build.version.release_or_codename", "15");

        defaults.put("ro.debuggable", "0");
        defaults.put("ro.secure", "1");
        defaults.put("ro.adb.secure", "1");
        defaults.put("ro.build.selinux", "0");
        defaults.put("ro.boot.verifiedbootstate", "green");
        defaults.put("ro.boot.flash.locked", "1");
        defaults.put("ro.boot.vbmeta.device_state", "locked");
        defaults.put("ro.boot.warranty_bit", "0");
        defaults.put("sys.oem_unlock_allowed", "0");
        defaults.put("ro.boot.veritymode", "enforcing");
        defaults.put("ro.crypto.state", "encrypted");
        defaults.put("ro.kernel.qemu", "0");
        defaults.put("ro.boot.qemu", "0");
        defaults.put("ro.boot.qemu.avd_name", "");
        defaults.put("ro.boot.qemu.camera_hq_edge_processing", "0");
        defaults.put("ro.boot.qemu.camera_protocol_ver", "0");
        defaults.put("ro.boot.qemu.cpuvulkan.version", "0");
        defaults.put("ro.boot.qemu.gltransport.drawFlushInterval", "0");
        defaults.put("ro.boot.qemu.gltransport.name", "");
        defaults.put("ro.boot.qemu.hwcodec.avcdec", "0");
        defaults.put("ro.boot.qemu.hwcodec.hevcdec", "0");
        defaults.put("ro.boot.qemu.hwcodec.vpxdec", "0");
        defaults.put("ro.boot.qemu.settings.system.screen_off_timeout", "0");
        defaults.put("ro.boot.qemu.virtiowifi", "0");
        defaults.put("ro.boot.qemu.vsync", "0");

        defaults.put("ro.boot.hardware", "cheetah");
        defaults.put("ro.boot.hardware.vulkan", "mali");
        defaults.put("ro.boot.hardware.gltransport", "");
        defaults.put("ro.boot.mode", "normal");
        defaults.put("ro.product.cpu.abi", "arm64-v8a");
        defaults.put("ro.product.cpu.abilist", "arm64-v8a,armeabi-v7a,armeabi");
        defaults.put("ro.product.cpu.abilist64", "arm64-v8a");
        defaults.put("ro.product.cpu.abilist32", "armeabi-v7a,armeabi");
        defaults.put("ro.arch", "arm64");
        defaults.put("ro.sf.lcd_density", "512");
        defaults.put("ro.treble.enabled", "true");
        defaults.put("ro.hardware.vulkan", "mali");
        defaults.put("ro.hardware.gralloc", "gs201");
        defaults.put("ro.hardware.power", "gs201-power");
        defaults.put("ro.hardware.egl", "mali");
        defaults.put("ro.soc.model", "gs201");
        defaults.put("ro.soc.manufacturer", "Google");
        defaults.put("screen.width", "1440");
        defaults.put("screen.height", "3120");
        defaults.put("screen.density", "512");
        defaults.put("dalvik.vm.heapsize", "576m");
        defaults.put("dalvik.vm.heapgrowthlimit", "256m");
        defaults.put("dalvik.vm.heapmaxfree", "8m");
        defaults.put("dalvik.vm.heapminfree", "512k");
        defaults.put("dalvik.vm.heapstartsize", "8m");
        defaults.put("dalvik.vm.heaptargetutilization", "0.75");

        defaults.put("gsm.operator.alpha", "T-Mobile");
        defaults.put("gsm.operator.numeric", "310260");
        defaults.put("gsm.sim.operator.alpha", "T-Mobile");
        defaults.put("gsm.sim.operator.numeric", "310260");
        defaults.put("gsm.sim.operator.iso-country", "us");
        defaults.put("persist.sys.timezone", "America/Los_Angeles");
        defaults.put("persist.sys.usb.config", "none");
        defaults.put("webview.user_agent", "Mozilla/5.0 (Linux; Android 15; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36");

        return defaults;
    }

    private static String getConfigValue(String key) {
        ensureFreshConfig();
        return allProperties.get(key);
    }

    private static boolean hasConfigValue(String key) {
        String value = getConfigValue(key);
        return value != null && !value.isEmpty();
    }

    private static String getOptionalConfigValue(String key) {
        String value = getConfigValue(key);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private static String getFirstOptionalConfigValue(String... keys) {
        for (String key : keys) {
            String value = getOptionalConfigValue(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static String getSystemProperty(String key, String defaultValue) {
        String fieldId = getToggleFieldForSystemProperty(key);
        if (fieldId != null && !isSpoofEnabled(fieldId)) {
            return defaultValue;
        }
        String value = getConfigValue(key);
        return (value != null) ? value : defaultValue;
    }

    public static boolean isTabletProfile() {
        String formFactor = getConfigValue("device.form_factor");
        if (formFactor != null && !formFactor.isEmpty()) {
            return "tablet".equalsIgnoreCase(formFactor);
        }

        String characteristics = getBuildCharacteristics();
        if (characteristics != null && characteristics.toLowerCase(Locale.US).contains("tablet")) {
            return true;
        }

        return getSmallestScreenWidthDp() >= 600;
    }

    public static boolean hasTelephonySupport() {
        String explicit = getConfigValue("device.has_telephony");
        if (explicit != null && !explicit.isEmpty()) {
            return explicit.equals("1") || explicit.equalsIgnoreCase("true");
        }
        return !isTabletProfile();
    }

    public static int getScreenWidth() {
        return parseInt(getConfigValue("screen.width"), 1440);
    }

    public static int getScreenHeight() {
        return parseInt(getConfigValue("screen.height"), 3120);
    }

    public static int getScreenDensityDpi() {
        return parseInt(getConfigValue("screen.density"), parseInt(getConfigValue("ro.sf.lcd_density"), 480));
    }

    public static int getSmallestScreenWidthDp() {
        int width = getScreenWidth();
        int height = getScreenHeight();
        int density = getScreenDensityDpi();
        if (density <= 0) {
            density = 480;
        }
        return Math.round((Math.min(width, height) * 160f) / density);
    }

    public static int getScreenWidthDp() {
        int density = getScreenDensityDpi();
        if (density <= 0) {
            density = 480;
        }
        return Math.round((getScreenWidth() * 160f) / density);
    }

    public static int getScreenHeightDp() {
        int density = getScreenDensityDpi();
        if (density <= 0) {
            density = 480;
        }
        return Math.round((getScreenHeight() * 160f) / density);
    }

    public static float getScreenDensity() {
        return getScreenDensityDpi() / 160f;
    }

    public static boolean shouldApplyScreenMetrics() {
        String explicit = getConfigValue(KEY_APPLY_SCREEN_METRICS);
        if (explicit == null || explicit.isEmpty()) {
            return false;
        }
        return explicit.equals("1") || explicit.equalsIgnoreCase("true");
    }

    public static boolean shouldBypassVersionSpoof(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return false;
        }
        String configured = getOptionalConfigValue(KEY_SAFE_MODE_PACKAGES);
        if (configured == null) {
            return false;
        }
        String normalizedPackage = packageName.trim();
        String[] parts = configured.split("[,\\n]");
        for (String part : parts) {
            if (normalizedPackage.equals(part.trim())) {
                return true;
            }
        }
        return false;
    }

    public static String getTogglePropertyKey(String fieldId) {
        return KEY_SPOOF_TOGGLE_PREFIX + fieldId;
    }

    public static boolean isSpoofEnabled(String fieldId) {
        if (fieldId == null || fieldId.trim().isEmpty()) {
            return true;
        }
        String explicit = getOptionalConfigValue(getTogglePropertyKey(fieldId));
        if (explicit == null) {
            return true;
        }
        return explicit.equals("1") || explicit.equalsIgnoreCase("true");
    }

    public static String getToggleFieldForSystemProperty(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        if (key.equals("ro.product.brand") || key.endsWith(".brand")) {
            return FIELD_BRAND;
        }
        if (key.equals("ro.product.manufacturer") || key.endsWith(".manufacturer")) {
            return FIELD_MANUFACTURER;
        }
        if (key.equals("ro.product.model") || key.endsWith(".model")) {
            return FIELD_MODEL;
        }
        if (key.equals("ro.product.device") || key.equals("ro.build.device") || key.endsWith(".device")) {
            return FIELD_DEVICE;
        }
        if (key.equals("ro.product.name") || key.equals("ro.build.product") || key.endsWith(".name")) {
            return FIELD_PRODUCT;
        }
        if (key.equals("ro.product.board")) {
            return FIELD_BOARD;
        }
        if (key.equals("ro.hardware") || key.equals("ro.boot.hardware")) {
            return FIELD_HARDWARE;
        }
        if (key.equals("ro.board.platform")) {
            return FIELD_BOARD_PLATFORM;
        }
        if (key.equals("ro.build.id") || key.equals("ro.build.display.id") || key.equals("ro.product.build.id")) {
            return FIELD_BUILD_ID;
        }
        if (key.equals("ro.build.version.incremental") || key.equals("ro.product.build.version.incremental")) {
            return FIELD_BUILD_INCREMENTAL;
        }
        if (key.equals("ro.build.version.release")
            || key.equals("ro.build.version.release_or_codename")
            || key.equals("ro.build.version.release_or_preview_display")
            || key.equals("ro.product.build.version.release")
            || key.equals("ro.product.build.version.release_or_codename")
            || key.endsWith(".build.version.release")
            || key.endsWith(".build.version.release_or_codename")) {
            return FIELD_ANDROID_RELEASE;
        }
        if (key.equals("ro.build.version.sdk")
            || key.equals("ro.product.build.version.sdk")
            || key.endsWith(".build.version.sdk")) {
            return FIELD_SDK;
        }
        if (key.equals("ro.build.version.security_patch")) {
            return FIELD_SECURITY_PATCH;
        }
        if (key.equals("ro.build.fingerprint")
            || key.equals("ro.product.build.fingerprint")
            || key.endsWith(".build.fingerprint")) {
            return FIELD_FINGERPRINT;
        }
        if (key.equals("screen.width")) {
            return FIELD_SCREEN_WIDTH;
        }
        if (key.equals("screen.height")) {
            return FIELD_SCREEN_HEIGHT;
        }
        if (key.equals("screen.density") || key.equals("ro.sf.lcd_density")) {
            return FIELD_SCREEN_DENSITY;
        }
        if (key.equals("gsm.operator.alpha") || key.equals("gsm.sim.operator.alpha")) {
            return FIELD_OPERATOR_ALPHA;
        }
        if (key.equals("gsm.operator.numeric") || key.equals("gsm.sim.operator.numeric")) {
            return FIELD_OPERATOR_NUMERIC;
        }
        if (key.equals("gsm.sim.operator.iso-country")) {
            return FIELD_SIM_COUNTRY;
        }
        if (key.equals("persist.sys.timezone")) {
            return FIELD_TIMEZONE;
        }
        return null;
    }

    public static String getIMEI() {
        if (!isSpoofEnabled(FIELD_IMEI)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_IMEI);
    }

    public static String getMEID() {
        if (!isSpoofEnabled(FIELD_MEID)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_MEID);
    }

    public static String getIMSI() {
        if (!isSpoofEnabled(FIELD_IMSI)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_IMSI);
    }

    public static String getICCID() {
        if (!isSpoofEnabled(FIELD_ICCID)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_ICCID);
    }

    public static String getPhoneNumber() {
        if (!isSpoofEnabled(FIELD_PHONE_NUMBER)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_PHONE_NUMBER);
    }

    public static String getSerial() {
        return getFirstOptionalConfigValue("ro.serialno", "ro.boot.serialno", "SERIAL_NUMBER");
    }

    public static String getGAID() {
        if (!isSpoofEnabled(FIELD_GAID)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_GAID);
    }

    public static String getGSFId() {
        if (!isSpoofEnabled(FIELD_GSF_ID)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_GSF_ID);
    }

    public static String getAndroidId() {
        return getOptionalConfigValue("ANDROID_ID");
    }

    public static byte[] getMediaDrmId() {
        if (!isSpoofEnabled(FIELD_MEDIA_DRM_ID)) {
            return null;
        }
        String hex = getOptionalConfigValue(KEY_SPOOF_MEDIA_DRM_ID);
        if (hex == null) {
            return null;
        }
        return decodeHex(hex);
    }

    public static String getAppSetId() {
        if (!isSpoofEnabled(FIELD_APP_SET_ID)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_APP_SET_ID);
    }

    public static boolean isConfigAvailable() {
        ensureFreshConfig();
        return !allProperties.isEmpty();
    }

    public static String getBuildFingerprint() {
        return getConfigValue("ro.build.fingerprint");
    }

    public static String getBuildModel() {
        return getConfigValue("ro.product.model");
    }

    public static String getBuildDevice() {
        return getConfigValue("ro.product.device");
    }

    public static String getBuildManufacturer() {
        return getConfigValue("ro.product.manufacturer");
    }

    public static String getBuildBrand() {
        return getConfigValue("ro.product.brand");
    }

    public static String getBuildProduct() {
        return getConfigValue("ro.product.name");
    }

    public static String getBuildBoard() {
        return getConfigValue("ro.product.board");
    }

    public static String getBuildHardware() {
        return getConfigValue("ro.hardware");
    }

    public static String getBuildBootloader() {
        return getOptionalConfigValue("ro.bootloader");
    }

    public static String getBuildId() {
        return getConfigValue("ro.build.id");
    }

    public static String getBuildDisplay() {
        return getConfigValue("ro.build.display.id");
    }

    public static String getBuildTags() {
        return getConfigValue("ro.build.tags");
    }

    public static String getBuildType() {
        return getConfigValue("ro.build.type");
    }

    public static String getBuildVersionRelease() {
        return getConfigValue("ro.build.version.release");
    }

    public static int getBuildVersionSdk() {
        return parseInt(getConfigValue("ro.build.version.sdk"), 35);
    }

    public static String getBuildVersionSecurityPatch() {
        return getConfigValue("ro.build.version.security_patch");
    }

    public static String getBuildVersionIncremental() {
        return getConfigValue("ro.build.version.incremental");
    }

    public static String getBuildVersionCodename() {
        return getConfigValue("ro.build.version.codename");
    }

    public static String getBuildDescription() {
        return getConfigValue("ro.build.description");
    }

    public static String getBuildCharacteristics() {
        return getConfigValue("ro.build.characteristics");
    }

    public static String getBuildFlavor() {
        return getConfigValue("ro.build.flavor");
    }

    public static String getWebViewUserAgent() {
        return getConfigValue("webview.user_agent");
    }

    public static String getCpuAbi() {
        return getConfigValue("ro.product.cpu.abi");
    }

    public static String getCpuAbiList() {
        return getConfigValue("ro.product.cpu.abilist");
    }

    public static String getCpuAbiList64() {
        return getConfigValue("ro.product.cpu.abilist64");
    }

    public static String getCpuAbiList32() {
        return getConfigValue("ro.product.cpu.abilist32");
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void resetGeneratedCaches() {
        // Intentionally empty.
    }

    private static byte[] decodeHex(String hex) {
        String normalized = hex.replace(" ", "").replace(":", "");
        if ((normalized.length() % 2) != 0) {
            return null;
        }

        try {
            byte[] result = new byte[normalized.length() / 2];
            for (int index = 0; index < normalized.length(); index += 2) {
                int value = Integer.parseInt(normalized.substring(index, index + 2), 16);
                result[index / 2] = (byte) value;
            }
            return result;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class LoadedProperties {
        private final Map<String, String> properties;
        private final boolean fromEmbeddedDefaults;

        private LoadedProperties(Map<String, String> properties, boolean fromEmbeddedDefaults) {
            this.properties = properties;
            this.fromEmbeddedDefaults = fromEmbeddedDefaults;
        }
    }
}
