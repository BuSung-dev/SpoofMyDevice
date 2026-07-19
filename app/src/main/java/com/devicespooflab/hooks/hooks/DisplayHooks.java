package com.devicespooflab.hooks.hooks;

import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks display and resource metrics so apps can see the configured tablet/phone size class.
 */
public class DisplayHooks {

    private static final String TAG = "DeviceSpoofLab-Display";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        hookResourcesMetrics();
        hookDisplayMetrics(lpparam);
        hookWindowMetrics(lpparam);
    }

    private static void hookResourcesMetrics() {
        try {
            XposedHelpers.findAndHookMethod("android.content.res.Resources", null, "getDisplayMetrics",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        DisplayMetrics metrics = (DisplayMetrics) param.getResult();
                        if (metrics != null) {
                            applyMetrics(metrics);
                        }
                    }
                });
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook Resources.getDisplayMetrics(): " + throwable.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod("android.content.res.Resources", null, "getConfiguration",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Configuration configuration = (Configuration) param.getResult();
                        if (configuration != null) {
                            applyConfiguration(configuration);
                        }
                    }
                });
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook Resources.getConfiguration(): " + throwable.getMessage());
        }
    }

    private static void hookDisplayMetrics(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> displayClass = XposedHelpers.findClassIfExists("android.view.Display", lpparam.classLoader);
        if (displayClass == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(displayClass, "getMetrics", DisplayMetrics.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        DisplayMetrics metrics = (DisplayMetrics) param.args[0];
                        if (metrics != null) {
                            applyMetrics(metrics);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(displayClass, "getRealMetrics", DisplayMetrics.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        DisplayMetrics metrics = (DisplayMetrics) param.args[0];
                        if (metrics != null) {
                            applyMetrics(metrics);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(displayClass, "getSize", Point.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Point point = (Point) param.args[0];
                        if (point != null && ConfigManager.shouldApplyScreenMetrics()) {
                            if (ConfigManager.isSpoofEnabled(ConfigManager.FIELD_SCREEN_WIDTH)) {
                                point.x = ConfigManager.getScreenWidth();
                            }
                            if (ConfigManager.isSpoofEnabled(ConfigManager.FIELD_SCREEN_HEIGHT)) {
                                point.y = ConfigManager.getScreenHeight();
                            }
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(displayClass, "getRealSize", Point.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Point point = (Point) param.args[0];
                        if (point != null && ConfigManager.shouldApplyScreenMetrics()) {
                            if (ConfigManager.isSpoofEnabled(ConfigManager.FIELD_SCREEN_WIDTH)) {
                                point.x = ConfigManager.getScreenWidth();
                            }
                            if (ConfigManager.isSpoofEnabled(ConfigManager.FIELD_SCREEN_HEIGHT)) {
                                point.y = ConfigManager.getScreenHeight();
                            }
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        hookDisplayDimension(displayClass, "getWidth", true);
        hookDisplayDimension(displayClass, "getHeight", false);

        try {
            XposedHelpers.findAndHookMethod(displayClass, "getRectSize", Rect.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!ConfigManager.shouldApplyScreenMetrics()) {
                            return;
                        }
                        Rect rect = (Rect) param.args[0];
                        if (rect != null) {
                            applyBounds(rect);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(displayClass, "getCurrentSizeRange", Point.class, Point.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!ConfigManager.shouldApplyScreenMetrics()) {
                            return;
                        }
                        Point smallest = (Point) param.args[0];
                        Point largest = (Point) param.args[1];
                        int shortSide = Math.min(ConfigManager.getScreenWidth(), ConfigManager.getScreenHeight());
                        int longSide = Math.max(ConfigManager.getScreenWidth(), ConfigManager.getScreenHeight());
                        if (smallest != null) {
                            smallest.set(shortSide, shortSide);
                        }
                        if (largest != null) {
                            largest.set(longSide, longSide);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static void hookDisplayDimension(Class<?> displayClass, String methodName, boolean width) {
        try {
            XposedHelpers.findAndHookMethod(displayClass, methodName,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!ConfigManager.shouldApplyScreenMetrics()) {
                            return;
                        }
                        String field = width ? ConfigManager.FIELD_SCREEN_WIDTH : ConfigManager.FIELD_SCREEN_HEIGHT;
                        if (ConfigManager.isSpoofEnabled(field)) {
                            param.setResult(width ? ConfigManager.getScreenWidth() : ConfigManager.getScreenHeight());
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static void hookWindowMetrics(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> windowMetricsClass = XposedHelpers.findClassIfExists("android.view.WindowMetrics", lpparam.classLoader);
        if (windowMetricsClass == null) {
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(windowMetricsClass, "getBounds",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!ConfigManager.shouldApplyScreenMetrics()) {
                            return;
                        }
                        Rect bounds = (Rect) param.getResult();
                        if (bounds != null) {
                            Rect spoofedBounds = new Rect(bounds);
                            applyBounds(spoofedBounds);
                            param.setResult(spoofedBounds);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static void applyBounds(Rect bounds) {
        int width = ConfigManager.isSpoofEnabled(ConfigManager.FIELD_SCREEN_WIDTH)
            ? ConfigManager.getScreenWidth()
            : bounds.width();
        int height = ConfigManager.isSpoofEnabled(ConfigManager.FIELD_SCREEN_HEIGHT)
            ? ConfigManager.getScreenHeight()
            : bounds.height();
        bounds.right = bounds.left + width;
        bounds.bottom = bounds.top + height;
    }

    private static void applyMetrics(DisplayMetrics metrics) {
        if (!ConfigManager.shouldApplyScreenMetrics()) {
            return;
        }
        if (ConfigManager.isSpoofEnabled(ConfigManager.FIELD_SCREEN_WIDTH)) {
            metrics.widthPixels = ConfigManager.getScreenWidth();
        }
        if (ConfigManager.isSpoofEnabled(ConfigManager.FIELD_SCREEN_HEIGHT)) {
            metrics.heightPixels = ConfigManager.getScreenHeight();
        }
        if (ConfigManager.isSpoofEnabled(ConfigManager.FIELD_SCREEN_DENSITY)) {
            metrics.densityDpi = ConfigManager.getScreenDensityDpi();
            metrics.density = ConfigManager.getScreenDensity();
            metrics.scaledDensity = ConfigManager.getScreenDensity();
            metrics.xdpi = metrics.densityDpi;
            metrics.ydpi = metrics.densityDpi;
        }
    }

    private static void applyConfiguration(Configuration configuration) {
        applyLocale(configuration);
        if (!ConfigManager.shouldApplyScreenMetrics()) {
            return;
        }
        boolean widthEnabled = ConfigManager.isSpoofEnabled(ConfigManager.FIELD_SCREEN_WIDTH);
        boolean heightEnabled = ConfigManager.isSpoofEnabled(ConfigManager.FIELD_SCREEN_HEIGHT);
        boolean densityEnabled = ConfigManager.isSpoofEnabled(ConfigManager.FIELD_SCREEN_DENSITY);

        if (!widthEnabled && !heightEnabled && !densityEnabled) {
            return;
        }

        int originalDensity = configuration.densityDpi > 0
            ? configuration.densityDpi
            : ConfigManager.getScreenDensityDpi();
        int effectiveDensity = densityEnabled
            ? ConfigManager.getScreenDensityDpi()
            : originalDensity;

        int widthPixels = widthEnabled
            ? ConfigManager.getScreenWidth()
            : Math.round(configuration.screenWidthDp * originalDensity / 160f);
        int heightPixels = heightEnabled
            ? ConfigManager.getScreenHeight()
            : Math.round(configuration.screenHeightDp * originalDensity / 160f);

        if (densityEnabled) {
            configuration.densityDpi = effectiveDensity;
        }
        if (widthEnabled || densityEnabled) {
            configuration.screenWidthDp = Math.round(widthPixels * 160f / effectiveDensity);
        }
        if (heightEnabled || densityEnabled) {
            configuration.screenHeightDp = Math.round(heightPixels * 160f / effectiveDensity);
        }

        configuration.smallestScreenWidthDp = Math.min(configuration.screenWidthDp, configuration.screenHeightDp);
        configuration.orientation = widthPixels >= heightPixels
            ? Configuration.ORIENTATION_LANDSCAPE
            : Configuration.ORIENTATION_PORTRAIT;

        int sizeMask;
        if (configuration.smallestScreenWidthDp >= 720) {
            sizeMask = Configuration.SCREENLAYOUT_SIZE_XLARGE;
        } else if (configuration.smallestScreenWidthDp >= 600) {
            sizeMask = Configuration.SCREENLAYOUT_SIZE_LARGE;
        } else if (configuration.smallestScreenWidthDp >= 480) {
            sizeMask = Configuration.SCREENLAYOUT_SIZE_NORMAL;
        } else {
            sizeMask = Configuration.SCREENLAYOUT_SIZE_SMALL;
        }
        configuration.screenLayout = (configuration.screenLayout & ~Configuration.SCREENLAYOUT_SIZE_MASK) | sizeMask;
    }

    private static void applyLocale(Configuration configuration) {
        String languageTag = ConfigManager.getLocaleTag();
        if (languageTag == null) {
            return;
        }
        Locale locale = Locale.forLanguageTag(languageTag.replace('_', '-'));
        if (locale.getLanguage().isEmpty()) {
            return;
        }
        configuration.setLocale(locale);
    }
}
