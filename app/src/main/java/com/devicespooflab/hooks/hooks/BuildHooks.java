package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.lang.reflect.Field;

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

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        reapply(lpparam.classLoader);
    }

    public static void reapply(ClassLoader classLoader) {
        try {
            Class<?> buildClass = findBuildClass(classLoader, "android.os.Build");
            Class<?> versionClass = findBuildClass(classLoader, "android.os.Build$VERSION");

            if (buildClass == null) {
                XposedBridge.log(TAG + ": Build class not found");
                return;
            }

            applyBuildFields(buildClass);
            if (versionClass != null) {
                applyVersionFields(versionClass);
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
        setStaticString(buildClass, "BRAND", ConfigManager.getBuildBrand());
        setStaticString(buildClass, "MANUFACTURER", ConfigManager.getBuildManufacturer());
        setStaticString(buildClass, "MODEL", ConfigManager.getBuildModel());
        setStaticString(buildClass, "DEVICE", ConfigManager.getBuildDevice());
        setStaticString(buildClass, "PRODUCT", ConfigManager.getBuildProduct());
        setStaticString(buildClass, "BOARD", ConfigManager.getBuildBoard());
        setStaticString(buildClass, "HARDWARE", ConfigManager.getBuildHardware());
        setStaticString(buildClass, "FINGERPRINT", ConfigManager.getBuildFingerprint());
        setStaticString(buildClass, "ID", ConfigManager.getBuildId());
        setStaticString(buildClass, "DISPLAY", ConfigManager.getBuildDisplay());
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
        setStaticString(versionClass, "RELEASE", ConfigManager.getBuildVersionRelease());
        setStaticString(versionClass, "RELEASE_OR_CODENAME", ConfigManager.getBuildVersionRelease());
        setStaticString(versionClass, "CODENAME", ConfigManager.getBuildVersionCodename());
        setStaticString(versionClass, "INCREMENTAL", ConfigManager.getBuildVersionIncremental());
        setStaticString(versionClass, "SECURITY_PATCH", ConfigManager.getBuildVersionSecurityPatch());
        setStaticInt(versionClass, "SDK_INT", ConfigManager.getBuildVersionSdk());
        setStaticInt(versionClass, "DEVICE_INITIAL_SDK_INT", ConfigManager.getBuildVersionSdk());
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
