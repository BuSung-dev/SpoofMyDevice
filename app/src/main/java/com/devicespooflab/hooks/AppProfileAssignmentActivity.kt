package com.devicespooflab.hooks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.graphics.drawable.toBitmap
import com.devicespooflab.hooks.data.AppProfileStore
import com.devicespooflab.hooks.data.AppProfileSession
import com.devicespooflab.hooks.data.AppSettingsStore
import com.devicespooflab.hooks.data.ConfigFileManager
import com.devicespooflab.hooks.ui.AppProfileAssignmentScreen
import com.devicespooflab.hooks.ui.AppProfileAssignmentLoadingScreen
import com.devicespooflab.hooks.ui.SpoofMyDeviceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

class AppProfileAssignmentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettingsStore.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (targetPackage.isNullOrBlank()) {
            finish()
            return
        }

        setContent {
            var screenState by remember { mutableStateOf<AssignmentScreenState?>(null) }
            val scope = rememberCoroutineScope()
            val profileEditorLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    AppProfileSession.current()?.let { profiles ->
                        screenState = screenState?.copy(profiles = profiles)
                    }
                }
            }

            LaunchedEffect(targetPackage) {
                screenState = loadAssignmentState(targetPackage)
            }

            SpoofMyDeviceTheme {
                val state = screenState
                if (state == null) {
                    AppProfileAssignmentLoadingScreen(onBack = ::finish)
                } else {
                    AppProfileAssignmentScreen(
                        label = state.label,
                        packageName = state.packageName,
                        icon = state.icon,
                        profiles = state.profiles.profiles,
                        assignedProfileId = state.profiles.assignments[state.packageName],
                        onProfileSelected = { profileId ->
                            scope.launch {
                                val profiles = saveAssignment(state.packageName, profileId)
                                screenState = state.copy(profiles = profiles)
                                setResult(Activity.RESULT_OK)
                            }
                        },
                        onCreateProfile = {
                            profileEditorLauncher.launch(
                                ProfileEditorActivity.createCreateIntent(this@AppProfileAssignmentActivity),
                            )
                        },
                        onBack = ::finish,
                    )
                }
            }
        }
    }

    private suspend fun loadAssignmentState(packageName: String): AssignmentScreenState {
        return withContext(Dispatchers.IO) {
            val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            val store = AppProfileStore(ConfigFileManager())
            AssignmentScreenState(
                label = applicationInfo.loadLabel(packageManager).toString().ifBlank { packageName },
                packageName = packageName,
                icon = applicationInfo.loadIcon(packageManager).toBitmap(96, 96),
                profiles = AppProfileSession.getOrLoad(this@AppProfileAssignmentActivity, store),
            )
        }
    }

    private suspend fun saveAssignment(packageName: String, profileId: String?): AppProfileStore.State {
        return withContext(Dispatchers.IO) {
            val store = AppProfileStore(ConfigFileManager())
            val state = AppProfileSession.getOrLoad(this@AppProfileAssignmentActivity, store)
            val assignments = LinkedHashMap(state.assignments)
            if (profileId == null) {
                assignments.remove(packageName)
            } else {
                assignments[packageName] = profileId
            }
            val updatedState = AppProfileStore.State(
                state.profiles,
                assignments,
                state.globalProperties,
            )
            store.save(this@AppProfileAssignmentActivity, updatedState)
            AppProfileSession.update(updatedState)
            updatedState
        }
    }

    private data class AssignmentScreenState(
        val label: String,
        val packageName: String,
        val icon: Bitmap,
        val profiles: AppProfileStore.State,
    )

    companion object {
        private const val EXTRA_PACKAGE_NAME = "package_name"

        fun createIntent(context: Context, packageName: String): Intent {
            return Intent(context, AppProfileAssignmentActivity::class.java)
                .putExtra(EXTRA_PACKAGE_NAME, packageName)
        }
    }
}
