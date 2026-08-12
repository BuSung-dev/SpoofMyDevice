package com.devicespooflab.hooks.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Content type shared by every installed-app row. Lazy lists reuse a scrolled-off item's
 * node tree only for a new item of the same content type, so tagging the rows keeps a
 * fling from rebuilding each row from scratch — and stops them being reused for the
 * header, search field or footer, which look nothing like a row.
 */
internal const val APP_ROW_CONTENT_TYPE = "app_row"

object MdSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 16.dp
    val md = 24.dp
    val lg = 32.dp
    val xl = 48.dp
}

/**
 * Shows an installed app's launcher icon, decoding it on first display and reusing the
 * cached bitmap afterwards. Only rows that are actually visible pay the decode cost,
 * which keeps large app lists cheap to open and smooth to scroll. A neutral placeholder
 * of the same size holds the slot so rows never reflow when an icon arrives.
 */
@Composable
fun AppIcon(
    appState: SpoofAppState,
    packageName: String,
    modifier: Modifier = Modifier,
) {
    // Reads this package's own icon state, so a decode elsewhere in the list does not
    // invalidate this row. A shared observable map would invalidate every reader on
    // every insert, recomposing the whole viewport repeatedly during a fling.
    val icon by appState.iconStateFor(packageName)
    LaunchedEffect(packageName) {
        appState.loadIcon(packageName)
    }
    val currentIcon = icon
    if (currentIcon != null) {
        Image(
            bitmap = currentIcon,
            contentDescription = null,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            ),
        )
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.padding(top = MdSpacing.xs, bottom = MdSpacing.xs),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun ExpressiveSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(title)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = MdSpacing.sm, vertical = 12.dp),
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectedSettingsRow(
    index: Int,
    count: Int,
    title: String,
    summary: String,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailingText: String? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    val modifier = segmentedGapModifier(index, count)
    val shapes = connectedListItemShapes(index, count)
    val colors = rememberSegmentedListColors()
    val leadingContent: (@Composable () -> Unit)? = icon?.let {
        {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        }
    }
    val supportingContent: (@Composable () -> Unit)? = summary.takeIf(String::isNotBlank)?.let {
        {
            Text(
                text = it,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    val trailingContent: (@Composable () -> Unit)? = when {
        checked != null && onCheckedChange != null -> {
            { Switch(checked = checked, onCheckedChange = onCheckedChange) }
        }
        !trailingText.isNullOrBlank() -> {
            {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        onClick != null -> {
            {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        else -> null
    }

    when {
        checked != null && onCheckedChange != null -> {
            SegmentedListItem(
                onClick = { onCheckedChange(!checked) },
                shapes = shapes,
                modifier = modifier,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                colors = colors,
            ) {
                Text(title)
            }
        }
        onClick != null -> {
            SegmentedListItem(
                onClick = onClick,
                shapes = shapes,
                modifier = modifier,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                colors = colors,
            ) {
                Text(title)
            }
        }
        else -> {
            SegmentedListItem(
                shapes = shapes,
                modifier = modifier,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                colors = colors,
            ) {
                Text(title)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectedInfoRow(
    index: Int,
    count: Int,
    label: String,
    value: String,
) {
    SegmentedListItem(
        shapes = connectedListItemShapes(index, count),
        modifier = segmentedGapModifier(index, count),
        supportingContent = { Text(value) },
        colors = rememberSegmentedListColors(),
    ) {
        Text(label)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MdSpacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(MdSpacing.xxs))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun segmentedGapModifier(index: Int, count: Int): Modifier {
    return if (index < count - 1) {
        Modifier.padding(bottom = ListItemDefaults.SegmentedGap)
    } else {
        Modifier
    }
}

/**
 * The segmented list colors used by every row in the app's lists.
 *
 * [ListItemDefaults.segmentedColors] allocates a fresh 24-field `ListItemColors` every
 * time it is called, so calling it per row means one allocation per visible item per
 * recomposition. The value depends only on the color scheme, so the theme computes it
 * once via [ProvideSegmentedListColors] and every row reads that single instance.
 *
 * The fallback path keeps this usable outside [SpoofMyDeviceTheme] (previews, tests).
 */
@Composable
fun rememberSegmentedListColors(): ListItemColors =
    LocalSegmentedListColors.current ?: defaultSegmentedListColors()

/**
 * Publishes one shared [ListItemColors] for the whole tree. Static because it only ever
 * changes with the color scheme, which already invalidates everything.
 */
private val LocalSegmentedListColors = staticCompositionLocalOf<ListItemColors?> { null }

@Composable
fun ProvideSegmentedListColors(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSegmentedListColors provides defaultSegmentedListColors(),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun defaultSegmentedListColors(): ListItemColors {
    val colorScheme = MaterialTheme.colorScheme
    return ListItemDefaults.segmentedColors(
        containerColor = colorScheme.surfaceContainer,
        contentColor = colorScheme.onSurface,
        leadingContentColor = colorScheme.onSurfaceVariant,
        trailingContentColor = colorScheme.onSurfaceVariant,
        supportingContentColor = colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun connectedListItemShapes(index: Int, count: Int) = if (count == 1) {
    ListItemDefaults.segmentedShapes(
        index = index,
        count = count,
        defaultShapes = ListItemDefaults.shapes(
            shape = MaterialTheme.shapes.large,
            selectedShape = MaterialTheme.shapes.large,
        ),
    )
} else {
    ListItemDefaults.segmentedShapes(index, count)
}

/**
 * Shape sets for a connected list, precomputed once per list instead of per row.
 *
 * In a segmented list only the first and last rows differ from the middle ones, so a
 * long list needs just three distinct shape objects rather than one per visible row.
 */
@Immutable
class SegmentedShapeSet(
    private val single: ListItemShapes,
    private val first: ListItemShapes,
    private val middle: ListItemShapes,
    private val last: ListItemShapes,
) {
    fun forIndex(index: Int, count: Int): ListItemShapes = when {
        count <= 1 -> single
        index == 0 -> first
        index == count - 1 -> last
        else -> middle
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberSegmentedShapes(): SegmentedShapeSet {
    val single = connectedListItemShapes(0, 1)
    val first = ListItemDefaults.segmentedShapes(0, 3)
    val middle = ListItemDefaults.segmentedShapes(1, 3)
    val last = ListItemDefaults.segmentedShapes(2, 3)
    return remember(single, first, middle, last) {
        SegmentedShapeSet(single, first, middle, last)
    }
}
