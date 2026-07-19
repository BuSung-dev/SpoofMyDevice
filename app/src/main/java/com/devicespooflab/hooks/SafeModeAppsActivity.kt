package com.devicespooflab.hooks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import com.devicespooflab.hooks.data.AppSettingsStore
import com.devicespooflab.hooks.ui.SafeModeScreen
import com.devicespooflab.hooks.ui.SpoofAppState
import com.devicespooflab.hooks.ui.SpoofMyDeviceTheme
import java.util.LinkedHashSet

class SafeModeAppsActivity : AppCompatActivity() {

    private val selectedPackages = LinkedHashSet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettingsStore.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
        selectedPackages.addAll(intent.getStringArrayListExtra(EXTRA_SELECTED_PACKAGES).orEmpty())
        enableEdgeToEdge()
        setContent {
            val appState = remember { SpoofAppState(this) }
            SpoofMyDeviceTheme {
                SafeModeScreen(
                    appState = appState,
                    initialSelectedPackages = selectedPackages,
                    onSelectionChanged = {
                        selectedPackages.clear()
                        selectedPackages.addAll(it)
                    },
                    onBack = ::finish,
                )
            }
        }
    }

    override fun finish() {
        setResult(
            Activity.RESULT_OK,
            Intent().putStringArrayListExtra(
                EXTRA_RESULT_SELECTED_PACKAGES,
                ArrayList(selectedPackages),
            ),
        )
        super.finish()
    }

    companion object {
        const val EXTRA_SELECTED_PACKAGES = "selected_packages"
        const val EXTRA_RESULT_SELECTED_PACKAGES = "result_selected_packages"

        fun createIntent(context: Context, selectedPackages: Set<String>): Intent {
            return Intent(context, SafeModeAppsActivity::class.java).putStringArrayListExtra(
                EXTRA_SELECTED_PACKAGES,
                ArrayList(selectedPackages),
            )
        }
    }
}
