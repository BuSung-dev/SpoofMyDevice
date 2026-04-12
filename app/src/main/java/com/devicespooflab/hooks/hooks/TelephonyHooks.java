package com.devicespooflab.hooks.hooks;

import android.telephony.TelephonyManager;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks TelephonyManager methods to spoof device identifiers.
 * Spoofs: IMEI, MEID, IMSI, ICCID, phone number
 */
public class TelephonyHooks {

    private static final int SYNTHETIC_SUBSCRIPTION_ID = 1;
    private static final int SYNTHETIC_SIM_SLOT_INDEX = 0;
    private static final Set<Object> SYNTHETIC_SUBSCRIPTION_INFOS =
        Collections.newSetFromMap(new WeakHashMap<>());

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> telephonyManager = XposedHelpers.findClassIfExists(
                "android.telephony.TelephonyManager",
                lpparam.classLoader
        );
        Class<?> subscriptionManager = XposedHelpers.findClassIfExists(
                "android.telephony.SubscriptionManager",
                lpparam.classLoader
        );
        Class<?> subscriptionInfo = XposedHelpers.findClassIfExists(
                "android.telephony.SubscriptionInfo",
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
                            if (!ConfigManager.shouldExposeTelephony()) {
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
                            if (!ConfigManager.shouldExposeTelephony()) {
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
                            if (!ConfigManager.shouldExposeTelephony()) {
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
                            if (!ConfigManager.shouldExposeTelephony()) {
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
                            if (!ConfigManager.shouldExposeTelephony()) {
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
                            if (!ConfigManager.shouldExposeTelephony()) {
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
                            if (!ConfigManager.shouldExposeTelephony()) {
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
                            if (!ConfigManager.shouldExposeTelephony()) {
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
                            if (!ConfigManager.shouldExposeTelephony()) {
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
                            if (!ConfigManager.shouldExposeTelephony()) {
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
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String spoofedValue = ConfigManager.getPhoneNumber();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                                return;
                            }
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getLine1Number", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String spoofedValue = ConfigManager.getPhoneNumber();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                                return;
                            }
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getPhoneType",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(TelephonyManager.PHONE_TYPE_NONE);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(TelephonyManager.PHONE_TYPE_GSM);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "hasIccCard",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(false);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "hasIccCard", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(false);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimState",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(TelephonyManager.SIM_STATE_ABSENT);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(TelephonyManager.SIM_STATE_READY);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimState", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(TelephonyManager.SIM_STATE_ABSENT);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(TelephonyManager.SIM_STATE_READY);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getPhoneCount",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(0);
                                return;
                            }
                            param.setResult(1);
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimCount",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(0);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(1);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getActiveModemCount",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(0);
                                return;
                            }
                            param.setResult(1);
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimCardState",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(TelephonyManager.SIM_STATE_ABSENT);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(TelephonyManager.SIM_STATE_READY);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimApplicationState",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(TelephonyManager.SIM_STATE_ABSENT);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(TelephonyManager.SIM_STATE_READY);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSubscriptionId",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(SYNTHETIC_SUBSCRIPTION_ID);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "createForSubscriptionId",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony() || !ConfigManager.shouldReportSimPresent()) {
                                return;
                            }
                            Object manager = param.getResult();
                            if (manager != null) {
                                try {
                                    XposedHelpers.setIntField(manager, "mSubId", SYNTHETIC_SUBSCRIPTION_ID);
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "isVoiceCapable",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(false);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        if (subscriptionManager != null) {
            hookSubscriptionManager(subscriptionManager, subscriptionInfo);
        }
        if (subscriptionInfo != null) {
            hookSubscriptionInfo(subscriptionInfo);
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "isSmsCapable",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(false);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
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

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getVoiceMailNumber",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = firstNonBlank(ConfigManager.getPhoneNumber(), null);
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getDeviceSoftwareVersion",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = firstNonBlank(
                                ConfigManager.getBuildDisplay(),
                                ConfigManager.getBuildVersionIncremental()
                            );
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }

    private static void hookSubscriptionManager(Class<?> subscriptionManager, Class<?> subscriptionInfoClass) {
        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getActiveSubscriptionInfoCount",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(0);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(1);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getActiveSubscriptionInfoCountMax",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(0);
                                return;
                            }
                            param.setResult(1);
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getActiveSubscriptionIdList",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(new int[0]);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(new int[]{SYNTHETIC_SUBSCRIPTION_ID});
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        hookSubscriptionInfoListMethod(subscriptionManager, "getActiveSubscriptionInfoList", subscriptionInfoClass);
        hookSubscriptionInfoListMethod(subscriptionManager, "getCompleteActiveSubscriptionInfoList", subscriptionInfoClass);
        hookSubscriptionInfoListMethod(subscriptionManager, "getAccessibleSubscriptionInfoList", subscriptionInfoClass);
        hookSubscriptionInfoListMethod(subscriptionManager, "getAllSubscriptionInfoList", subscriptionInfoClass);

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getActiveSubscriptionInfo",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(createSyntheticSubscriptionInfo(subscriptionInfoClass));
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getActiveSubscriptionInfoForSimSlotIndex",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            int slotIndex = param.args[0] instanceof Integer ? (Integer) param.args[0] : 0;
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent() && slotIndex == SYNTHETIC_SIM_SLOT_INDEX) {
                                param.setResult(createSyntheticSubscriptionInfo(subscriptionInfoClass));
                            } else if (slotIndex != SYNTHETIC_SIM_SLOT_INDEX) {
                                param.setResult(null);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        hookStaticSubscriptionIds(subscriptionManager);
    }

    private static void hookSubscriptionInfoListMethod(Class<?> subscriptionManager, String methodName, Class<?> subscriptionInfoClass) {
        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(Collections.emptyList());
                                return;
                            }
                            if (!ConfigManager.shouldReportSimPresent()) {
                                return;
                            }
                            Object info = createSyntheticSubscriptionInfo(subscriptionInfoClass);
                            if (info == null) {
                                return;
                            }
                            List<Object> list = new ArrayList<>();
                            list.add(info);
                            param.setResult(list);
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }

    private static void hookStaticSubscriptionIds(Class<?> subscriptionManager) {
        hookStaticSubscriptionInt(subscriptionManager, "getDefaultSubscriptionId", SYNTHETIC_SUBSCRIPTION_ID);
        hookStaticSubscriptionInt(subscriptionManager, "getDefaultVoiceSubscriptionId", SYNTHETIC_SUBSCRIPTION_ID);
        hookStaticSubscriptionInt(subscriptionManager, "getDefaultSmsSubscriptionId", SYNTHETIC_SUBSCRIPTION_ID);
        hookStaticSubscriptionInt(subscriptionManager, "getDefaultDataSubscriptionId", SYNTHETIC_SUBSCRIPTION_ID);
        hookStaticSubscriptionInt(subscriptionManager, "getActiveDataSubscriptionId", SYNTHETIC_SUBSCRIPTION_ID);

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getSubscriptionId",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            int slotIndex = param.args[0] instanceof Integer ? (Integer) param.args[0] : 0;
                            if (ConfigManager.shouldExposeTelephony() && ConfigManager.shouldReportSimPresent()) {
                                param.setResult(slotIndex == SYNTHETIC_SIM_SLOT_INDEX
                                    ? SYNTHETIC_SUBSCRIPTION_ID
                                    : -1);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getSlotIndex",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (ConfigManager.shouldExposeTelephony() && ConfigManager.shouldReportSimPresent()) {
                                param.setResult(SYNTHETIC_SIM_SLOT_INDEX);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getSubscriptionIds",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            int slotIndex = param.args[0] instanceof Integer ? (Integer) param.args[0] : 0;
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(new int[0]);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(slotIndex == SYNTHETIC_SIM_SLOT_INDEX
                                    ? new int[]{SYNTHETIC_SUBSCRIPTION_ID}
                                    : new int[0]);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "isActiveSubscriptionId",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(false);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "isValidSubscriptionId",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (ConfigManager.shouldExposeTelephony() && ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "isUsableSubscriptionId",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (ConfigManager.shouldExposeTelephony() && ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }

    private static void hookStaticSubscriptionInt(Class<?> subscriptionManager, String methodName, int value) {
        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(value);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }

    private static void hookSubscriptionInfo(Class<?> subscriptionInfoClass) {
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getSubscriptionId", () -> SYNTHETIC_SUBSCRIPTION_ID);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getSimSlotIndex", () -> SYNTHETIC_SIM_SLOT_INDEX);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getCarrierId", () -> 0);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getDisplayName", () -> firstNonBlank(
                ConfigManager.getSystemProperty("gsm.sim.operator.alpha", null),
                ConfigManager.getSystemProperty("gsm.operator.alpha", null),
                "SIM 1"
        ));
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getCarrierName", () -> firstNonBlank(
                ConfigManager.getSystemProperty("gsm.sim.operator.alpha", null),
                ConfigManager.getSystemProperty("gsm.operator.alpha", null),
                "Carrier"
        ));
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getNumber", () -> firstNonBlank(ConfigManager.getPhoneNumber(), ""));
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getDataRoaming", () -> 0);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getMcc", TelephonyHooks::parseMcc);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getMnc", TelephonyHooks::parseMnc);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getMccString", TelephonyHooks::parseMccString);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getMncString", TelephonyHooks::parseMncString);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getCountryIso", () -> firstNonBlank(
                ConfigManager.getSystemProperty("gsm.sim.operator.iso-country", null),
                "us"
        ));
        hookSubscriptionInfoGetter(subscriptionInfoClass, "isEmbedded", () -> false);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "isOpportunistic", () -> false);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getSubscriptionType", () -> 0);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getCardId", () -> 0);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getPortIndex", () -> 0);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getUsageSetting", () -> 0);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getIccId", () -> firstNonBlank(ConfigManager.getICCID(), ""));
    }

    private static void hookSubscriptionInfoGetter(Class<?> subscriptionInfoClass, String methodName, ValueProvider valueProvider) {
        try {
            XposedHelpers.findAndHookMethod(subscriptionInfoClass, methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (SYNTHETIC_SUBSCRIPTION_INFOS.contains(param.thisObject)) {
                                param.setResult(valueProvider.get());
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }

    private static Object createSyntheticSubscriptionInfo(Class<?> subscriptionInfoClass) {
        if (subscriptionInfoClass == null) {
            return null;
        }
        try {
            Constructor<?> constructor = subscriptionInfoClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            SYNTHETIC_SUBSCRIPTION_INFOS.add(instance);
            return instance;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String firstNonBlank(String value, String fallback) {
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        return fallback;
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        String resolved = firstNonBlank(first, null);
        if (resolved != null && !resolved.isEmpty()) {
            return resolved;
        }
        resolved = firstNonBlank(second, null);
        if (resolved != null && !resolved.isEmpty()) {
            return resolved;
        }
        return fallback;
    }

    private static int parseMcc() {
        String mcc = parseMccString();
        try {
            return Integer.parseInt(mcc);
        } catch (Exception ignored) {
            return 310;
        }
    }

    private static int parseMnc() {
        String mnc = parseMncString();
        try {
            return Integer.parseInt(mnc);
        } catch (Exception ignored) {
            return 260;
        }
    }

    private static String parseMccString() {
        String numeric = firstNonBlank(
                ConfigManager.getSystemProperty("gsm.sim.operator.numeric", null),
                ConfigManager.getSystemProperty("gsm.operator.numeric", null),
                "310260"
        );
        if (numeric.length() >= 3) {
            return numeric.substring(0, 3);
        }
        return "310";
    }

    private static String parseMncString() {
        String numeric = firstNonBlank(
                ConfigManager.getSystemProperty("gsm.sim.operator.numeric", null),
                ConfigManager.getSystemProperty("gsm.operator.numeric", null),
                "310260"
        );
        if (numeric.length() > 3) {
            return numeric.substring(3);
        }
        return "260";
    }

    private interface ValueProvider {
        Object get();
    }
}
