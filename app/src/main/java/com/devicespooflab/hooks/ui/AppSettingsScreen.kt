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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
fun AppSettingsScreen(
    appState: SpoofAppState,
    scaffoldPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
) {
    val context = appState.activity
    val scope = rememberCoroutineScope()
    var optionDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var presetSourceDialog by rememberSaveable { mutableStateOf(false) }
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

    // These option tables are constant; rebuilding them on every recomposition would
    // allocate fresh objects inside a scrolling list and defeat Compose's skipping.
    val themeOptions = remember {
        listOf(
            SettingOption(AppSettingsStore.THEME_SYSTEM, R.string.theme_system),
            SettingOption(AppSettingsStore.THEME_LIGHT, R.string.theme_light),
            SettingOption(AppSettingsStore.THEME_DARK, R.string.theme_dark),
        )
    }
    val themeIcons = remember {
        listOf(
            Icons.Filled.BrightnessAuto,
            Icons.Filled.LightMode,
            Icons.Filled.DarkMode,
        )
    }
    val languageOptions = remember {
        listOf(
            SettingOption(AppSettingsStore.LANGUAGE_DEFAULT, R.string.language_default),
            SettingOption(AppSettingsStore.LANGUAGE_ENGLISH, R.string.language_english),
            SettingOption(AppSettingsStore.LANGUAGE_KOREAN, R.string.language_korean),
            SettingOption(AppSettingsStore.LANGUAGE_JAPANESE, R.string.language_japanese),
            SettingOption(AppSettingsStore.LANGUAGE_CHINESE_SIMPLIFIED, R.string.language_chinese),
        )
    }
    val colorOptions = remember {
        listOf(
            SettingOption(AppSettingsStore.COLOR_STYLE_MINT, R.string.color_style_mint),
            SettingOption(AppSettingsStore.COLOR_STYLE_BLUE, R.string.color_style_blue),
            SettingOption(AppSettingsStore.COLOR_STYLE_ROSE, R.string.color_style_rose),
            SettingOption(AppSettingsStore.COLOR_STYLE_AMBER, R.string.color_style_amber),
        )
    }
    val systemColors = appState.systemColorsEnabled
    val appearanceCount = if (systemColors) 2 else 3
    val safeModePackages = appState.safeModePackages()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(horizontal = 20.dp),
    ) {
        item(key = "appearance_title") { SectionTitle(stringResource(R.string.settings_appearance_title)) }
        item(key = "theme_toggle") {
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
        item(key = "system_colors") {
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
            item(key = "color_style") {
                ConnectedSettingsRow(
                    index = 1,
                    count = appearanceCount,
                    title = stringResource(R.string.settings_color_style_title),
                    summary = stringResource(R.string.settings_color_style_summary, optionLabel(colorOptions, appState.colorStyle)),
                    icon = Icons.Outlined.Palette,
                    trailingText = optionLabel(colorOptions, appState.colorStyle),
                    onClick = { optionDialog = "color" },
                )
            }
        }
        item(key = "language") {
            ConnectedSettingsRow(
                index = appearanceCount - 1,
                count = appearanceCount,
                title = stringResource(R.string.settings_language_title),
                summary = stringResource(R.string.settings_language_helper),
                icon = Icons.Outlined.Translate,
                trailingText = optionLabel(languageOptions, appState.languageMode),
                onClick = { optionDialog = "language" },
            )
        }

        item(key = "runtime_title") { SectionTitle(stringResource(R.string.settings_runtime_title), Modifier.padding(top = 22.dp)) }
        item(key = "resolution") {
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
        item(key = "safe_mode") {
            ConnectedSettingsRow(
                index = 1,
                count = 2,
                title = stringResource(R.string.settings_safe_mode_title),
                summary = if (safeModePackages.isEmpty()) {
                    stringResource(R.string.settings_safe_mode_summary_disabled)
                } else {
                    stringResource(R.string.settings_safe_mode_summary_count, safeModePackages.size)
                },
                icon = Icons.Outlined.Security,
                onClick = {
                    safeModeLauncher.launch(SafeModeAppsActivity.createIntent(context, safeModePackages))
                },
            )
        }

        item(key = "presets_title") { SectionTitle(stringResource(R.string.settings_presets_title), Modifier.padding(top = 22.dp)) }
        item(key = "preset_source") {
            ConnectedSettingsRow(
                index = 0,
                count = 1,
                title = stringResource(R.string.settings_preset_source_title),
                summary = appState.presetSourceUrl,
                icon = Icons.Outlined.Link,
                onClick = { presetSourceDialog = true },
            )
        }

        item(key = "tools_title") { SectionTitle(stringResource(R.string.settings_tools_title), Modifier.padding(top = 22.dp)) }
        item(key = "diagnostics") {
            ConnectedSettingsRow(
                index = 0,
                count = 2,
                title = stringResource(R.string.settings_diagnostics_title),
                summary = stringResource(R.string.real_info_button_helper),
                icon = Icons.Outlined.BugReport,
                onClick = { context.startActivity(Intent(context, DiagnosticsActivity::class.java)) },
            )
        }
        item(key = "real_info") {
            ConnectedSettingsRow(
                index = 1,
                count = 2,
                title = stringResource(R.string.settings_real_info_title),
                summary = stringResource(R.string.settings_real_info_summary),
                icon = Icons.Outlined.Info,
                onClick = { context.startActivity(Intent(context, RealInfoActivity::class.java)) },
            )
        }
        item(key = "bottom_gap") { Spacer(Modifier.height(104.dp)) }
    }

    when (optionDialog) {
        "language" -> OptionDialog(
            title = stringResource(R.string.settings_choose_language),
            options = languageOptions,
            selectedId = appState.languageMode,
            compactSpacing = true,
            onDismiss = { optionDialog = null },
            onSelect = {
                appState.updateLanguageMode(it)
                optionDialog = null
            },
        )
        "color" -> OptionDialog(
            title = stringResource(R.string.settings_choose_color_style),
            options = colorOptions,
            selectedId = appState.colorStyle,
            onDismiss = { optionDialog = null },
            onSelect = {
                appState.updateColorStyle(it)
                optionDialog = null
            },
        )
    }

    if (presetSourceDialog) {
        PresetSourceDialog(
            currentUrl = appState.presetSourceUrl,
            onDismiss = { presetSourceDialog = false },
            onSave = {
                presetSourceDialog = false
                scope.launch { appState.updatePresetSource(it) }
            },
        )
    }
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
    var custom by rememberSaveable { mutableStateOf(currentUrl != defaultUrl) }
    var url by rememberSaveable { mutableStateOf(if (currentUrl != defaultUrl) currentUrl else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_preset_source_title)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { custom = false }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = !custom, onClick = { custom = false })
                    Text(stringResource(R.string.settings_preset_source_option_default))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { custom = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = custom, onClick = { custom = true })
                    Text(stringResource(R.string.settings_preset_source_option_custom))
                }
                if (custom) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.settings_preset_source_input_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(if (custom) url.trim() else defaultUrl) },
                enabled = !custom || url.isNotBlank(),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}
