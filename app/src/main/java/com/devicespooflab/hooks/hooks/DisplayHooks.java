package com.devicespooflab.hooks.hooks;

import android.content.res.Configuration;
import android.graphics.Point;
import android.util.DisplayMetrics;

import com.devicespooflab.hooks.utils.ConfigManager;

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
                            point.x = ConfigManager.getScreenWidth();
                            point.y = ConfigManager.getScreenHeight();
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
                            point.x = ConfigManager.getScreenWidth();
                            point.y = ConfigManager.getScreenHeight();
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static void applyMetrics(DisplayMetrics metrics) {
        if (!ConfigManager.shouldApplyScreenMetrics()) {
            return;
        }
        metrics.widthPixels = ConfigManager.getScreenWidth();
        metrics.heightPixels = ConfigManager.getScreenHeight();
        metrics.densityDpi = ConfigManager.getScreenDensityDpi();
        metrics.density = ConfigManager.getScreenDensity();
        metrics.scaledDensity = ConfigManager.getScreenDensity();
        metrics.xdpi = metrics.densityDpi;
        metrics.ydpi = metrics.densityDpi;
    }

    private static void applyConfiguration(Configuration configuration) {
        if (!ConfigManager.shouldApplyScreenMetrics()) {
            return;
        }
        configuration.densityDpi = ConfigManager.getScreenDensityDpi();
        configuration.screenWidthDp = ConfigManager.getScreenWidthDp();
        configuration.screenHeightDp = ConfigManager.getScreenHeightDp();
        configuration.smallestScreenWidthDp = ConfigManager.getSmallestScreenWidthDp();
        configuration.orientation = ConfigManager.getScreenWidth() >= ConfigManager.getScreenHeight()
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
}
