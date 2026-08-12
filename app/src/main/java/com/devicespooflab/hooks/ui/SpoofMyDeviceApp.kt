package com.devicespooflab.hooks.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.devicespooflab.hooks.R
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.delay

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
    var navigationExpanded by rememberSaveable { mutableStateOf(false) }
    val windowSizeClass = calculateWindowSizeClass(appState.activity)
    val navigationType = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> NavigationType.Drawer
        else -> NavigationType.Bar
    }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            appState.consumeMessage()
        }
    }

    // One top bar state per destination, so each tab keeps its own collapsed/expanded
    // app bar. The screens' scroll positions need no such hoisting: RetainedTabs never
    // discards a tab's composition, so each list keeps its own state.
    val topBarStates = AppDestination.entries.associateWith { rememberTopAppBarState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = topBarStates.getValue(destination),
    )
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
                                IconButton(
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
                    RetainedTabs(
                        current = destination,
                        contentPadding = contentPadding,
                    ) { target, tabPadding ->
                        when (target) {
                            AppDestination.Home -> HomeScreen(appState, tabPadding)
                            AppDestination.Apps -> AppProfilesScreen(appState, tabPadding)
                            AppDestination.Profiles -> ProfileManagementScreen(appState, tabPadding)
                            AppDestination.Settings -> AppSettingsScreen(appState, tabPadding)
                        }
                    }
                }
            }
        }
    }

    when (navigationType) {
        NavigationType.Bar -> page()
        NavigationType.Drawer -> {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.animateContentSize(alignment = Alignment.TopStart),
                ) {
                    if (navigationExpanded) {
                        ExpandedNavigationDrawer(
                            destination = destination,
                            onDestinationSelected = { destinationName = it.name },
                            onCollapse = { navigationExpanded = false },
                        )
                    } else {
                        CollapsedNavigationRail(
                            destination = destination,
                            onDestinationSelected = { destinationName = it.name },
                            onExpand = { navigationExpanded = true },
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) { page() }
            }
        }
    }
}

/**
 * Keeps every tab the user has opened alive, the way a UITabBarController holds on to its
 * view controllers.
 *
 * This replaced an `AnimatedContent`, which threw the outgoing screen away and rebuilt the
 * incoming one from scratch on every switch. That meant the very first frame of a
 * transition had to compose and lay out a whole screenful of list rows — text measurement
 * included — before it could draw, which no animation curve can hide. Here a tab is built
 * once, on its first visit; after that a switch is a crossfade between layers the GPU is
 * already holding, so it costs essentially nothing per frame.
 *
 * Each screen also keeps its own scroll position and search text for free, because its
 * composition is never discarded.
 */
@Composable
private fun RetainedTabs(
    current: AppDestination,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable (AppDestination, PaddingValues) -> Unit,
) {
    val opened = remember { mutableStateListOf<AppDestination>() }
    if (current !in opened) {
        opened.add(current)
    }

    // Build the tabs the user has not reached yet during idle frames, so that even the
    // *first* switch to each one is free. Composing a screen is main-thread work that has
    // to happen at some point; doing it here, while the user is still reading the landing
    // tab, means no transition ever pays for it. One tab per frame, after a short settle,
    // so the warm-up cannot itself become the stall it is meant to prevent.
    LaunchedEffect(Unit) {
        delay(TabWarmUpDelayMillis)
        for (destination in AppDestination.entries) {
            withFrameNanos { }
            if (destination !in opened) {
                opened.add(destination)
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        opened.forEach { destination ->
            key(destination) {
                val active = destination == current
                val presence by animateFloatAsState(
                    targetValue = if (active) 1f else 0f,
                    animationSpec = TabSwitchSpec,
                    label = "tabPresence",
                )
                // The scaffold's padding changes on every frame while the large app bar
                // collapses. Only the tab on screen should follow it: if the hidden ones
                // tracked it too they would re-measure in lockstep, multiplying the cost
                // of every scroll frame by the number of open tabs. Re-keyed on `active`
                // so a hidden tab holds the padding it had when it left the screen and
                // picks the live value back up the moment it returns.
                val frozenPadding = remember(active) { contentPadding }
                val tabPadding = if (active) contentPadding else frozenPadding
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Incoming tab on top, so the outgoing one dissolves beneath it
                        // instead of briefly painting over it.
                        .zIndex(if (active) 1f else 0f)
                        .graphicsLayer {
                            alpha = presence
                            // A trace of scale instead of a slide: the settling "zoom"
                            // iOS uses when a view comes forward, kept small enough to
                            // read as depth rather than movement. At alpha 0 Compose
                            // skips drawing the layer altogether, so idle tabs are free.
                            val scale = TabEnterScale + (1f - TabEnterScale) * presence
                            scaleX = scale
                            scaleY = scale
                        }
                        .then(if (active) Modifier else InertTabModifier),
                ) {
                    content(destination, tabPadding)
                }
            }
        }
    }
}

/**
 * Critically damped, so the swap settles without a wobble, and a spring rather than a
 * fixed-duration curve so tapping a second tab mid-transition retargets from the current
 * velocity instead of snapping back to the start. That interruptibility is most of what
 * reads as "iOS" — the animation always follows the finger rather than finishing a script.
 */
private val TabSwitchSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 700f,
)

/** How small an incoming tab starts out. Deliberately only just perceptible. */
private const val TabEnterScale = 0.97f

/** Long enough for the landing tab to have settled before warm-up starts competing. */
private const val TabWarmUpDelayMillis = 350L

/**
 * A tab that is not on screen must not take touches or be reachable by a screen reader.
 * Consuming in the initial pass stops the event before any child sees it.
 */
private val InertTabModifier = Modifier
    .clearAndSetSemantics {}
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    }

@Composable
private fun CollapsedNavigationRail(
    destination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    onExpand: () -> Unit,
) {
    NavigationRail(
        header = {
            Spacer(Modifier.height(MdSpacing.md))
            IconButton(onClick = onExpand) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.navigation_expand),
                )
            }
            Spacer(Modifier.height(MdSpacing.lg))
        },
    ) {
        AppDestination.entries.forEach { item ->
            NavigationRailItem(
                selected = item == destination,
                onClick = { onDestinationSelected(item) },
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

@Composable
private fun ExpandedNavigationDrawer(
    destination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    onCollapse: () -> Unit,
) {
    PermanentDrawerSheet(modifier = Modifier.width(256.dp)) {
        Spacer(Modifier.height(MdSpacing.md))
        IconButton(
            onClick = onCollapse,
            modifier = Modifier.padding(start = MdSpacing.sm),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                contentDescription = stringResource(R.string.navigation_collapse),
            )
        }
        Spacer(Modifier.height(MdSpacing.lg))
        AppDestination.entries.forEach { item ->
            NavigationDrawerItem(
                selected = item == destination,
                onClick = { onDestinationSelected(item) },
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
}
