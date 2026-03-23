package com.devicespooflab.hooks.hooks;

import android.content.ContentResolver;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks Settings.Secure.getString() to spoof Android ID and GSF ID.
 *
 * IMPORTANT: Uses narrow hooks on Settings.Secure.getString() rather than
 * broad ContentResolver.query() hooks which would break many apps.
 */
public class SettingsHooks {

    private static final String ANDROID_ID = "android_id";
    private static final String GSF_ID = "gsf_id";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> settingsSecure = XposedHelpers.findClassIfExists(
                "android.provider.Settings$Secure",
                lpparam.classLoader
        );

        if (settingsSecure == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(settingsSecure, "getString",
                    ContentResolver.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[1];

                            if (name == null) {
                                return;
                            }

                            if (ANDROID_ID.equals(name)) {
                                param.setResult(ConfigManager.getAndroidId());
                                return;
                            }

                            if (name.contains("gsf") || GSF_ID.equals(name)) {
                                param.setResult(ConfigManager.getGSFId());
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(settingsSecure, "getString",
                    ContentResolver.class, String.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[1];

                            if (name == null) {
                                return;
                            }

                            if (ANDROID_ID.equals(name)) {
                                param.setResult(ConfigManager.getAndroidId());
                                return;
                            }

                            if (name.contains("gsf") || GSF_ID.equals(name)) {
                                param.setResult(ConfigManager.getGSFId());
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }
}
