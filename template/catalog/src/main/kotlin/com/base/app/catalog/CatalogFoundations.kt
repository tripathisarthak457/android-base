package com.base.app.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppMonoText
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.component.data.AppDetailRow
import com.base.app.core.designsystem.theme.AppFontNames
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.StatusColors

@Composable
fun FoundationsSection() {
    CatalogGroup(
        title = "Surfaces and content",
        caption = "Flip the theme from the index to see both palettes.",
    ) {
        ColorRow("background", AppTheme.colors.background)
        ColorRow("surface", AppTheme.colors.surface)
        ColorRow("surfaceVariant", AppTheme.colors.surfaceVariant)
        ColorRow("contentPrimary", AppTheme.colors.contentPrimary)
        ColorRow("contentSecondary", AppTheme.colors.contentSecondary)
        ColorRow("contentTertiary", AppTheme.colors.contentTertiary)
        ColorRow("border", AppTheme.colors.border)
        ColorRow("accent", AppTheme.colors.accent)
    }

    CatalogGroup(
        title = "Status",
        caption = "Each meaning carries a content, a subtle fill and a border — always together.",
    ) {
        StatusRow("success", AppTheme.colors.success)
        StatusRow("warning", AppTheme.colors.warning)
        StatusRow("danger", AppTheme.colors.danger)
        StatusRow("info", AppTheme.colors.info)
        StatusRow("neutral", AppTheme.colors.neutral)
    }

    CatalogGroup(
        title = "Typeface",
        caption = "One name in AppFontNames sets every style below. Downloaded, not bundled.",
    ) {
        AppDetailRow(label = "Sans", value = AppFontNames.Sans)
        AppDetailRow(label = "Mono", value = AppFontNames.Mono)
        AppText(
            text = "Sphinx of black quartz, judge my vow. 0123456789",
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.contentPrimary,
        )
        AppMonoText(
            text = "0O 1lI 8B 5S  —  8F3K-2211-90XZ",
            color = AppTheme.colors.contentSecondary,
        )
    }

    CatalogGroup(title = "Type scale") {
        TypeRow("displayLarge", AppTheme.typography.displayLarge)
        TypeRow("displayMedium", AppTheme.typography.displayMedium)
        TypeRow("displaySmall", AppTheme.typography.displaySmall)
        TypeRow("headingLarge", AppTheme.typography.headingLarge)
        TypeRow("headingMedium", AppTheme.typography.headingMedium)
        TypeRow("headingSmall", AppTheme.typography.headingSmall)
        TypeRow("titleLarge", AppTheme.typography.titleLarge)
        TypeRow("titleMedium", AppTheme.typography.titleMedium)
        TypeRow("titleSmall", AppTheme.typography.titleSmall)
        TypeRow("bodyLarge", AppTheme.typography.bodyLarge)
        TypeRow("bodyMedium", AppTheme.typography.bodyMedium)
        TypeRow("bodySmall", AppTheme.typography.bodySmall)
        TypeRow("label", AppTheme.typography.label)
        TypeRow("caption", AppTheme.typography.caption)
        TypeRow("button", AppTheme.typography.button)
        TypeRow("mono", AppTheme.typography.mono)
    }

    CatalogGroup(title = "Spacing", caption = "A 4dp grid. gutter and stack are the named ones.") {
        SpacingRow("xs", AppTheme.spacing.xs)
        SpacingRow("sm", AppTheme.spacing.sm)
        SpacingRow("md", AppTheme.spacing.md)
        SpacingRow("lg", AppTheme.spacing.lg)
        SpacingRow("xl", AppTheme.spacing.xl)
        SpacingRow("xxl", AppTheme.spacing.xxl)
        SpacingRow("gutter", AppTheme.spacing.gutter)
        SpacingRow("stack", AppTheme.spacing.stack)
    }

    CatalogGroup(
        title = "Elevation",
        caption = "A shadow in light, a lighter surface and an outline in dark — same token.",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            ElevationSwatch("raised", AppTheme.elevation.raised)
            ElevationSwatch("card", AppTheme.elevation.card)
            ElevationSwatch("overlay", AppTheme.elevation.overlay)
            ElevationSwatch("modal", AppTheme.elevation.modal)
        }
    }

    CatalogGroup(title = "Shapes") {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            ShapeSwatch("xs") { AppTheme.shapes.xs }
            ShapeSwatch("sm") { AppTheme.shapes.sm }
            ShapeSwatch("md") { AppTheme.shapes.md }
            ShapeSwatch("lg") { AppTheme.shapes.lg }
            ShapeSwatch("pill") { AppTheme.shapes.pill }
        }
    }
}

@Composable
private fun ColorRow(name: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(AppTheme.shapes.xs)
                .background(color),
        )
        AppText(
            text = name,
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.contentPrimary,
        )
        AppMonoText(text = color.hex(), color = AppTheme.colors.contentTertiary)
    }
}

@Composable
private fun StatusRow(name: String, status: StatusColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.sm)
            .background(status.subtle)
            .padding(AppTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(AppTheme.shapes.pill)
                .background(status.content),
        )
        AppText(
            text = name,
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.titleSmall,
            color = status.content,
        )
        AppMonoText(text = status.content.hex(), color = status.content)
    }
}

@Composable
private fun TypeRow(name: String, style: TextStyle) {
    Column {
        AppMonoText(
            text = "$name · ${style.fontSize.value.toInt()}sp",
            color = AppTheme.colors.contentTertiary,
        )
        AppText(
            text = "The quick brown fox 0123",
            style = style,
            color = AppTheme.colors.contentPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun SpacingRow(name: String, value: Dp) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppMonoText(
            text = name,
            modifier = Modifier.width(64.dp),
            color = AppTheme.colors.contentSecondary,
        )
        Box(
            modifier = Modifier
                .width(value)
                .height(16.dp)
                .clip(AppTheme.shapes.xs)
                .background(AppTheme.colors.accent),
        )
        AppMonoText(text = "${value.value.toInt()}dp", color = AppTheme.colors.contentTertiary)
    }
}

@Composable
private fun ElevationSwatch(name: String, elevation: Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        AppSurface(
            modifier = Modifier.size(56.dp),
            shape = AppTheme.shapes.sm,
            elevation = elevation,
        ) { }
        AppMonoText(text = name, color = AppTheme.colors.contentTertiary)
    }
}

@Composable
private fun ShapeSwatch(name: String, shape: @Composable () -> Shape) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(shape())
                .background(AppTheme.colors.accentSubtle),
        )
        AppMonoText(text = name, color = AppTheme.colors.contentTertiary)
    }
}

/**
 * `#AARRGGBB`, which is the form you paste back into a palette file.
 *
 * `toArgb()` returns a signed Int, so the value is masked to a Long before formatting — without
 * that, every colour with an alpha above 0x7F formats as a sixteen-digit negative.
 */
private fun Color.hex(): String =
    "#%08X".format(toArgb().toLong() and 0xFFFFFFFFL)
