package com.devicespooflab.hooks.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devicespooflab.hooks.R
import java.util.TimeZone

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiagnosticsScreen(appState: SpoofAppState, onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val configuration = LocalConfiguration.current
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings_diagnostics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.safe_mode_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        val config = appState.loadedConfig
        if (!appState.initialized || config == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val profile = config.profile
        val metrics = appState.activity.resources.displayMetrics
        val buildRows = listOf(
            stringResource(R.string.settings_field_brand) to Build.BRAND,
            stringResource(R.string.settings_field_manufacturer) to Build.MANUFACTURER,
            stringResource(R.string.settings_field_model) to Build.MODEL,
            stringResource(R.string.settings_field_device_codename) to Build.DEVICE,
            stringResource(R.string.settings_field_board) to Build.BOARD,
            stringResource(R.string.settings_field_hardware) to Build.HARDWARE,
            stringResource(R.string.settings_field_build_id) to Build.ID,
            stringResource(R.string.settings_field_build_display_id) to Build.DISPLAY,
            stringResource(R.string.settings_field_fingerprint) to Build.FINGERPRINT,
            stringResource(R.string.settings_field_android_release) to Build.VERSION.RELEASE,
            stringResource(R.string.settings_field_sdk_level) to Build.VERSION.SDK_INT.toString(),
            stringResource(R.string.settings_field_security_patch) to Build.VERSION.SECURITY_PATCH,
        )
        val runtimeRows = listOf(
            stringResource(R.string.home_info_profile_display_title) to stringResource(
                R.string.home_info_display_value_format,
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
            ),
            "ABI" to Build.SUPPORTED_ABIS.joinToString(", "),
            stringResource(R.string.settings_field_timezone) to TimeZone.getDefault().id,
            stringResource(R.string.settings_field_locale) to configuration.locales[0].toLanguageTag(),
        )
        val savedRows = listOf(
            stringResource(R.string.settings_hint_preset) to appState.presetLabel(config.selectedPresetId),
            stringResource(R.string.home_info_profile_mode_title) to stringResource(if (config.isCustomMode) R.string.mode_custom else R.string.mode_preset),
            stringResource(R.string.settings_field_model) to profile.model,
            stringResource(R.string.settings_field_device_codename) to profile.deviceCode,
            stringResource(R.string.settings_field_build_id) to profile.buildId,
            stringResource(R.string.settings_field_build_display_id) to profile.buildDisplayId,
            stringResource(R.string.settings_field_fingerprint) to profile.buildFingerprint,
            stringResource(R.string.settings_field_timezone) to profile.timezone,
            stringResource(R.string.settings_field_locale) to profile.locale,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
        ) {
            item { SectionTitle(stringResource(R.string.real_info_build_title)) }
            buildRows.forEachIndexed { index, row ->
                item { ConnectedInfoRow(index, buildRows.size, row.first, row.second) }
            }
            item { SectionTitle(stringResource(R.string.real_info_runtime_title), Modifier.padding(top = 22.dp)) }
            runtimeRows.forEachIndexed { index, row ->
                item { ConnectedInfoRow(index, runtimeRows.size, row.first, row.second) }
            }
            item { SectionTitle(stringResource(R.string.real_info_saved_title), Modifier.padding(top = 22.dp)) }
            savedRows.forEachIndexed { index, row ->
                item { ConnectedInfoRow(index, savedRows.size, row.first, row.second) }
            }
            item { Spacer(Modifier.height(36.dp)) }
        }
    }
}
