# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep Xposed entry point
-keep class com.devicespooflab.hooks.MainHook { *; }

# Keep the activation self-check. MainHook hooks this method by name at runtime
# to flip its result from false to true. R8 cannot see that caller, so without
# this rule the method is inlined and deleted, findAndHookMethod throws
# NoSuchMethodError into a swallowed catch, and the app reports "Module is not
# active" even though every other hook is working. MainActivity itself also
# reads a volatile field so there is no constant left to propagate.
-keepclassmembers class com.devicespooflab.hooks.MainActivity {
    boolean isModuleActivated();
}

# Keep all hook classes
-keep class com.devicespooflab.hooks.hooks.** { *; }

# Keep utility classes
-keep class com.devicespooflab.hooks.utils.** { *; }

# Keep the Xposed API itself. It is provided by the framework at runtime
# (compileOnly), so it must never be renamed or warned about.
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# Keep components reached from outside this APK. The provider and receiver are
# invoked by the system / other apps by name, so R8 must not strip or rename them.
-keep class com.devicespooflab.hooks.ConfigProvider { *; }
-keep class com.devicespooflab.hooks.ConfigBridgeReceiver { *; }

# Keep the config model types. These are read inside hooked processes and are
# serialized by field name, so renaming them would silently break spoofing.
-keep class com.devicespooflab.hooks.data.** { *; }

# GMS advertising/app-set IDs are resolved reflectively and are not compiled in.
-dontwarn com.google.android.gms.**
