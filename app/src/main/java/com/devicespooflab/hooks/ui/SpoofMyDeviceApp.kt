package com.devicespooflab.hooks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devicespooflab.hooks.R
import androidx.core.content.pm.PackageInfoCompat

private enum class AppDestination(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    Profiles(R.string.tab_profile_management, Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts),
    Apps(R.string.tab_app_profiles, Icons.Filled.Apps, Icons.Outlined.Apps),
    Settings(R.string.tab_app_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
}

private enum class NavigationType {
    Bar,
    Rail,
    Drawer,
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
@Composable
fun SpoofMyDeviceApp(appState: SpoofAppState) {
    val snackbarHostState = remember { SnackbarHostState() }
    var destinationName by rememberSaveable { mutableStateOf(AppDestination.Home.name) }
    val destination = AppDestination.valueOf(destinationName)
    val message = appState.message
    var appsMenuExpanded by remember { mutableStateOf(false) }
    val windowSizeClass = calculateWindowSizeClass(appState.activity)
    val navigationType = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> NavigationType.Bar
        WindowWidthSizeClass.Medium -> NavigationType.Rail
        else -> NavigationType.Drawer
    }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            appState.consumeMessage()
        }
    }

    key(destination) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val packageInfo = remember { appState.activity.packageManager.getPackageInfo(appState.activity.packageName, 0) }
        val page: @Composable () -> Unit = {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LargeFlexibleTopAppBar(
                        title = {
                            Text(
                                stringResource(
                                    if (destination == AppDestination.Home) {
                                        R.string.home_title
                                    } else {
                                        destination.labelRes
                                    },
                                ),
                            )
                        },
                        subtitle = if (destination == AppDestination.Settings) {
                            {
                                Text(
                                    "${packageInfo.versionName.orEmpty()} " +
                                        "(${PackageInfoCompat.getLongVersionCode(packageInfo)})"
                                )
                            }
                        } else {
                            null
                        },
                        actions = {
                            if (destination == AppDestination.Apps) {
                                Box {
                                    androidx.compose.material3.IconButton(
                                        onClick = { appsMenuExpanded = true },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = stringResource(R.string.safe_mode_more_options),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = appsMenuExpanded,
                                        onDismissRequest = { appsMenuExpanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.safe_mode_hide_system_apps)) },
                                            leadingIcon = {
                                                Checkbox(
                                                    checked = appState.hideSystemAppsInAppSettings,
                                                    onCheckedChange = null,
                                                )
                                            },
                                            onClick = {
                                                appState.toggleSystemAppsInAppSettings()
                                                appsMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                    )
                },
                bottomBar = {
                    if (navigationType == NavigationType.Bar) {
                        NavigationBar {
                            AppDestination.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = item == destination,
                                    onClick = { destinationName = item.name },
                                    icon = {
                                        Icon(
                                            imageVector = if (item == destination) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = null,
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                )
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.surface,
            ) { contentPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (appState.initialized) {
                        when (destination) {
                            AppDestination.Home -> HomeScreen(appState, contentPadding)
                            AppDestination.Apps -> AppProfilesScreen(appState, contentPadding)
                            AppDestination.Profiles -> ProfileManagementScreen(appState, contentPadding)
                            AppDestination.Settings -> AppSettingsScreen(appState, contentPadding)
                        }
                    }
                }
            }
        }

        when (navigationType) {
            NavigationType.Bar -> page()
            NavigationType.Rail -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail {
                        AppDestination.entries.forEach { item ->
                            NavigationRailItem(
                                selected = item == destination,
                                onClick = { destinationName = item.name },
                                icon = {
                                    Icon(
                                        imageVector = if (item == destination) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = null,
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) { page() }
                }
            }
            NavigationType.Drawer -> {
                PermanentNavigationDrawer(
                    drawerContent = {
                        PermanentDrawerSheet(modifier = Modifier.width(320.dp)) {
                            Spacer(Modifier.height(MdSpacing.lg))
                            Text(
                                text = stringResource(R.string.app_name),
                                modifier = Modifier.padding(horizontal = MdSpacing.md),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(Modifier.height(MdSpacing.md))
                            AppDestination.entries.forEach { item ->
                                NavigationDrawerItem(
                                    selected = item == destination,
                                    onClick = { destinationName = item.name },
                                    icon = {
                                        Icon(
                                            imageVector = if (item == destination) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = null,
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                )
                            }
                        }
                    },
                    content = page,
                )
            }
        }
    }
}
