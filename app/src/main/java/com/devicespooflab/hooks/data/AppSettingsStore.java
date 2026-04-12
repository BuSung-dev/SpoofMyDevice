package com.devicespooflab.hooks.data;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.devicespooflab.hooks.R;

public final class AppSettingsStore {

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    public static final String LANGUAGE_DEFAULT = "default";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_KOREAN = "ko";
    public static final String LANGUAGE_JAPANESE = "ja";
    public static final String LANGUAGE_CHINESE_SIMPLIFIED = "zh-CN";

    public static final String COLOR_STYLE_MINT = "mint";
    public static final String COLOR_STYLE_BLUE = "blue";
    public static final String COLOR_STYLE_ROSE = "rose";
    public static final String COLOR_STYLE_AMBER = "amber";
    public static final String DEFAULT_PRESET_SOURCE_URL = "https://github.com/BuSung-dev/SpoofMyDevice_Devices.git";

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_LANGUAGE_MODE = "language_mode";
    private static final String KEY_USE_SYSTEM_COLORS = "use_system_colors";
    private static final String KEY_COLOR_STYLE = "color_style";
    private static final String KEY_PRESET_SOURCE_URL = "preset_source_url";

    private AppSettingsStore() {
    }

    public static void applyActivityTheme(Activity activity) {
        if (isSystemColorEnabled(activity) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.setTheme(R.style.Theme_DeviceSpoofLab_Dynamic);
            return;
        }
        activity.setTheme(resolveStaticTheme(getColorStyle(activity)));
    }

    public static void apply(Activity activity) {
        applyThemeIfNeeded(getThemeMode(activity));
        applyLanguageIfNeeded(getLanguageMode(activity));
    }

    public static String getThemeMode(Context context) {
        return normalizeTheme(preferences(context).getString(KEY_THEME_MODE, THEME_SYSTEM));
    }

    public static void setThemeMode(Context context, String value) {
        String normalized = normalizeTheme(value);
        preferences(context).edit().putString(KEY_THEME_MODE, normalized).apply();
        applyThemeIfNeeded(normalized);
    }

    public static String getLanguageMode(Context context) {
        return normalizeLanguage(preferences(context).getString(KEY_LANGUAGE_MODE, LANGUAGE_DEFAULT));
    }

    public static void setLanguageMode(Context context, String value) {
        String normalized = normalizeLanguage(value);
        preferences(context).edit().putString(KEY_LANGUAGE_MODE, normalized).apply();
        applyLanguageIfNeeded(normalized);
    }

    public static boolean isSystemColorEnabled(Context context) {
        return preferences(context).getBoolean(KEY_USE_SYSTEM_COLORS, true);
    }

    public static void setSystemColorEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_USE_SYSTEM_COLORS, enabled).apply();
    }

    public static String getColorStyle(Context context) {
        return normalizeColorStyle(preferences(context).getString(KEY_COLOR_STYLE, COLOR_STYLE_MINT));
    }

    public static void setColorStyle(Context context, String value) {
        preferences(context).edit().putString(KEY_COLOR_STYLE, normalizeColorStyle(value)).apply();
    }

    public static String getPresetSourceUrl(Context context) {
        return normalizePresetSourceUrl(
            preferences(context).getString(KEY_PRESET_SOURCE_URL, DEFAULT_PRESET_SOURCE_URL)
        );
    }

    public static void setPresetSourceUrl(Context context, String value) {
        preferences(context).edit()
            .putString(KEY_PRESET_SOURCE_URL, normalizePresetSourceUrl(value))
            .apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static void applyThemeIfNeeded(String themeMode) {
        int nightMode;
        switch (themeMode) {
            case THEME_LIGHT:
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case THEME_DARK:
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case THEME_SYSTEM:
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode);
        }
    }

    private static void applyLanguageIfNeeded(String languageMode) {
        LocaleListCompat locales;
        switch (languageMode) {
            case LANGUAGE_ENGLISH:
                locales = LocaleListCompat.forLanguageTags("en");
                break;
            case LANGUAGE_KOREAN:
                locales = LocaleListCompat.forLanguageTags("ko");
                break;
            case LANGUAGE_JAPANESE:
                locales = LocaleListCompat.forLanguageTags("ja");
                break;
            case LANGUAGE_CHINESE_SIMPLIFIED:
                locales = LocaleListCompat.forLanguageTags("zh-CN");
                break;
            case LANGUAGE_DEFAULT:
            default:
                locales = LocaleListCompat.getEmptyLocaleList();
                break;
        }
        String currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String desiredTags = locales.toLanguageTags();
        if (!currentTags.equals(desiredTags)) {
            AppCompatDelegate.setApplicationLocales(locales);
        }
    }

    private static String normalizeTheme(String value) {
        if (THEME_LIGHT.equals(value)) {
            return THEME_LIGHT;
        }
        if (THEME_DARK.equals(value)) {
            return THEME_DARK;
        }
        return THEME_SYSTEM;
    }

    private static String normalizeLanguage(String value) {
        if (LANGUAGE_ENGLISH.equals(value)) {
            return LANGUAGE_ENGLISH;
        }
        if (LANGUAGE_KOREAN.equals(value)) {
            return LANGUAGE_KOREAN;
        }
        if (LANGUAGE_JAPANESE.equals(value)) {
            return LANGUAGE_JAPANESE;
        }
        if (LANGUAGE_CHINESE_SIMPLIFIED.equals(value)) {
            return LANGUAGE_CHINESE_SIMPLIFIED;
        }
        return LANGUAGE_DEFAULT;
    }

    private static String normalizeColorStyle(String value) {
        if (COLOR_STYLE_BLUE.equals(value)) {
            return COLOR_STYLE_BLUE;
        }
        if (COLOR_STYLE_ROSE.equals(value)) {
            return COLOR_STYLE_ROSE;
        }
        if (COLOR_STYLE_AMBER.equals(value)) {
            return COLOR_STYLE_AMBER;
        }
        return COLOR_STYLE_MINT;
    }

    private static String normalizePresetSourceUrl(String value) {
        if (value == null) {
            return DEFAULT_PRESET_SOURCE_URL;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? DEFAULT_PRESET_SOURCE_URL : trimmed;
    }

    private static int resolveStaticTheme(String colorStyle) {
        switch (colorStyle) {
            case COLOR_STYLE_BLUE:
                return R.style.Theme_DeviceSpoofLab_Blue;
            case COLOR_STYLE_ROSE:
                return R.style.Theme_DeviceSpoofLab_Rose;
            case COLOR_STYLE_AMBER:
                return R.style.Theme_DeviceSpoofLab_Amber;
            case COLOR_STYLE_MINT:
            default:
                return R.style.Theme_DeviceSpoofLab;
        }
    }
}
