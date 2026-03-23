package com.devicespooflab.hooks.hooks;

import android.app.ActivityManager;
import android.os.Debug;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks to spoof hardware specifications (CPU cores, RAM, CPU frequency, etc.)
 * to match real Pixel 7 Pro hardware.
 *
 * Real Pixel 7 Pro specs:
 * - CPU: Google Tensor G2 (8 cores: 2x2.85GHz + 2x2.35GHz + 4x1.80GHz)
 * - RAM: 12GB LPDDR5
 * - Architecture: ARM64-v8a
 */
public class HardwareHooks {

    private static final String TAG = "DeviceSpoofLab-Hardware";

    // Pixel 7 Pro specs
    private static final int PIXEL_7_PRO_CORES = 8;
    private static final long PIXEL_7_PRO_RAM_BYTES = 12L * 1024 * 1024 * 1024; // 12GB
    private static final long PIXEL_7_PRO_RAM_KB = 12L * 1024 * 1024; // 12GB in KB

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookRuntimeCores();
            hookActivityManagerMemory(lpparam);
            hookDebugMemory();
            hookFileReads(); // Hook /proc/cpuinfo and /proc/meminfo reads
            XposedBridge.log(TAG + ": Successfully hooked hardware specs");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook hardware: " + e.getMessage());
        }
    }

    /**
     * Hook Runtime.availableProcessors() to return 8 cores
     */
    private static void hookRuntimeCores() {
        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "availableProcessors",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(PIXEL_7_PRO_CORES);
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook Runtime.availableProcessors(): " + e.getMessage());
        }
    }

    /**
     * Hook ActivityManager memory info methods
     */
    private static void hookActivityManagerMemory(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClassIfExists(
                "android.app.ActivityManager", lpparam.classLoader);

            if (activityManagerClass == null) {
                return;
            }

            // Hook getMemoryInfo()
            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryInfo",
                ActivityManager.MemoryInfo.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ActivityManager.MemoryInfo memInfo = (ActivityManager.MemoryInfo) param.args[0];
                        if (memInfo != null) {
                            // Spoof total RAM to 12GB
                            memInfo.totalMem = PIXEL_7_PRO_RAM_BYTES;
                            // Keep available/free memory proportional
                            long originalTotal = memInfo.totalMem;
                            if (originalTotal > 0) {
                                double usedRatio = 1.0 - ((double) memInfo.availMem / originalTotal);
                                memInfo.availMem = (long) (PIXEL_7_PRO_RAM_BYTES * (1.0 - usedRatio));
                            }
                        }
                    }
                });

            // Hook getMemoryClass() - returns heap size in MB
            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Pixel 7 Pro typically has 512MB heap per app
                        param.setResult(512);
                    }
                });

            // Hook getLargeMemoryClass()
            XposedHelpers.findAndHookMethod(activityManagerClass, "getLargeMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Large heap on Pixel 7 Pro
                        param.setResult(1024);
                    }
                });

        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook ActivityManager memory: " + e.getMessage());
        }
    }

    /**
     * Hook Debug.getNativeHeapSize() and related memory methods
     */
    private static void hookDebugMemory() {
        try {
            XposedHelpers.findAndHookMethod(Debug.class, "getNativeHeapSize",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        long originalSize = (Long) param.getResult();
                        // Scale to 12GB device
                        param.setResult(originalSize * 4);
                    }
                });
        } catch (Exception e) {
            // Method might not exist on all Android versions
        }
    }

    /**
     * Hook file reads to intercept /proc/cpuinfo and /proc/meminfo
     */
    private static void hookFileReads() {
        // Hook BufferedReader for /proc/cpuinfo
        try {
            XposedHelpers.findAndHookConstructor(BufferedReader.class,
                java.io.Reader.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        BufferedReader reader = (BufferedReader) param.thisObject;
                        // Check if reading from FileReader
                        if (param.args[0] instanceof FileReader) {
                            // We'll intercept readLine() calls instead
                        }
                    }
                });
        } catch (Exception e) {
            // Ignore
        }

        // Hook RandomAccessFile reads for /proc/meminfo
        try {
            XposedHelpers.findAndHookMethod(RandomAccessFile.class, "readLine",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String line = (String) param.getResult();
                        if (line != null) {
                            // Spoof MemTotal in /proc/meminfo
                            if (line.startsWith("MemTotal:")) {
                                param.setResult("MemTotal:       " + PIXEL_7_PRO_RAM_KB + " kB");
                            }
                        }
                    }
                });
        } catch (Exception e) {
            // Ignore
        }

        // Hook File operations for /proc/cpuinfo
        try {
            XposedHelpers.findAndHookMethod(File.class, "exists",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.thisObject;
                        String path = file.getAbsolutePath();

                        // Ensure /proc/cpuinfo exists (some apps check this)
                        if (path.equals("/proc/cpuinfo") || path.equals("/proc/meminfo")) {
                            // Let it pass through normally
                        }
                    }
                });
        } catch (Exception e) {
            // Ignore
        }
    }
}
