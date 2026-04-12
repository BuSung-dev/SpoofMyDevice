package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Applies the configured device profile directly to Build and Build.VERSION static fields.
 *
 * This is more invasive than serial-only spoofing, but many apps read Build fields directly
 * instead of going through SystemProperties. Tablet/phone gating commonly depends on these fields.
 */
public class BuildHooks {

    private static final String TAG = "DeviceSpoofLab-Build";
    private static final Map<String, Object> ORIGINAL_BUILD_FIELDS = new HashMap<>();
    private static final Map<String, Object> ORIGINAL_VERSION_FIELDS = new HashMap<>();

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        reapply(lpparam.classLoader, lpparam.packageName);
    }

    public static void reapply(ClassLoader classLoader) {
        reapply(classLoader, null);
    }

    public static void reapply(ClassLoader classLoader, String packageName) {
        try {
            Class<?> buildClass = findBuildClass(classLoader, "android.os.Build");
            Class<?> versionClass = findBuildClass(classLoader, "android.os.Build$VERSION");

            if (buildClass == null) {
                XposedBridge.log(TAG + ": Build class not found");
                return;
            }

            captureOriginalBuildFields(buildClass);
            if (versionClass != null) {
                captureOriginalVersionFields(versionClass);
            }

            if (ConfigManager.isUsingEmbeddedDefaults()) {
                restoreBuildFields(buildClass);
                if (versionClass != null) {
                    restoreVersionFields(versionClass);
                }
            } else {
                applyBuildFields(buildClass);
                if (versionClass != null) {
                    if (ConfigManager.shouldBypassVersionSpoof(packageName)) {
                        restoreVersionFields(versionClass);
                    } else {
                        applyVersionFields(versionClass);
                    }
                }
            }
            hookGetSerial(buildClass);

            XposedBridge.log(TAG + ": Build fields and getSerial hooked");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook Build methods: " + throwable.getMessage());
        }
    }

    private static Class<?> findBuildClass(ClassLoader classLoader, String className) {
        Class<?> clazz = XposedHelpers.findClassIfExists(className, null);
        if (clazz != null) {
            return clazz;
        }
        clazz = XposedHelpers.findClassIfExists(className, classLoader);
        if (clazz != null) {
            return clazz;
        }
        return XposedHelpers.findClassIfExists(className, ClassLoader.getSystemClassLoader());
    }

    private static void applyBuildFields(Class<?> buildClass) {
        applyOrRestoreString(buildClass, "BRAND", ConfigManager.FIELD_BRAND, ConfigManager.getBuildBrand());
        applyOrRestoreString(buildClass, "MANUFACTURER", ConfigManager.FIELD_MANUFACTURER, ConfigManager.getBuildManufacturer());
        applyOrRestoreString(buildClass, "MODEL", ConfigManager.FIELD_MODEL, ConfigManager.getBuildModel());
        applyOrRestoreString(buildClass, "DEVICE", ConfigManager.FIELD_DEVICE, ConfigManager.getBuildDevice());
        applyOrRestoreString(buildClass, "PRODUCT", ConfigManager.FIELD_PRODUCT, ConfigManager.getBuildProduct());
        applyOrRestoreString(buildClass, "BOARD", ConfigManager.FIELD_BOARD, ConfigManager.getBuildBoard());
        applyOrRestoreString(buildClass, "HARDWARE", ConfigManager.FIELD_HARDWARE, ConfigManager.getBuildHardware());
        applyOrRestoreString(buildClass, "FINGERPRINT", ConfigManager.FIELD_FINGERPRINT, ConfigManager.getBuildFingerprint());
        applyOrRestoreString(buildClass, "ID", ConfigManager.FIELD_BUILD_ID, ConfigManager.getBuildId());
        applyOrRestoreString(buildClass, "DISPLAY", ConfigManager.FIELD_BUILD_ID, ConfigManager.getBuildDisplay());
        setStaticString(buildClass, "TAGS", ConfigManager.getBuildTags());
        setStaticString(buildClass, "TYPE", ConfigManager.getBuildType());
        setStaticString(buildClass, "BOOTLOADER", ConfigManager.getBuildBootloader());
        setStaticString(buildClass, "SERIAL", ConfigManager.getSerial());
        setStaticStringArray(buildClass, "SUPPORTED_ABIS", splitAbis(ConfigManager.getCpuAbiList()));
        setStaticStringArray(buildClass, "SUPPORTED_64_BIT_ABIS", splitAbis(ConfigManager.getCpuAbiList64()));
        setStaticStringArray(buildClass, "SUPPORTED_32_BIT_ABIS", splitAbis(ConfigManager.getCpuAbiList32()));
        setStaticString(buildClass, "CPU_ABI", ConfigManager.getCpuAbi());

        String[] abi32 = splitAbis(ConfigManager.getCpuAbiList32());
        if (abi32.length > 0) {
            setStaticString(buildClass, "CPU_ABI2", abi32.length > 1 ? abi32[1] : abi32[0]);
        }
    }

    private static void applyVersionFields(Class<?> versionClass) {
        applyOrRestoreVersionString(versionClass, "RELEASE", ConfigManager.FIELD_ANDROID_RELEASE, ConfigManager.getBuildVersionRelease());
        applyOrRestoreVersionString(versionClass, "RELEASE_OR_CODENAME", ConfigManager.FIELD_ANDROID_RELEASE, ConfigManager.getBuildVersionRelease());
        applyOrRestoreVersionString(versionClass, "CODENAME", ConfigManager.FIELD_ANDROID_RELEASE, ConfigManager.getBuildVersionCodename());
        applyOrRestoreVersionString(versionClass, "INCREMENTAL", ConfigManager.FIELD_BUILD_INCREMENTAL, ConfigManager.getBuildVersionIncremental());
        applyOrRestoreVersionString(versionClass, "SECURITY_PATCH", ConfigManager.FIELD_SECURITY_PATCH, ConfigManager.getBuildVersionSecurityPatch());
        applyOrRestoreVersionInt(versionClass, "SDK_INT", ConfigManager.FIELD_SDK, ConfigManager.getBuildVersionSdk());
        applyOrRestoreVersionInt(versionClass, "DEVICE_INITIAL_SDK_INT", ConfigManager.FIELD_SDK, ConfigManager.getBuildVersionSdk());
    }

    private static void captureOriginalBuildFields(Class<?> buildClass) {
        captureField(buildClass, "BRAND", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "MANUFACTURER", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "MODEL", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "DEVICE", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "PRODUCT", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "BOARD", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "HARDWARE", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "FINGERPRINT", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "ID", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "DISPLAY", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "TAGS", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "TYPE", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "BOOTLOADER", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "SERIAL", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "SUPPORTED_ABIS", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "SUPPORTED_64_BIT_ABIS", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "SUPPORTED_32_BIT_ABIS", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "CPU_ABI", ORIGINAL_BUILD_FIELDS);
        captureField(buildClass, "CPU_ABI2", ORIGINAL_BUILD_FIELDS);
    }

    private static void captureOriginalVersionFields(Class<?> versionClass) {
        captureField(versionClass, "RELEASE", ORIGINAL_VERSION_FIELDS);
        captureField(versionClass, "RELEASE_OR_CODENAME", ORIGINAL_VERSION_FIELDS);
        captureField(versionClass, "CODENAME", ORIGINAL_VERSION_FIELDS);
        captureField(versionClass, "INCREMENTAL", ORIGINAL_VERSION_FIELDS);
        captureField(versionClass, "SECURITY_PATCH", ORIGINAL_VERSION_FIELDS);
        captureField(versionClass, "SDK_INT", ORIGINAL_VERSION_FIELDS);
        captureField(versionClass, "DEVICE_INITIAL_SDK_INT", ORIGINAL_VERSION_FIELDS);
    }

    private static void restoreBuildFields(Class<?> buildClass) {
        restoreField(buildClass, "BRAND", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "MANUFACTURER", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "MODEL", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "DEVICE", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "PRODUCT", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "BOARD", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "HARDWARE", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "FINGERPRINT", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "ID", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "DISPLAY", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "TAGS", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "TYPE", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "BOOTLOADER", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "SERIAL", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "SUPPORTED_ABIS", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "SUPPORTED_64_BIT_ABIS", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "SUPPORTED_32_BIT_ABIS", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "CPU_ABI", ORIGINAL_BUILD_FIELDS);
        restoreField(buildClass, "CPU_ABI2", ORIGINAL_BUILD_FIELDS);
    }

    private static void restoreVersionFields(Class<?> versionClass) {
        restoreField(versionClass, "RELEASE", ORIGINAL_VERSION_FIELDS);
        restoreField(versionClass, "RELEASE_OR_CODENAME", ORIGINAL_VERSION_FIELDS);
        restoreField(versionClass, "CODENAME", ORIGINAL_VERSION_FIELDS);
        restoreField(versionClass, "INCREMENTAL", ORIGINAL_VERSION_FIELDS);
        restoreField(versionClass, "SECURITY_PATCH", ORIGINAL_VERSION_FIELDS);
        restoreField(versionClass, "SDK_INT", ORIGINAL_VERSION_FIELDS);
        restoreField(versionClass, "DEVICE_INITIAL_SDK_INT", ORIGINAL_VERSION_FIELDS);
    }

    private static void applyOrRestoreString(Class<?> targetClass, String fieldName, String toggleFieldId, String value) {
        if (ConfigManager.isSpoofEnabled(toggleFieldId)) {
            setStaticString(targetClass, fieldName, value);
        } else {
            restoreField(targetClass, fieldName, ORIGINAL_BUILD_FIELDS);
        }
    }

    private static void applyOrRestoreVersionString(Class<?> targetClass, String fieldName, String toggleFieldId, String value) {
        if (ConfigManager.isSpoofEnabled(toggleFieldId)) {
            setStaticString(targetClass, fieldName, value);
        } else {
            restoreField(targetClass, fieldName, ORIGINAL_VERSION_FIELDS);
        }
    }

    private static void applyOrRestoreVersionInt(Class<?> targetClass, String fieldName, String toggleFieldId, int value) {
        if (ConfigManager.isSpoofEnabled(toggleFieldId)) {
            setStaticInt(targetClass, fieldName, value);
        } else {
            restoreField(targetClass, fieldName, ORIGINAL_VERSION_FIELDS);
        }
    }

    private static void captureField(Class<?> targetClass, String fieldName, Map<String, Object> bucket) {
        if (bucket.containsKey(fieldName)) {
            return;
        }
        Field field = XposedHelpers.findFieldIfExists(targetClass, fieldName);
        if (field == null) {
            return;
        }
        try {
            if (field.getType() == Integer.TYPE) {
                bucket.put(fieldName, XposedHelpers.getStaticIntField(targetClass, fieldName));
            } else {
                bucket.put(fieldName, XposedHelpers.getStaticObjectField(targetClass, fieldName));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void restoreField(Class<?> targetClass, String fieldName, Map<String, Object> bucket) {
        Field field = XposedHelpers.findFieldIfExists(targetClass, fieldName);
        if (field == null || !bucket.containsKey(fieldName)) {
            return;
        }
        try {
            if (field.getType() == Integer.TYPE && bucket.get(fieldName) instanceof Integer) {
                XposedHelpers.setStaticIntField(targetClass, fieldName, (Integer) bucket.get(fieldName));
            } else {
                XposedHelpers.setStaticObjectField(targetClass, fieldName, bucket.get(fieldName));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void hookGetSerial(Class<?> buildClass) {
        try {
            XposedHelpers.findAndHookMethod(buildClass, "getSerial",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String spoofedValue = ConfigManager.getSerial();
                        if (spoofedValue != null) {
                            param.setResult(spoofedValue);
                        }
                    }
                });
        } catch (NoSuchMethodError ignored) {
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook getSerial(): " + throwable.getMessage());
        }
    }

    private static void setStaticString(Class<?> targetClass, String fieldName, String value) {
        if (value == null) {
            return;
        }
        Field field = XposedHelpers.findFieldIfExists(targetClass, fieldName);
        if (field == null) {
            return;
        }
        try {
            XposedHelpers.setStaticObjectField(targetClass, fieldName, value);
        } catch (Throwable ignored) {
        }
    }

    private static void setStaticStringArray(Class<?> targetClass, String fieldName, String[] values) {
        Field field = XposedHelpers.findFieldIfExists(targetClass, fieldName);
        if (field == null || values == null || values.length == 0) {
            return;
        }
        try {
            XposedHelpers.setStaticObjectField(targetClass, fieldName, values);
        } catch (Throwable ignored) {
        }
    }

    private static void setStaticInt(Class<?> targetClass, String fieldName, int value) {
        Field field = XposedHelpers.findFieldIfExists(targetClass, fieldName);
        if (field == null) {
            return;
        }
        try {
            XposedHelpers.setStaticIntField(targetClass, fieldName, value);
        } catch (Throwable ignored) {
        }
    }

    private static String[] splitAbis(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new String[0];
        }
        String[] parts = value.split(",");
        for (int index = 0; index < parts.length; index++) {
            parts[index] = parts[index].trim();
        }
        return parts;
    }
}
