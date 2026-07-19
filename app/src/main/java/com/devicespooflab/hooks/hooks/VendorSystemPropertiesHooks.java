package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks OEM system-property wrappers such as Samsung's SemSystemProperties.
 */
public final class VendorSystemPropertiesHooks {

    private static final String TAG = "SpoofMyDevice-VendorProps";
    private static final String[] CANDIDATE_CLASSES = {
        "android.os.SemSystemProperties",
        "com.samsung.android.os.SemSystemProperties"
    };

    private VendorSystemPropertiesHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        for (String className : CANDIDATE_CLASSES) {
            hookClass(className, lpparam.classLoader, lpparam.packageName);
            hookClass(className, null, lpparam.packageName);
        }
    }

    private static void hookClass(String className, ClassLoader classLoader, String packageName) {
        Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);
        if (clazz == null) {
            return;
        }

        hookStringGetter(clazz, packageName, "get", String.class);
        hookStringGetter(clazz, packageName, "get", String.class, String.class);
        hookIntGetter(clazz, packageName);
        hookLongGetter(clazz, packageName);
        hookBooleanGetter(clazz, packageName);
    }

    private static void hookStringGetter(Class<?> clazz, String packageName, Object... parameterTypes) {
        Object[] args = new Object[parameterTypes.length + 1];
        System.arraycopy(parameterTypes, 0, args, 0, parameterTypes.length);
        args[parameterTypes.length] = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String key = (String) param.args[0];
                if (shouldKeepRealVersion(packageName, key)) {
                    return;
                }
                String spoofedValue = ConfigManager.getSystemProperty(key, null);
                if (spoofedValue != null) {
                    param.setResult(spoofedValue);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(clazz, "get", args);
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook " + clazz.getName() + ".get: " + throwable.getMessage());
        }
    }

    private static void hookIntGetter(Class<?> clazz, String packageName) {
        try {
            XposedHelpers.findAndHookMethod(clazz, "getInt",
                String.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[0];
                        if (shouldKeepRealVersion(packageName, key)) {
                            return;
                        }
                        String spoofedValue = ConfigManager.getSystemProperty(key, null);
                        if (spoofedValue == null) {
                            return;
                        }
                        try {
                            param.setResult(Integer.parseInt(spoofedValue));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static void hookLongGetter(Class<?> clazz, String packageName) {
        try {
            XposedHelpers.findAndHookMethod(clazz, "getLong",
                String.class, long.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[0];
                        if (shouldKeepRealVersion(packageName, key)) {
                            return;
                        }
                        String spoofedValue = ConfigManager.getSystemProperty(key, null);
                        if (spoofedValue == null) {
                            return;
                        }
                        try {
                            param.setResult(Long.parseLong(spoofedValue));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static void hookBooleanGetter(Class<?> clazz, String packageName) {
        try {
            XposedHelpers.findAndHookMethod(clazz, "getBoolean",
                String.class, boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[0];
                        if (shouldKeepRealVersion(packageName, key)) {
                            return;
                        }
                        String spoofedValue = ConfigManager.getSystemProperty(key, null);
                        if (spoofedValue == null) {
                            return;
                        }
                        param.setResult("1".equals(spoofedValue) || "true".equalsIgnoreCase(spoofedValue));
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static boolean shouldKeepRealVersion(String packageName, String key) {
        return ConfigManager.shouldBypassVersionSpoof(packageName)
            && key != null
            && (key.startsWith("ro.build.version.")
                || key.startsWith("ro.product.build.version.")
                || key.contains(".build.version."));
    }
}
