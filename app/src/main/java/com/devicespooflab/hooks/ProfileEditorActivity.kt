package com.devicespooflab.hooks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.devicespooflab.hooks.data.AppSettingsStore
import com.devicespooflab.hooks.ui.ProfileEditorLoadingScreen
import com.devicespooflab.hooks.ui.ProfileEditorScreen
import com.devicespooflab.hooks.ui.SpoofAppState
import com.devicespooflab.hooks.ui.SpoofMyDeviceTheme
import kotlinx.coroutines.launch

class ProfileEditorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettingsStore.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val editingProfileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        setContent {
            val appState = remember { SpoofAppState(this) }
            val scope = rememberCoroutineScope()
            var ready by remember { mutableStateOf(false) }
            var profileName by rememberSaveable { mutableStateOf("") }

            LaunchedEffect(editingProfileId) {
                appState.initialize()
                if (!appState.initialized) {
                    finish()
                    return@LaunchedEffect
                }
                if (editingProfileId != null && !appState.selectProfile(editingProfileId)) {
                    finish()
                    return@LaunchedEffect
                }
                profileName = editingProfileId
                    ?.let { appState.profileState?.findProfile(it)?.name }
                    .orEmpty()
                ready = true
            }

            SpoofMyDeviceTheme(
                themeMode = appState.themeMode,
                languageMode = appState.languageMode,
                useSystemColor = appState.systemColorsEnabled,
                colorStyle = appState.colorStyle,
            ) {
                if (!ready) {
                    ProfileEditorLoadingScreen(
                        creating = editingProfileId == null,
                        onBack = ::finish,
                    )
                } else {
                    ProfileEditorScreen(
                        appState = appState,
                        creating = editingProfileId == null,
                        profileName = profileName,
                        onProfileNameChange = { profileName = it },
                        onSave = {
                            scope.launch {
                                val savedProfileId = if (editingProfileId == null) {
                                    appState.createProfile(profileName)
                                } else {
                                    appState.saveEditor(profileName)
                                }
                                if (savedProfileId != null) {
                                    setResult(
                                        Activity.RESULT_OK,
                                        Intent().putExtra(EXTRA_RESULT_PROFILE_ID, savedProfileId),
                                    )
                                    finish()
                                }
                            }
                        },
                        onBack = ::finish,
                    )
                }
            }
        }
    }

    companion object {
        private const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_RESULT_PROFILE_ID = "result_profile_id"

        fun createCreateIntent(context: Context): Intent {
            return Intent(context, ProfileEditorActivity::class.java)
        }

        fun createEditIntent(context: Context, profileId: String): Intent {
            return Intent(context, ProfileEditorActivity::class.java)
                .putExtra(EXTRA_PROFILE_ID, profileId)
        }
    }
}
