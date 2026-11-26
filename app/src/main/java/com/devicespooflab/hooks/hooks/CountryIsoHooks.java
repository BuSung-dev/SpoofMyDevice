package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public final class CountryIsoHooks {
    private CountryIsoHooks() {
    }

    public static void hook(ClassLoader classLoader) {
        XC_MethodHook networkCountry = replaceCountry(ConfigManager.KEY_NETWORK_COUNTRY_ISO);
        XC_MethodHook simCountry = replaceCountry(ConfigManager.KEY_SIM_COUNTRY_ISO);

        hookMethods(classLoader, "android.telephony.TelephonyManager", "getNetworkCountryIso", networkCountry);
        hookMethods(classLoader, "android.telephony.TelephonyManager", "getSimCountryIso", simCountry);
        hookMethods(classLoader, "android.telephony.TelephonyManager", "getSimCountryIsoForPhone", simCountry);
        hookMethods(classLoader, "android.telephony.SubscriptionInfo", "getCountryIso", simCountry);
        hookMethods(classLoader, "android.os.SystemProperties", "get", new XC_MethodHook(XC_MethodHook.PRIORITY_HIGHEST) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String key = (String) param.args[0];
                if (isCountryProperty(key)) {
                    param.setResult(ConfigManager.getCountryIso(key));
                }
            }
        });
    }

    private static boolean isCountryProperty(String key) {
        return ConfigManager.KEY_NETWORK_COUNTRY_ISO.equals(key) || ConfigManager.KEY_SIM_COUNTRY_ISO.equals(key);
    }

    private static XC_MethodHook replaceCountry(String key) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(ConfigManager.getCountryIso(key));
            }
        };
    }

    private static void hookMethods(ClassLoader classLoader, String className, String method, XC_MethodHook callback) {
        try {
            Class<?> type = Class.forName(className, false, classLoader);
            XposedBridge.hookAllMethods(type, method, callback);
        } catch (ClassNotFoundException | RuntimeException | LinkageError exception) {
            XposedBridge.log("SpoofMyDevice: Failed to hook " + className + "." + method + ": " + exception);
        }
    }
}
