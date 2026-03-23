# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep Xposed entry point
-keep class com.devicespooflab.hooks.MainHook { *; }

# Keep all hook classes
-keep class com.devicespooflab.hooks.hooks.** { *; }

# Keep utility classes
-keep class com.devicespooflab.hooks.utils.** { *; }
