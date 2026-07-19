package com.devicespooflab.hooks

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.devicespooflab.hooks.data.AppSettingsStore
import com.devicespooflab.hooks.ui.DiagnosticsScreen
import com.devicespooflab.hooks.ui.SpoofAppState
import com.devicespooflab.hooks.ui.SpoofMyDeviceTheme

class DiagnosticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettingsStore.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appState = remember { SpoofAppState(this) }
            LaunchedEffect(appState) { appState.initialize() }
            SpoofMyDeviceTheme {
                DiagnosticsScreen(appState, onBack = ::finish)
            }
        }
    }
}
