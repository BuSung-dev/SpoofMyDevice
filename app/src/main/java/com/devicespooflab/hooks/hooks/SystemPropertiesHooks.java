package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks android.os.SystemProperties to intercept ALL system property reads.
 * This is the CRITICAL hook that solves the Zygote bypass problem.
 *
 * Apps can read properties via reflection:
 *   Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, "ro.build.fingerprint")
 *
 * This bypasses Magisk's resetprop. We hook at the Java API level to catch these calls.
 */
public class SystemPropertiesHooks {

    private static final String TAG = "DeviceSpoofLab-SystemProps";
    private static final String SYSTEM_PROPERTIES_CLASS = "android.os.SystemProperties";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookSystemProperties(null, lpparam.packageName);

            // Hook SystemProperties in app's classloader
            hookSystemProperties(lpparam.classLoader, lpparam.packageName);

            // Also try to hook in system classloader (for apps that use it)
            try {
                ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                if (systemClassLoader != null && systemClassLoader != lpparam.classLoader) {
                    hookSystemProperties(systemClassLoader, lpparam.packageName);
                }
            } catch (Exception e) {
                // System classloader hook failed, that's okay
            }
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook SystemProperties: " + e.getMessage());
        }
    }

    private static void hookSystemProperties(ClassLoader classLoader, String packageName) {
        Class<?> sysPropClass = XposedHelpers.findClassIfExists(SYSTEM_PROPERTIES_CLASS, classLoader);

        if (sysPropClass == null) {
            return;
        }

        // Hook get(String key)
        try {
            XposedHelpers.findAndHookMethod(sysPropClass, "get",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        if (ConfigManager.shouldBypassVersionSpoof(packageName) && isVersionProperty(key)) {
                            return;
                        }
                        String spoofedValue = ConfigManager.getSystemProperty(key, null);

                        if (spoofedValue != null) {
                            param.setResult(spoofedValue);
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook get(String): " + e.getMessage());
        }

        // Hook get(String key, String def)
        try {
            XposedHelpers.findAndHookMethod(sysPropClass, "get",
                String.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        if (ConfigManager.shouldBypassVersionSpoof(packageName) && isVersionProperty(key)) {
                            return;
                        }
                        String spoofedValue = ConfigManager.getSystemProperty(key, null);

                        if (spoofedValue != null) {
                            param.setResult(spoofedValue);
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook get(String, String): " + e.getMessage());
        }

        // Hook getInt(String key, int def)
        try {
            XposedHelpers.findAndHookMethod(sysPropClass, "getInt",
                String.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        if (ConfigManager.shouldBypassVersionSpoof(packageName) && isVersionProperty(key)) {
                            return;
                        }
                        String spoofedValue = ConfigManager.getSystemProperty(key, null);

                        if (spoofedValue != null) {
                            try {
                                int intValue = Integer.parseInt(spoofedValue);
                                param.setResult(intValue);
                            } catch (NumberFormatException e) {
                                // Invalid int value, keep original
                            }
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook getInt(String, int): " + e.getMessage());
        }

        // Hook getBoolean(String key, boolean def)
        try {
            XposedHelpers.findAndHookMethod(sysPropClass, "getBoolean",
                String.class, boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        if (ConfigManager.shouldBypassVersionSpoof(packageName) && isVersionProperty(key)) {
                            return;
                        }
                        String spoofedValue = ConfigManager.getSystemProperty(key, null);

                        if (spoofedValue != null) {
                            // Handle both "true"/"false" and "1"/"0"
                            boolean boolValue = spoofedValue.equals("1") ||
                                              spoofedValue.equalsIgnoreCase("true");
                            param.setResult(boolValue);
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook getBoolean(String, boolean): " + e.getMessage());
        }

        // Hook getLong(String key, long def)
        try {
            XposedHelpers.findAndHookMethod(sysPropClass, "getLong",
                String.class, long.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        if (ConfigManager.shouldBypassVersionSpoof(packageName) && isVersionProperty(key)) {
                            return;
                        }
                        String spoofedValue = ConfigManager.getSystemProperty(key, null);

                        if (spoofedValue != null) {
                            try {
                                long longValue = Long.parseLong(spoofedValue);
                                param.setResult(longValue);
                            } catch (NumberFormatException e) {
                                // Invalid long value, keep original
                            }
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook getLong(String, long): " + e.getMessage());
        }
    }

    private static boolean isVersionProperty(String key) {
        if (key == null) {
            return false;
        }
        return key.startsWith("ro.build.version.")
            || key.startsWith("ro.product.build.version.")
            || key.contains(".build.version.");
    }
}
