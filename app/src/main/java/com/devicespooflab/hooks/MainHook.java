package com.devicespooflab.hooks;

import android.os.Build;

import com.devicespooflab.hooks.hooks.AdvertisingIdHooks;
import com.devicespooflab.hooks.hooks.AppSetIdHooks;
import com.devicespooflab.hooks.hooks.BuildHooks;
import com.devicespooflab.hooks.hooks.DisplayHooks;
import com.devicespooflab.hooks.hooks.EmulatorDetectionHooks;
import com.devicespooflab.hooks.hooks.GetPropHooks;
import com.devicespooflab.hooks.hooks.HardwareHooks;
import com.devicespooflab.hooks.hooks.JavaSystemPropertyHooks;
import com.devicespooflab.hooks.hooks.MediaDrmHooks;
import com.devicespooflab.hooks.hooks.PackageManagerHooks;
import com.devicespooflab.hooks.hooks.SettingsHooks;
import com.devicespooflab.hooks.hooks.SystemPropertiesHooks;
import com.devicespooflab.hooks.hooks.TelephonyHooks;
import com.devicespooflab.hooks.hooks.VendorSystemPropertiesHooks;
import com.devicespooflab.hooks.hooks.WebViewHooks;
import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Main entry point for DeviceSpoofLabs-Hooks LSPosed module.
 *
 * STANDALONE MODULE - No Magisk dependency.
 *
 * This module hooks Android APIs to spoof device identifiers and properties
 * that some apps read directly from Java APIs or SystemProperties.
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "SpoofMyDevice";
    private static final String MODULE_PACKAGE = "com.spoofmydevice";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log(TAG + ": Loading hooks for " + lpparam.packageName);

        if (MODULE_PACKAGE.equals(lpparam.packageName)) {
            hookActivationCheck(lpparam);
        }

        try {
            ConfigManager.init();
            XposedBridge.log(TAG + ": Config initialized successfully");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": Failed to init config: " + exception.getMessage());
            exception.printStackTrace();
            return;
        }

        try {
            SystemPropertiesHooks.hook(lpparam);
            XposedBridge.log(TAG + ": SystemPropertiesHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": SystemPropertiesHooks failed: " + exception.getMessage());
        }

        try {
            VendorSystemPropertiesHooks.hook(lpparam);
            XposedBridge.log(TAG + ": VendorSystemPropertiesHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": VendorSystemPropertiesHooks failed: " + exception.getMessage());
        }

        try {
            JavaSystemPropertyHooks.hook(lpparam);
            XposedBridge.log(TAG + ": JavaSystemPropertyHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": JavaSystemPropertyHooks failed: " + exception.getMessage());
        }

        try {
            GetPropHooks.hook(lpparam);
            XposedBridge.log(TAG + ": GetPropHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": GetPropHooks failed: " + exception.getMessage());
        }

        try {
            BuildHooks.hook(lpparam);
            XposedBridge.log(TAG + ": BuildHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": BuildHooks failed: " + exception.getMessage());
        }

        hookApplicationAttachForReload(lpparam);
        hookActivityLifecycleForReapply(lpparam);

        try {
            HardwareHooks.hook(lpparam);
            XposedBridge.log(TAG + ": HardwareHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": HardwareHooks failed: " + exception.getMessage());
        }

        try {
            DisplayHooks.hook(lpparam);
            XposedBridge.log(TAG + ": DisplayHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": DisplayHooks failed: " + exception.getMessage());
        }

        try {
            EmulatorDetectionHooks.hook(lpparam);
            XposedBridge.log(TAG + ": EmulatorDetectionHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": EmulatorDetectionHooks failed: " + exception.getMessage());
        }

        try {
            TelephonyHooks.hook(lpparam);
            XposedBridge.log(TAG + ": TelephonyHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": TelephonyHooks failed: " + exception.getMessage());
        }

        try {
            SettingsHooks.hook(lpparam);
            XposedBridge.log(TAG + ": SettingsHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": SettingsHooks failed: " + exception.getMessage());
        }

        try {
            AdvertisingIdHooks.hook(lpparam);
            XposedBridge.log(TAG + ": AdvertisingIdHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": AdvertisingIdHooks failed: " + exception.getMessage());
        }

        if (Build.VERSION.SDK_INT >= 30) {
            try {
                AppSetIdHooks.hook(lpparam);
                XposedBridge.log(TAG + ": AppSetIdHooks loaded");
            } catch (Exception exception) {
                XposedBridge.log(TAG + ": AppSetIdHooks failed: " + exception.getMessage());
            }
        }

        try {
            MediaDrmHooks.hook(lpparam);
            XposedBridge.log(TAG + ": MediaDrmHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": MediaDrmHooks failed: " + exception.getMessage());
        }

        try {
            WebViewHooks.hook(lpparam);
            XposedBridge.log(TAG + ": WebViewHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": WebViewHooks failed: " + exception.getMessage());
        }

        try {
            PackageManagerHooks.hook(lpparam);
            XposedBridge.log(TAG + ": PackageManagerHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": PackageManagerHooks failed: " + exception.getMessage());
        }

        XposedBridge.log(TAG + ": All hooks initialized for " + lpparam.packageName);
    }

    private void hookApplicationAttachForReload(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                null,
                "attach",
                android.content.Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            android.content.Context context = (android.content.Context) param.args[0];
                            ConfigManager.forceReload(context);
                            BuildHooks.reapply(((android.content.Context) param.args[0]).getClassLoader(), lpparam.packageName);
                            XposedBridge.log(TAG + ": Config reloaded after Application.attach for " + lpparam.packageName);
                        } catch (Throwable throwable) {
                            XposedBridge.log(TAG + ": Failed to reload config after attach for " + lpparam.packageName + ": " + throwable.getMessage());
                        }
                    }
                }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook Application.attach reload path: " + throwable.getMessage());
        }
    }

    private void hookActivityLifecycleForReapply(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                null,
                "onCreate",
                android.os.Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isUsingEmbeddedDefaults()) {
                                return;
                            }
                            android.app.Activity activity = (android.app.Activity) param.thisObject;
                            ConfigManager.forceReload(activity);
                            BuildHooks.reapply(activity.getClassLoader(), lpparam.packageName);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );
        } catch (Throwable ignored) {
        }
    }

    private void hookActivationCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                MainActivity.class.getName(),
                lpparam.classLoader,
                "isModuleActivated",
                XC_MethodReplacement.returnConstant(true)
            );
            XposedBridge.log(TAG + ": Activation check hooked for companion app");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook activation check: " + throwable.getMessage());
        }
    }
}
