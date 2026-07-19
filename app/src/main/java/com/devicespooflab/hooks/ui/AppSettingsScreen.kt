package com.devicespooflab.hooks.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devicespooflab.hooks.DiagnosticsActivity
import com.devicespooflab.hooks.R
import com.devicespooflab.hooks.RealInfoActivity
import com.devicespooflab.hooks.SafeModeAppsActivity
import com.devicespooflab.hooks.data.AppSettingsStore
import kotlinx.coroutines.launch

private data class SettingOption(val id: String, val labelRes: Int)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppSettingsScreen(appState: SpoofAppState, scaffoldPadding: PaddingValues) {
    val context = appState.activity
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var optionDialog = rememberMutableString()
    var presetSourceDialog = rememberMutableBoolean()
    val safeModeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val packages = result.data
            ?.getStringArrayListExtra(SafeModeAppsActivity.EXTRA_RESULT_SELECTED_PACKAGES)
            ?.toSet()
            ?: emptySet()
        scope.launch { appState.updateSafeModePackages(packages) }
    }

    val themeOptions = listOf(
        SettingOption(AppSettingsStore.THEME_SYSTEM, R.string.theme_system),
        SettingOption(AppSettingsStore.THEME_LIGHT, R.string.theme_light),
        SettingOption(AppSettingsStore.THEME_DARK, R.string.theme_dark),
    )
    val themeIcons = listOf(
        Icons.Filled.BrightnessAuto,
        Icons.Filled.LightMode,
        Icons.Filled.DarkMode,
    )
    val languageOptions = listOf(
        SettingOption(AppSettingsStore.LANGUAGE_DEFAULT, R.string.language_default),
        SettingOption(AppSettingsStore.LANGUAGE_ENGLISH, R.string.language_english),
        SettingOption(AppSettingsStore.LANGUAGE_KOREAN, R.string.language_korean),
        SettingOption(AppSettingsStore.LANGUAGE_JAPANESE, R.string.language_japanese),
        SettingOption(AppSettingsStore.LANGUAGE_CHINESE_SIMPLIFIED, R.string.language_chinese),
    )
    val colorOptions = listOf(
        SettingOption(AppSettingsStore.COLOR_STYLE_MINT, R.string.color_style_mint),
        SettingOption(AppSettingsStore.COLOR_STYLE_BLUE, R.string.color_style_blue),
        SettingOption(AppSettingsStore.COLOR_STYLE_ROSE, R.string.color_style_rose),
        SettingOption(AppSettingsStore.COLOR_STYLE_AMBER, R.string.color_style_amber),
    )
    val systemColors = appState.systemColorsEnabled
    val appearanceCount = if (systemColors) 2 else 3
    LaunchedEffect(Unit) { listState.scrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(horizontal = 20.dp),
    ) {
        item { SectionTitle(stringResource(R.string.settings_appearance_title)) }
        item {
            val selectedTheme = appState.themeMode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                themeOptions.forEachIndexed { index, option ->
                    ToggleButton(
                        checked = selectedTheme == option.id,
                        onCheckedChange = {
                            appState.updateThemeMode(option.id)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            themeOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        Icon(
                            imageVector = themeIcons[index],
                            contentDescription = null,
                            modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(stringResource(option.labelRes))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            ConnectedSettingsRow(
                index = 0,
                count = appearanceCount,
                title = stringResource(R.string.settings_system_colors_title),
                summary = stringResource(R.string.settings_system_colors_summary),
                icon = Icons.Outlined.Palette,
                checked = systemColors,
                onCheckedChange = {
                    appState.updateSystemColors(it)
                },
            )
        }
        if (!systemColors) {
            item {
                ConnectedSettingsRow(
                    index = 1,
                    count = appearanceCount,
                    title = stringResource(R.string.settings_color_style_title),
                    summary = stringResource(R.string.settings_color_style_summary, optionLabel(colorOptions, appState.colorStyle)),
                    icon = Icons.Outlined.Palette,
                    trailingText = optionLabel(colorOptions, appState.colorStyle),
                    onClick = { optionDialog.value = "color" },
                )
            }
        }
        item {
            ConnectedSettingsRow(
                index = appearanceCount - 1,
                count = appearanceCount,
                title = stringResource(R.string.settings_language_title),
                summary = stringResource(R.string.settings_language_helper),
                icon = Icons.Outlined.Translate,
                trailingText = optionLabel(languageOptions, appState.languageMode),
                onClick = { optionDialog.value = "language" },
            )
        }

        item { SectionTitle(stringResource(R.string.settings_runtime_title), Modifier.padding(top = 22.dp)) }
        item {
            ConnectedSettingsRow(
                index = 0,
                count = 2,
                title = stringResource(R.string.settings_resolution_title),
                summary = stringResource(R.string.settings_resolution_summary),
                icon = Icons.Outlined.AspectRatio,
                checked = appState.isScreenMetricsEnabled(),
                onCheckedChange = { enabled -> scope.launch { appState.updateScreenMetrics(enabled) } },
            )
        }
        item {
            ConnectedSettingsRow(
                index = 1,
                count = 2,
                title = stringResource(R.string.settings_safe_mode_title),
                summary = if (appState.safeModePackages().isEmpty()) {
                    stringResource(R.string.settings_safe_mode_summary_disabled)
                } else {
                    stringResource(R.string.settings_safe_mode_summary_count, appState.safeModePackages().size)
                },
                icon = Icons.Outlined.Security,
                onClick = {
                    safeModeLauncher.launch(SafeModeAppsActivity.createIntent(context, appState.safeModePackages()))
                },
            )
        }

        item { SectionTitle(stringResource(R.string.settings_presets_title), Modifier.padding(top = 22.dp)) }
        item {
            ConnectedSettingsRow(
                index = 0,
                count = 1,
                title = stringResource(R.string.settings_preset_source_title),
                summary = AppSettingsStore.getPresetSourceUrl(context),
                icon = Icons.Outlined.Link,
                onClick = { presetSourceDialog.value = true },
            )
        }

        item { SectionTitle(stringResource(R.string.settings_tools_title), Modifier.padding(top = 22.dp)) }
        item {
            ConnectedSettingsRow(
                index = 0,
                count = 2,
                title = stringResource(R.string.settings_diagnostics_title),
                summary = stringResource(R.string.real_info_button_helper),
                icon = Icons.Outlined.BugReport,
                onClick = { context.startActivity(Intent(context, DiagnosticsActivity::class.java)) },
            )
        }
        item {
            ConnectedSettingsRow(
                index = 1,
                count = 2,
                title = stringResource(R.string.settings_real_info_title),
                summary = stringResource(R.string.settings_real_info_summary),
                icon = Icons.Outlined.Info,
                onClick = { context.startActivity(Intent(context, RealInfoActivity::class.java)) },
            )
        }
        item { Spacer(Modifier.height(104.dp)) }
    }

    when (optionDialog.value) {
        "language" -> OptionDialog(
            title = stringResource(R.string.settings_choose_language),
            options = languageOptions,
            selectedId = appState.languageMode,
            compactSpacing = true,
            onDismiss = { optionDialog.value = null },
            onSelect = {
                appState.updateLanguageMode(it)
                optionDialog.value = null
            },
        )
        "color" -> OptionDialog(
            title = stringResource(R.string.settings_choose_color_style),
            options = colorOptions,
            selectedId = appState.colorStyle,
            onDismiss = { optionDialog.value = null },
            onSelect = {
                appState.updateColorStyle(it)
                optionDialog.value = null
            },
        )
    }

    if (presetSourceDialog.value) {
        PresetSourceDialog(
            currentUrl = AppSettingsStore.getPresetSourceUrl(context),
            onDismiss = { presetSourceDialog.value = false },
            onSave = {
                presetSourceDialog.value = false
                scope.launch { appState.updatePresetSource(it) }
            },
        )
    }
}

@Composable
private fun rememberMutableString() = androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf<String?>(null)
}

@Composable
private fun rememberMutableBoolean() = androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(false)
}

@Composable
private fun optionLabel(options: List<SettingOption>, selectedId: String): String {
    return options.firstOrNull { it.id == selectedId }?.let { stringResource(it.labelRes) }.orEmpty()
}

@Composable
private fun OptionDialog(
    title: String,
    options: List<SettingOption>,
    selectedId: String,
    compactSpacing: Boolean = false,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.id) }
                            .padding(vertical = if (compactSpacing) 0.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option.id == selectedId, onClick = { onSelect(option.id) })
                        Text(stringResource(option.labelRes), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun PresetSourceDialog(currentUrl: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val defaultUrl = AppSettingsStore.DEFAULT_PRESET_SOURCE_URL
    val custom = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentUrl != defaultUrl) }
    val url = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(if (custom.value) currentUrl else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_preset_source_title)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { custom.value = false }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = !custom.value, onClick = { custom.value = false })
                    Text(stringResource(R.string.settings_preset_source_option_default))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { custom.value = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = custom.value, onClick = { custom.value = true })
                    Text(stringResource(R.string.settings_preset_source_option_custom))
                }
                if (custom.value) {
                    OutlinedTextField(
                        value = url.value,
                        onValueChange = { url.value = it },
                        label = { Text(stringResource(R.string.settings_preset_source_input_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(if (custom.value) url.value.trim() else defaultUrl) },
                enabled = !custom.value || url.value.isNotBlank(),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}
