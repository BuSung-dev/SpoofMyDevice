package com.devicespooflab.hooks.hooks;

import android.telephony.TelephonyManager;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks TelephonyManager methods to spoof device identifiers.
 * Spoofs: IMEI, MEID, IMSI, ICCID, phone number
 */
public class TelephonyHooks {

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> telephonyManager = XposedHelpers.findClassIfExists(
                "android.telephony.TelephonyManager",
                lpparam.classLoader
        );

        if (telephonyManager == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getDeviceId",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getIMEI();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getDeviceId", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getIMEI();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getImei",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getIMEI();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getImei", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getIMEI();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getMeid",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getMEID();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getMeid", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getMEID();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSubscriberId",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getIMSI();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSubscriberId", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getIMSI();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimSerialNumber",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getICCID();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimSerialNumber", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getICCID();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getLine1Number",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getPhoneNumber();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getLine1Number", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getPhoneNumber();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getPhoneType",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(TelephonyManager.PHONE_TYPE_NONE);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "isVoiceCapable",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(false);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "isSmsCapable",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.hasTelephonySupport()) {
                                param.setResult(false);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        // Hook network operator methods (MCC/MNC)
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getNetworkOperator",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String mccMnc = ConfigManager.getSystemProperty("gsm.operator.numeric", null);
                            if (mccMnc != null) {
                                param.setResult(mccMnc);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getNetworkOperatorName",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String operatorName = ConfigManager.getSystemProperty("gsm.operator.alpha", null);
                            if (operatorName != null) {
                                param.setResult(operatorName);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimOperator",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String simMccMnc = ConfigManager.getSystemProperty("gsm.sim.operator.numeric", null);
                            if (simMccMnc != null) {
                                param.setResult(simMccMnc);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimOperatorName",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String simOperatorName = ConfigManager.getSystemProperty("gsm.sim.operator.alpha", null);
                            if (simOperatorName != null) {
                                param.setResult(simOperatorName);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimCountryIso",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String simCountry = ConfigManager.getSystemProperty("gsm.sim.operator.iso-country", null);
                            if (simCountry != null) {
                                param.setResult(simCountry);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getNetworkCountryIso",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String networkCountry = ConfigManager.getSystemProperty("gsm.operator.iso-country", null);
                            if (networkCountry != null) {
                                param.setResult(networkCountry);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }
}
