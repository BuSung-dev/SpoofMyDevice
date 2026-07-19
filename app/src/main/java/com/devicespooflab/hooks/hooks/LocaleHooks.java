package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class LocaleHooks {

    private LocaleHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        hookLocaleDefaults();
        hookTimeZoneDefaults();
    }

    private static void hookLocaleDefaults() {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Locale locale = configuredLocale();
                if (locale != null) {
                    param.setResult(locale);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(Locale.class, "getDefault", hook);
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(Locale.class, "getDefault", Locale.Category.class, hook);
        } catch (Throwable ignored) {
        }
    }

    private static void hookTimeZoneDefaults() {
        try {
            XposedHelpers.findAndHookMethod(TimeZone.class, "getDefault",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String timezone = ConfigManager.getTimezone();
                        if (timezone != null) {
                            param.setResult(TimeZone.getTimeZone(timezone));
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(ZoneId.class, "systemDefault",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String timezone = ConfigManager.getTimezone();
                        if (timezone == null) {
                            return;
                        }
                        try {
                            param.setResult(ZoneId.of(timezone));
                        } catch (Throwable ignored) {
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static Locale configuredLocale() {
        String languageTag = ConfigManager.getLocaleTag();
        if (languageTag == null) {
            return null;
        }
        Locale locale = Locale.forLanguageTag(languageTag.replace('_', '-'));
        return locale.getLanguage().isEmpty() ? null : locale;
    }
}
