package com.devicespooflab.hooks;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.devicespooflab.hooks.hooks.AdvertisingIdHooks;
import com.devicespooflab.hooks.hooks.AppSetIdHooks;
import com.devicespooflab.hooks.hooks.BuildHooks;
import com.devicespooflab.hooks.hooks.DisplayHooks;
import com.devicespooflab.hooks.hooks.EmulatorDetectionHooks;
import com.devicespooflab.hooks.hooks.GetPropHooks;
import com.devicespooflab.hooks.hooks.HardwareHooks;
import com.devicespooflab.hooks.hooks.JavaSystemPropertyHooks;
import com.devicespooflab.hooks.hooks.LocaleHooks;
import com.devicespooflab.hooks.hooks.MediaDrmHooks;
import com.devicespooflab.hooks.hooks.PackageManagerHooks;
import com.devicespooflab.hooks.hooks.SettingsHooks;
import com.devicespooflab.hooks.hooks.SystemPropertiesHooks;
import com.devicespooflab.hooks.hooks.TelephonyHooks;
import com.devicespooflab.hooks.hooks.VendorSystemPropertiesHooks;
import com.devicespooflab.hooks.hooks.WebViewHooks;
import com.devicespooflab.hooks.utils.ConfigManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final int REAL_SDK_INT = Build.VERSION.SDK_INT;
    private static final Set<String> INITIALIZED_HOOK_PROCESSES = ConcurrentHashMap.newKeySet();
    private static final Set<String> CONFIG_RECEIVER_PROCESSES = ConcurrentHashMap.newKeySet();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log(TAG + ": Loading hooks for " + lpparam.packageName);

        if ("android".equals(lpparam.packageName)) {
            XposedBridge.log(TAG + ": Skipping Android system framework");
            return;
        }

        if (MODULE_PACKAGE.equals(lpparam.packageName)) {
            hookActivationCheck(lpparam);
            return;
        }

        hookApplicationAttachForInitialization(lpparam);

        try {
            ConfigManager.init(lpparam.packageName);
            if (!ConfigManager.hasAssignedProfile()) {
                XposedBridge.log(TAG + ": Profile unavailable before Application.attach; deferring hooks for " + lpparam.packageName);
                return;
            }
            XposedBridge.log(TAG + ": Config initialized successfully");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": Failed to init config: " + exception.getMessage());
            exception.printStackTrace();
            return;
        }

        initializeHooks(lpparam);
    }

    private void initializeHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        String processName = lpparam.processName == null ? lpparam.packageName : lpparam.processName;
        if (!INITIALIZED_HOOK_PROCESSES.add(processName)) {
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

        hookActivityLifecycleForReapply(lpparam);

        try {
            LocaleHooks.hook(lpparam);
            XposedBridge.log(TAG + ": LocaleHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": LocaleHooks failed: " + exception.getMessage());
        }

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

        if (REAL_SDK_INT >= 30) {
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

    private void hookApplicationAttachForInitialization(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                null,
                "attach",
                android.content.Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            Context context = (Context) param.args[0];
                            ConfigManager.forceReload(context);
                            registerConfigChangeReceiver(context, lpparam);
                            if (!ConfigManager.hasAssignedProfile()) {
                                XposedBridge.log(TAG + ": No profile assigned after Application.attach for " + lpparam.packageName);
                                return;
                            }
                            initializeHooks(lpparam);
                            BuildHooks.reapply(context.getClassLoader(), lpparam.packageName);
                            XposedBridge.log(TAG + ": Profile loaded after Application.attach for " + lpparam.packageName);
                        } catch (Throwable throwable) {
                            XposedBridge.log(TAG + ": Failed to initialize hooks after attach for " + lpparam.packageName + ": " + throwable.getMessage());
                        }
                    }
                }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook Application.attach initialization path: " + throwable.getMessage());
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerConfigChangeReceiver(Context context, XC_LoadPackage.LoadPackageParam lpparam) {
        String processName = lpparam.processName == null ? lpparam.packageName : lpparam.processName;
        if (!CONFIG_RECEIVER_PROCESSES.add(processName)) {
            return;
        }

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                if (intent == null || !ConfigManager.ACTION_CONFIG_CHANGED.equals(intent.getAction())) {
                    return;
                }
                ConfigManager.forceReload(receiverContext);
                if (!ConfigManager.hasAssignedProfile()) {
                    return;
                }
                initializeHooks(lpparam);
                BuildHooks.reapply(receiverContext.getClassLoader(), lpparam.packageName);
            }
        };

        IntentFilter filter = new IntentFilter(ConfigManager.ACTION_CONFIG_CHANGED);
        if (REAL_SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
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
