package com.devicespooflab.hooks.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.devicespooflab.hooks.AppProfileAssignmentActivity
import com.devicespooflab.hooks.ProfileEditorActivity
import com.devicespooflab.hooks.R
import com.devicespooflab.hooks.utils.ConfigManager
import kotlinx.coroutines.launch

private data class EditorField(
    val fieldId: String,
    val labelRes: Int,
    val numeric: Boolean = false,
    val multiline: Boolean = false,
)

private val identityFields = listOf(
    EditorField(ConfigManager.FIELD_BRAND, R.string.settings_field_brand),
    EditorField(ConfigManager.FIELD_MANUFACTURER, R.string.settings_field_manufacturer),
    EditorField(ConfigManager.FIELD_MODEL, R.string.settings_field_model),
    EditorField(ConfigManager.FIELD_DEVICE, R.string.settings_field_device_codename),
    EditorField(ConfigManager.FIELD_PRODUCT, R.string.settings_field_product_name),
    EditorField(ConfigManager.FIELD_BOARD, R.string.settings_field_board),
    EditorField(ConfigManager.FIELD_HARDWARE, R.string.settings_field_hardware),
    EditorField(ConfigManager.FIELD_BOARD_PLATFORM, R.string.settings_field_board_platform),
)

private val buildFields = listOf(
    EditorField(ConfigManager.FIELD_ANDROID_RELEASE, R.string.settings_field_android_release),
    EditorField(ConfigManager.FIELD_SDK, R.string.settings_field_sdk_level, numeric = true),
    EditorField(ConfigManager.FIELD_SECURITY_PATCH, R.string.settings_field_security_patch),
    EditorField(ConfigManager.FIELD_BUILD_ID, R.string.settings_field_build_id),
    EditorField(ConfigManager.FIELD_BUILD_DISPLAY_ID, R.string.settings_field_build_display_id),
    EditorField(ConfigManager.FIELD_BUILD_INCREMENTAL, R.string.settings_field_build_incremental),
    EditorField(ConfigManager.FIELD_FINGERPRINT, R.string.settings_field_fingerprint, multiline = true),
    EditorField(ConfigManager.FIELD_SCREEN_WIDTH, R.string.settings_field_screen_width, numeric = true),
    EditorField(ConfigManager.FIELD_SCREEN_HEIGHT, R.string.settings_field_screen_height, numeric = true),
    EditorField(ConfigManager.FIELD_SCREEN_DENSITY, R.string.settings_field_screen_density, numeric = true),
)

private val networkFields = listOf(
    EditorField(ConfigManager.FIELD_OPERATOR_ALPHA, R.string.settings_field_operator_name),
    EditorField(ConfigManager.FIELD_OPERATOR_NUMERIC, R.string.settings_field_operator_numeric, numeric = true),
    EditorField(ConfigManager.FIELD_SIM_COUNTRY, R.string.settings_field_sim_country_iso),
    EditorField(ConfigManager.FIELD_TIMEZONE, R.string.settings_field_timezone),
    EditorField(ConfigManager.FIELD_LOCALE, R.string.settings_field_locale),
)

private val advancedFields = listOf(
    EditorField(ConfigManager.FIELD_ANDROID_ID, R.string.settings_field_device_id),
    EditorField(ConfigManager.FIELD_IMEI, R.string.settings_field_imei, numeric = true),
    EditorField(ConfigManager.FIELD_MEID, R.string.settings_field_meid),
    EditorField(ConfigManager.FIELD_IMSI, R.string.settings_field_imsi, numeric = true),
    EditorField(ConfigManager.FIELD_ICCID, R.string.settings_field_iccid, numeric = true),
    EditorField(ConfigManager.FIELD_PHONE_NUMBER, R.string.settings_field_phone_number),
    EditorField(ConfigManager.FIELD_GAID, R.string.settings_field_gaid),
    EditorField(ConfigManager.FIELD_GSF_ID, R.string.settings_field_gsf_id),
    EditorField(ConfigManager.FIELD_MEDIA_DRM_ID, R.string.settings_field_media_drm_id),
    EditorField(ConfigManager.FIELD_APP_SET_ID, R.string.settings_field_app_set_id),
)

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun AppProfilesScreen(appState: SpoofAppState, scaffoldPadding: PaddingValues) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val assignmentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        scope.launch { appState.reloadProfileState() }
    }

    LaunchedEffect(Unit) {
        appState.ensureInstalledAppsLoaded()
    }

    val visibleApps = remember(appState.installedApps, appState.hideSystemAppsInAppSettings) {
        if (appState.hideSystemAppsInAppSettings) {
            appState.installedApps.filterNot(SafeAppEntry::systemApp)
        } else {
            appState.installedApps
        }
    }
    AppSelectionList(
        apps = visibleApps,
        loading = !appState.installedAppsLoaded,
        scaffoldPadding = scaffoldPadding,
        profileName = appState::assignedProfileName,
        onSelect = { app ->
            assignmentLauncher.launch(
                AppProfileAssignmentActivity.createIntent(context, app.packageName),
            )
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun AppSelectionList(
    apps: List<SafeAppEntry>,
    loading: Boolean,
    scaffoldPadding: PaddingValues,
    profileName: (String) -> String?,
    onSelect: (SafeAppEntry) -> Unit,
) {
    if (loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LoadingIndicator(modifier = Modifier.progressSemantics())
                Spacer(Modifier.height(MdSpacing.sm))
                Text(
                    text = stringResource(R.string.app_profile_loading_apps),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            apps
        } else {
            apps.filter { app ->
                app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(horizontal = 20.dp),
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.app_profile_search)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
            )
            Spacer(Modifier.height(MdSpacing.sm))
        }
        itemsIndexed(
            items = filteredApps,
            key = { _, app -> app.packageName },
        ) { index, app ->
            val assignedProfile = profileName(app.packageName)
            SegmentedListItem(
                onClick = { onSelect(app) },
                shapes = connectedListItemShapes(index, filteredApps.size),
                modifier = if (index < filteredApps.lastIndex) {
                    Modifier.padding(bottom = ListItemDefaults.SegmentedGap)
                } else {
                    Modifier
                },
                leadingContent = {
                    Image(
                        bitmap = app.icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.medium),
                    )
                },
                supportingContent = {
                    Text(
                        text = app.packageName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    Text(
                        text = assignedProfile ?: stringResource(R.string.app_profile_unassigned),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (assignedProfile == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item { Spacer(Modifier.height(112.dp)) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun ProfileManagementScreen(
    appState: SpoofAppState,
    scaffoldPadding: PaddingValues,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profiles = appState.profileState?.profiles.orEmpty()
    var profileIdPendingDeletion by remember { mutableStateOf<String?>(null) }
    val editorLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val profileId = result.data?.getStringExtra(ProfileEditorActivity.EXTRA_RESULT_PROFILE_ID)
        scope.launch { appState.reloadProfileState(profileId) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(horizontal = 20.dp),
    ) {
        item { SectionTitle(stringResource(R.string.profile_management_list_title)) }
        itemsIndexed(
            items = profiles,
            key = { _, profile -> profile.id },
        ) { index, profile ->
            SegmentedListItem(
                onClick = {
                    editorLauncher.launch(ProfileEditorActivity.createEditIntent(context, profile.id))
                },
                shapes = connectedListItemShapes(index, profiles.size),
                modifier = if (index < profiles.lastIndex) {
                    Modifier.padding(bottom = ListItemDefaults.SegmentedGap)
                } else {
                    Modifier
                },
                leadingContent = {
                    Icon(Icons.Outlined.AccountBox, contentDescription = null)
                },
                supportingContent = {
                    Text(stringResource(R.string.app_profile_assignment_count, appState.profileAssignmentCount(profile.id)))
                },
                trailingContent = {
                    IconButton(
                        onClick = { profileIdPendingDeletion = profile.id },
                        enabled = profiles.size > 1,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.profile_delete),
                            tint = if (profiles.size > 1) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                    }
                },
                colors = ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(profile.name)
            }
        }
        item {
            FilledTonalButton(
                onClick = { editorLauncher.launch(ProfileEditorActivity.createCreateIntent(context)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MdSpacing.sm),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(MdSpacing.xs))
                Text(stringResource(R.string.app_profile_create))
            }
        }
        item { Spacer(Modifier.height(112.dp)) }
    }

    val profilePendingDeletion = profiles.firstOrNull { it.id == profileIdPendingDeletion }
    if (profilePendingDeletion != null) {
        val assignmentCount = appState.profileAssignmentCount(profilePendingDeletion.id)
        AlertDialog(
            onDismissRequest = { profileIdPendingDeletion = null },
            title = { Text(stringResource(R.string.profile_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.profile_delete_message,
                        profilePendingDeletion.name,
                        assignmentCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileIdPendingDeletion = null
                        scope.launch { appState.deleteProfile(profilePendingDeletion.id) }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.profile_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { profileIdPendingDeletion = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun ProfileEditorForm(
    appState: SpoofAppState,
    scaffoldPadding: PaddingValues,
    profileName: String,
    onProfileNameChange: (String) -> Unit,
) {
    val editor = appState.editor
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var advancedExpanded by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        scope.launch { appState.populateAdvancedDefaults() }
    }

    fun loadAdvancedValues() {
        val permissions = listOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS)
        val missing = permissions.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            scope.launch { appState.populateAdvancedDefaults() }
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(Unit) { listState.scrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DeviceSettingsSection(stringResource(R.string.profile_details_title)) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = onProfileNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.app_profile_name)) },
                    singleLine = true,
                )
            }
        }
        item {
            DeviceSettingsSection(stringResource(R.string.settings_section_preset_mode)) {
                PresetSelector(appState)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    val options = listOf(
                        Triple(false, R.string.mode_preset, Icons.AutoMirrored.Outlined.ViewList),
                        Triple(true, R.string.mode_custom, Icons.Outlined.Edit),
                    )
                    options.forEachIndexed { index, option ->
                        ToggleButton(
                            checked = editor.customMode == option.first,
                            onCheckedChange = { editor.setCustomMode(option.first, appState.presets) },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.RadioButton },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        ) {
                            Icon(
                                imageVector = option.third,
                                contentDescription = null,
                                modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                            )
                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                            Text(stringResource(option.second))
                        }
                    }
                }
            }
        }
        item {
            EditorSection(
                title = stringResource(R.string.settings_section_identity),
                fields = identityFields,
                editor = editor,
            )
        }
        item {
            EditorSection(
                title = stringResource(R.string.settings_section_build_display),
                fields = buildFields,
                editor = editor,
            )
        }
        item {
            EditorSection(
                title = stringResource(R.string.settings_section_network_region),
                fields = networkFields,
                editor = editor,
            )
        }
        item {
            DeviceSettingsSection(stringResource(R.string.settings_section_advanced)) {
                Text(
                    text = stringResource(R.string.settings_advanced_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = {
                        advancedExpanded = !advancedExpanded
                        if (advancedExpanded) loadAdvancedValues()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (advancedExpanded) R.string.advanced_hide else R.string.advanced_show))
                }
                AnimatedVisibility(advancedExpanded) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(
                                onClick = editor::randomizeAll,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Casino,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.settings_generate_all))
                            }
                            FilledTonalButton(
                                onClick = editor::clearAdvanced,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.settings_clear_all))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        advancedFields.forEach { field ->
                            AdvancedEditorField(field, editor)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(MdSpacing.xl)) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PresetSelector(appState: SpoofAppState) {
    var expanded by remember { mutableStateOf(false) }
    val editor = appState.editor
    val selectedLabel = appState.presets.firstOrNull { it.id == editor.selectedPresetId }?.displayName.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_hint_preset)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            appState.presets.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(preset.displayName)
                            if (preset.summary.isNotBlank()) {
                                Text(
                                    preset.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        editor.selectPreset(preset)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EditorSection(title: String, fields: List<EditorField>, editor: ProfileEditorState) {
    DeviceSettingsSection(title) {
        fields.forEach { field ->
            ProfileEditorField(field, editor)
        }
    }
}

@Composable
private fun DeviceSettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MdSpacing.xxs),
            content = content,
        )
    }
}

@Composable
private fun ProfileEditorField(field: EditorField, editor: ProfileEditorState) {
    val spoofEnabled = editor.enabledFields[field.fieldId] != false
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = spoofEnabled,
            onCheckedChange = { editor.enabledFields[field.fieldId] = it },
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = editor.values[field.fieldId].orEmpty(),
            onValueChange = { editor.values[field.fieldId] = it },
            label = { Text(stringResource(field.labelRes)) },
            enabled = spoofEnabled && editor.customMode,
            minLines = if (field.multiline) 2 else 1,
            keyboardOptions = KeyboardOptions(keyboardType = if (field.numeric) KeyboardType.Number else KeyboardType.Text),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AdvancedEditorField(field: EditorField, editor: ProfileEditorState) {
    val spoofEnabled = editor.enabledFields[field.fieldId] != false
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = spoofEnabled,
            onCheckedChange = { editor.enabledFields[field.fieldId] = it },
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = editor.advancedValues[field.fieldId].orEmpty(),
            onValueChange = { editor.advancedValues[field.fieldId] = it },
            label = { Text(stringResource(field.labelRes)) },
            enabled = spoofEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = if (field.numeric) KeyboardType.Number else KeyboardType.Text),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(
            onClick = { editor.randomize(field.fieldId) },
            enabled = spoofEnabled,
        ) {
            Icon(Icons.Filled.Casino, stringResource(R.string.settings_generate_random))
        }
    }
}
