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

    fun isModuleActivated(): Boolean = false
}
