package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks Build.SERIAL and Build.getSerial() to spoof device serial number.
 *
 * NOTE: Only hooks SERIAL, NOT other Build.* fields like FINGERPRINT, MODEL, etc.
 * Those are handled by Magisk via resetprop and should NOT be hooked here.
 */
public class BuildSerialHooks {

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> buildClass = XposedHelpers.findClassIfExists(
                "android.os.Build",
                lpparam.classLoader
        );

        if (buildClass == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(buildClass, "getSerial",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(ConfigManager.getSerial());
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.setStaticObjectField(buildClass, "SERIAL", ConfigManager.getSerial());
        } catch (NoSuchFieldError ignored) {
        }
    }
}
