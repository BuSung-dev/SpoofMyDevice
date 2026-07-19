package com.devicespooflab.hooks.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object MdSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 16.dp
    val md = 24.dp
    val lg = 32.dp
    val xl = 48.dp
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
    val colors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        trailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
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
