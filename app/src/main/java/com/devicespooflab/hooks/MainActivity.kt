package com.devicespooflab.hooks

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.devicespooflab.hooks.data.AppSettingsStore
import com.devicespooflab.hooks.ui.SpoofAppState
import com.devicespooflab.hooks.ui.SpoofMyDeviceApp
import com.devicespooflab.hooks.ui.SpoofMyDeviceTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettingsStore.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appState = remember { SpoofAppState(this) }
            LaunchedEffect(appState) {
                appState.initialize()
            }
            // Warm the installed-app list in the background so the Apps tab is usually
            // ready by the time it is opened. Runs in its own effect so it never delays
            // the first frame or the initial config load.
            LaunchedEffect(appState.initialized) {
                if (appState.initialized) {
                    runCatching { appState.ensureInstalledAppsLoaded() }
                }
            }
            SpoofMyDeviceTheme(
                themeMode = appState.themeMode,
                languageMode = appState.languageMode,
                useSystemColor = appState.systemColorsEnabled,
                colorStyle = appState.colorStyle,
            ) {
                SpoofMyDeviceApp(appState)
            }
        }
    }

    /**
     * Reports whether the Xposed module is loaded into this process.
     *
     * Always false as compiled. [MainHook] finds this method by name at runtime and
     * replaces it to return true, so a true result means the hook is live.
     *
     * The backing field is deliberately mutable and volatile. R8 cannot see the runtime
     * caller, so a plain `= false` body is a constant it will propagate into call sites
     * before deleting the method outright, leaving nothing to hook and making the app
     * report itself inactive while the module is in fact working. Reading a volatile
     * field removes the constant, so no optimizer flags are needed to keep the method
     * alive. The keep rules in proguard-rules.pro guard the same method by name.
     */
    fun isModuleActivated(): Boolean = moduleActivationFlag

    private companion object {
        @Volatile
        @JvmStatic
        private var moduleActivationFlag: Boolean = false
    }
}
