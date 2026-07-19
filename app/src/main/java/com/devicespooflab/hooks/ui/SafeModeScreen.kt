package com.devicespooflab.hooks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devicespooflab.hooks.R
import com.devicespooflab.hooks.data.AppSettingsStore

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SafeModeScreen(
    appState: SpoofAppState,
    initialSelectedPackages: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onBack: () -> Unit,
) {
    val apps = remember { mutableStateListOf<SafeAppEntry>() }
    val selected = remember { mutableStateListOf<String>().apply { addAll(initialSelectedPackages) } }
    var loading by remember { mutableStateOf(true) }
    var hideSystemApps by remember { mutableStateOf(true) }
    var showOptions by remember { mutableStateOf(false) }
    var showExplanation by remember {
        mutableStateOf(!AppSettingsStore.isSafeModeExplanationDismissed(appState.activity))
    }
    var doNotShowAgain by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun toggle(packageName: String) {
        if (selected.contains(packageName)) {
            selected.remove(packageName)
        } else {
            selected.add(packageName)
        }
        onSelectionChanged(selected.toSet())
    }

    fun dismissExplanation() {
        if (doNotShowAgain) {
            AppSettingsStore.setSafeModeExplanationDismissed(appState.activity, true)
        }
        showExplanation = false
    }

    LaunchedEffect(Unit) {
        val selectedAtEntry = initialSelectedPackages
        apps.addAll(
            appState.loadInstalledApps().sortedWith(
                compareBy<SafeAppEntry> { !selectedAtEntry.contains(it.packageName) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
            )
        )
        loading = false
    }
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings_safe_mode_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.safe_mode_back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOptions = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.safe_mode_more_options),
                            )
                        }
                        DropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.safe_mode_hide_system_apps)) },
                                leadingIcon = {
                                    Checkbox(checked = hideSystemApps, onCheckedChange = null)
                                },
                                onClick = {
                                    hideSystemApps = !hideSystemApps
                                    showOptions = false
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        if (loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LoadingIndicator(modifier = Modifier.progressSemantics())
                Spacer(modifier = Modifier.height(MdSpacing.sm))
                Text(
                    text = stringResource(R.string.app_profile_loading_apps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        val visibleApps = apps.filter {
            !hideSystemApps || !it.systemApp || selected.contains(it.packageName)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = MdSpacing.sm,
                vertical = MdSpacing.sm,
            ),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MdSpacing.sm, vertical = MdSpacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.safe_mode_apps_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.safe_mode_selected_count, selected.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (visibleApps.isEmpty()) {
                item { Text(stringResource(R.string.safe_mode_empty), modifier = Modifier.padding(MdSpacing.sm)) }
            } else {
                itemsIndexed(visibleApps, key = { _, app -> app.packageName }) { index, app ->
                    val checked = selected.contains(app.packageName)
                    SegmentedListItem(
                        onClick = { toggle(app.packageName) },
                        shapes = ListItemDefaults.segmentedShapes(index, visibleApps.size),
                        modifier = if (index < visibleApps.lastIndex) {
                            Modifier.padding(bottom = ListItemDefaults.SegmentedGap)
                        } else {
                            Modifier
                        },
                        leadingContent = {
                            Image(
                                bitmap = app.icon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                            )
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (app.versionName.isBlank()) {
                                        stringResource(R.string.safe_mode_version_unknown)
                                    } else {
                                        stringResource(R.string.safe_mode_version_format, app.versionName)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        },
                        trailingContent = {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { toggle(app.packageName) },
                            )
                        },
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            trailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text(
                            text = app.label,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(MdSpacing.lg)) }
        }
    }

    if (showExplanation) {
        AlertDialog(
            onDismissRequest = ::dismissExplanation,
            title = { Text(stringResource(R.string.settings_safe_mode_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.safe_mode_explanation))
                    Spacer(Modifier.height(MdSpacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = doNotShowAgain,
                            onCheckedChange = { doNotShowAgain = it },
                        )
                        Text(stringResource(R.string.safe_mode_dont_show_again))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = ::dismissExplanation) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}
