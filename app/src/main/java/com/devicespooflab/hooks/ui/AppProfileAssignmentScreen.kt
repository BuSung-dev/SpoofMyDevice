package com.devicespooflab.hooks.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devicespooflab.hooks.R
import com.devicespooflab.hooks.data.AppProfileStore

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun AppProfileAssignmentLoadingScreen(onBack: () -> Unit) {
    AssignmentScaffold(
        title = stringResource(R.string.app_profile_assignment_title),
        onBack = onBack,
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LoadingIndicator(modifier = Modifier.progressSemantics())
                Spacer(Modifier.height(MdSpacing.sm))
                Text(
                    text = stringResource(R.string.app_profile_loading_profiles),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun AppProfileAssignmentScreen(
    label: String,
    packageName: String,
    icon: Bitmap,
    profiles: List<AppProfileStore.Profile>,
    assignedProfileId: String?,
    onProfileSelected: (String?) -> Unit,
    onCreateProfile: () -> Unit,
    onBack: () -> Unit,
) {
    AssignmentScaffold(
        title = stringResource(R.string.app_profile_assignment_title),
        onBack = onBack,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp),
        ) {
            item {
                ListItem(
                    supportingContent = { Text(packageName) },
                    leadingContent = {
                        Image(
                            bitmap = icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.medium),
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                ) {
                    Text(label)
                }
                SectionTitle(stringResource(R.string.app_profile_select))
            }
            item {
                SegmentedListItem(
                    onClick = { onProfileSelected(null) },
                    shapes = connectedListItemShapes(0, profiles.size + 1),
                    modifier = Modifier.padding(bottom = ListItemDefaults.SegmentedGap),
                    leadingContent = {
                        Icon(Icons.Outlined.Block, contentDescription = null)
                    },
                    supportingContent = {
                        Text(stringResource(R.string.app_profile_none_summary))
                    },
                    trailingContent = {
                        RadioButton(
                            selected = assignedProfileId == null,
                            onClick = null,
                        )
                    },
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text(stringResource(R.string.app_profile_none))
                }
            }
            itemsIndexed(
                items = profiles,
                key = { _, profile -> profile.id },
            ) { index, profile ->
                SegmentedListItem(
                    onClick = { onProfileSelected(profile.id) },
                    shapes = connectedListItemShapes(index + 1, profiles.size + 1),
                    modifier = if (index < profiles.lastIndex) {
                        Modifier.padding(bottom = ListItemDefaults.SegmentedGap)
                    } else {
                        Modifier
                    },
                    leadingContent = {
                        Icon(Icons.Outlined.AccountBox, contentDescription = null)
                    },
                    trailingContent = {
                        RadioButton(
                            selected = profile.id == assignedProfileId,
                            onClick = null,
                        )
                    },
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(profile.name)
                }
            }
            item {
                FilledTonalButton(
                    onClick = onCreateProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MdSpacing.sm),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.size(MdSpacing.xs))
                    Text(stringResource(R.string.app_profile_create))
                }
            }
            item { Spacer(Modifier.height(MdSpacing.xl)) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun AssignmentScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        content = content,
    )
}
