package com.devicespooflab.hooks.ui

import android.text.format.DateFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devicespooflab.hooks.R

private data class HomeInfo(
    val icon: ImageVector,
    val label: String,
    val value: String,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(appState: SpoofAppState, scaffoldPadding: PaddingValues) {
    val config = appState.loadedConfig ?: return
    val profile = config.profile
    val moduleActivated = appState.isModuleActivated()
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { listState.scrollToItem(0) }
    val profileInfo = listOf(
        HomeInfo(
            Icons.Outlined.PhoneAndroid,
            stringResource(R.string.home_info_profile_model_title),
            profile.model,
        ),
        HomeInfo(
            Icons.AutoMirrored.Outlined.FormatListBulleted,
            stringResource(R.string.home_info_profile_preset_title),
            stringResource(
                R.string.home_info_profile_preset_value_format,
                appState.presetLabel(config.selectedPresetId),
                stringResource(if (config.isCustomMode) R.string.mode_custom else R.string.mode_preset),
            ),
        ),
        HomeInfo(
            Icons.Outlined.Info,
            stringResource(R.string.home_info_profile_android_title),
            stringResource(R.string.home_info_android_value_format, profile.buildRelease, profile.buildSdk),
        ),
        HomeInfo(
            Icons.Outlined.AspectRatio,
            stringResource(R.string.home_info_profile_display_title),
            stringResource(
                R.string.home_info_display_value_format,
                profile.screenWidth,
                profile.screenHeight,
                profile.screenDensity,
            ),
        ),
        HomeInfo(
            Icons.Outlined.Folder,
            stringResource(R.string.home_config_file_title),
            stringResource(
                R.string.home_info_config_value_format,
                config.configFile.name,
                DateFormat.format("yyyy-MM-dd HH:mm", config.configFile.lastModified()),
            ),
        ),
    )
    val statusContainerColor = if (moduleActivated) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        lerp(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.errorContainer,
            0.30f,
        )
    }
    val statusContentColor = if (moduleActivated) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val statusSupportingColor = if (moduleActivated) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.78f)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding),
        contentPadding = PaddingValues(horizontal = MdSpacing.sm, vertical = MdSpacing.sm),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = statusContainerColor),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = MdSpacing.sm, vertical = MdSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (moduleActivated) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = if (moduleActivated) statusContentColor else MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(MdSpacing.sm))
                    Column {
                        Text(
                            text = stringResource(if (moduleActivated) R.string.home_module_active_title else R.string.home_module_inactive_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = (MaterialTheme.typography.titleLarge.fontSize.value - 1).sp,
                            fontWeight = FontWeight.Medium,
                            color = statusContentColor,
                        )
                        if (moduleActivated) {
                            Spacer(Modifier.height(MdSpacing.xxs))
                        }
                        Text(
                            text = stringResource(if (moduleActivated) R.string.home_module_active_body else R.string.home_module_inactive_body),
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value - 1).sp,
                            color = statusSupportingColor,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(MdSpacing.xs)) }
        item { SectionTitle(stringResource(R.string.home_active_profile_title)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(modifier = Modifier.padding(vertical = MdSpacing.xs)) {
                    profileInfo.forEach { info ->
                        ListItem(
                            leadingContent = {
                                Icon(imageVector = info.icon, contentDescription = null, modifier = Modifier.size(22.dp))
                            },
                            contentPadding = PaddingValues(horizontal = MdSpacing.sm, vertical = MdSpacing.xxs),
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                leadingContentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy((-4).dp)) {
                                Text(
                                    text = info.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                )
                                Text(
                                    text = info.value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(MdSpacing.xl)) }
    }
}
