package com.example.everything.catalog.sections

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.everything.catalog.CatalogGroup
import com.example.everything.catalog.TileGrid
import com.example.everything.catalog.fontScaled
import com.example.everything.catalog.wrappableIdentifier
import com.example.everything.core.designsystem.component.button.AppButton
import com.example.everything.core.designsystem.component.button.ButtonSize
import com.example.everything.core.designsystem.component.button.ButtonVariant
import com.example.everything.core.designsystem.component.text.AppFiguresText
import com.example.everything.core.designsystem.component.text.AppText
import com.example.everything.core.designsystem.foundation.content.AppSurface
import com.example.everything.core.designsystem.theme.AppSpring
import com.example.everything.core.designsystem.theme.AppTheme
import com.example.everything.core.designsystem.theme.StatusColors
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun FoundationsSection() {
    val colors = AppTheme.colors
    CatalogGroup("Surfaces", caption = "Grouped lists sit on backgroundGrouped; floating chrome on surfaceElevated, lifted in dark.") {
        Swatches(
            "background" to colors.background,
            "backgroundGrouped" to colors.backgroundGrouped,
            "surface" to colors.surface,
            "surfaceGrouped" to colors.surfaceGrouped,
            "surfaceElevated" to colors.surfaceElevated,
            "surfaceInverse" to colors.surfaceInverse,
            "fill" to colors.fill,
            "segmentThumb" to colors.segmentThumb,
            "separator" to colors.separator,
            "outline" to colors.outline,
        )
    }
    CatalogGroup("Content") {
        Swatches(
            "contentPrimary" to colors.contentPrimary,
            "contentSecondary" to colors.contentSecondary,
            "contentTertiary" to colors.contentTertiary,
            "contentDisabled" to colors.contentDisabled,
            "contentInverse" to colors.contentInverse,
        )
    }
    CatalogGroup("Brand", caption = "accentContent is the accent as text, kept at 4.5:1 on every background whatever the brand colour.") {
        Swatches(
            "accent" to colors.accent,
            "accentPressed" to colors.accentPressed,
            "accentSubtle" to colors.accentSubtle,
            "onAccent" to colors.onAccent,
            "accentContent" to colors.accentContent,
            "secondary" to colors.secondary,
            "secondaryPressed" to colors.secondaryPressed,
            "secondarySubtle" to colors.secondarySubtle,
            "onSecondary" to colors.onSecondary,
            "tertiary" to colors.tertiary,
            "tertiaryPressed" to colors.tertiaryPressed,
            "tertiarySubtle" to colors.tertiarySubtle,
            "onTertiary" to colors.onTertiary,
        )
    }
    CatalogGroup("Status", caption = "Each meaning in four roles: base, content, subtle and on.") {
        StatusRow("success", colors.success)
        StatusRow("warning", colors.warning)
        StatusRow("danger", colors.danger)
        StatusRow("info", colors.info)
        StatusRow("neutral", colors.neutral)
    }
    CatalogGroup("States") {
        Swatches(
            "highlight" to colors.highlight,
            "scrim" to colors.scrim,
            "focusRing" to colors.focusRing,
            "shadow" to colors.shadow,
            "barIndicator" to colors.barIndicator,
            "skeleton" to colors.skeleton,
            "skeletonHighlight" to colors.skeletonHighlight,
        )
    }
    CatalogGroup("Type", caption = "iOS's Dynamic Type sizes. A design style's voice changes weights only.") {
        val type = AppTheme.typography
        TypeRow("largeTitle", type.largeTitle)
        TypeRow("title1", type.title1)
        TypeRow("title2", type.title2)
        TypeRow("title3", type.title3)
        TypeRow("headline", type.headline)
        TypeRow("body", type.body)
        TypeRow("callout", type.callout)
        TypeRow("subheadline", type.subheadline)
        TypeRow("footnote", type.footnote)
        TypeRow("caption1", type.caption1)
        TypeRow("caption2", type.caption2)
        TypeRow("mono", type.mono)
    }
    CatalogGroup("Spacing", caption = "The gutter follows the window: wider on tablets.") {
        val spacing = AppTheme.spacing
        listOf(
            "xxs" to spacing.xxs, "xs" to spacing.xs, "sm" to spacing.sm, "md" to spacing.md, "lg" to spacing.lg,
            "xl" to spacing.xl, "xxl" to spacing.xxl, "xxxl" to spacing.xxxl, "gutter" to spacing.gutter,
            "section" to spacing.section, "stack" to spacing.stack,
        ).forEach { (name, size) -> MeasureRow(name, size) }
    }
    CatalogGroup("Shapes") {
        val shapes = AppTheme.shapes
        val style = AppTheme.style
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            listOf(
                "none" to shapes.none, "xs" to shapes.xs, "sm" to shapes.sm, "md" to shapes.md, "lg" to shapes.lg,
                "xl" to shapes.xl, "pill" to shapes.pill, "sheet" to shapes.sheet,
                "buttonShape" to style.buttonShape, "chipShape" to style.chipShape,
            ).forEach { (name, shape) -> ShapeSwatch(name, shape) }
        }
    }
    CatalogGroup("Sizes", caption = "Heights are minimums: text-bearing components grow with the font size.") {
        val sizes = AppTheme.sizes
        listOf(
            "minTouchTarget" to sizes.minTouchTarget, "iconSmall" to sizes.iconSmall, "icon" to sizes.icon,
            "iconLarge" to sizes.iconLarge, "buttonSmall" to sizes.buttonSmall, "buttonMedium" to sizes.buttonMedium,
            "buttonLarge" to sizes.buttonLarge, "fieldHeight" to sizes.fieldHeight, "rowHeight" to sizes.rowHeight,
            "topBarHeight" to sizes.topBarHeight,
        ).forEach { (name, size) -> MeasureRow(name, size) }
        val elevation = AppTheme.elevation
        listOf("none" to elevation.none, "raised" to elevation.raised, "card" to elevation.card, "overlay" to elevation.overlay, "modal" to elevation.modal).forEach { (name, lift) ->
            AppSurface(modifier = Modifier.fillMaxWidth().height(40.dp), elevation = lift, color = AppTheme.colors.surfaceElevated, contentAlignment = Alignment.Center) {
                AppText(text = "elevation.$name", style = AppTheme.typography.footnote)
            }
        }
    }
    CatalogGroup("Springs", caption = "Response and bounce, as iOS designs them. Tap one to send the dot across on it.") {
        val springs = AppTheme.springs
        SpringRow("press", springs.press)
        SpringRow("release", springs.release)
        SpringRow("thumb", springs.thumb)
        SpringRow("indicatorLead", springs.indicatorLead)
        SpringRow("indicatorTrail", springs.indicatorTrail)
        SpringRow("screen", springs.screen)
        SpringRow("sheet", springs.sheet)
        SpringRow("dialog", springs.dialog)
        SpringRow("layout", springs.layout)
        SpringRow("emphasis", springs.emphasis)
        SpringRow("interactive", springs.interactive)
        SpringRow("bounceBack", springs.bounceBack)
        AppText(
            text = "Fades ${springs.fadeQuick} and ${springs.fadeSlow} ms. Stagger ${springs.staggerMillis} ms, up to ${springs.staggerMaxItems} items. " +
                "Press scale ${springs.pressScale}, large ${springs.pressScaleLarge}, compact ${springs.pressScaleCompact}, dim ${springs.pressDimAlpha}.",
            style = AppTheme.typography.footnote,
            color = AppTheme.colors.contentSecondary,
        )
    }
}

@Composable
private fun Swatches(vararg roles: Pair<String, Color>) {
    TileGrid(minTileWidth = fontScaled(96.dp), spacing = AppTheme.spacing.sm) {
        roles.forEach { (name, color) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth().height(40.dp).background(color, AppTheme.shapes.sm).border(1.dp, AppTheme.colors.separator, AppTheme.shapes.sm))
                AppText(
                    text = remember(name) { wrappableIdentifier(name) },
                    style = AppTheme.typography.caption2,
                    color = AppTheme.colors.contentSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatusRow(name: String, status: StatusColors) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.background(status.base, AppTheme.shapes.pill).padding(horizontal = 10.dp, vertical = 4.dp)) {
            AppText(text = name, style = AppTheme.typography.caption1, color = status.on)
        }
        Box(Modifier.background(status.subtle, AppTheme.shapes.pill).padding(horizontal = 10.dp, vertical = 4.dp)) {
            AppText(text = "$name on subtle", style = AppTheme.typography.caption1, color = status.content)
        }
    }
}

@Composable
private fun TypeRow(name: String, style: TextStyle) {
    Column {
        AppText(text = name, style = style, color = AppTheme.colors.contentPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        AppFiguresText(
            text = "${style.fontSize.value.roundToInt()} / ${style.lineHeight.value.roundToInt()} · ${style.fontWeight?.weight ?: 400}",
            style = AppTheme.typography.caption1,
            color = AppTheme.colors.contentSecondary,
        )
    }
}

@Composable
private fun MeasureRow(name: String, size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
        AppText(text = name, modifier = Modifier.width(fontScaled(120.dp)), style = AppTheme.typography.footnote)
        Box(Modifier.width(size).height(12.dp).background(AppTheme.colors.accent, AppTheme.shapes.xs))
        AppFiguresText(text = "${size.value.roundToInt()}", style = AppTheme.typography.caption1, color = AppTheme.colors.contentSecondary)
    }
}

@Composable
private fun ShapeSwatch(name: String, shape: Shape) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(64.dp).background(AppTheme.colors.accentSubtle, shape).border(1.dp, AppTheme.colors.accent, shape))
        AppText(text = name, style = AppTheme.typography.caption2, color = AppTheme.colors.contentSecondary)
    }
}

@Composable
private fun SpringRow(name: String, spring: AppSpring) {
    val scope = rememberCoroutineScope()
    val position = remember { Animatable(0f) }
    val track = remember { intArrayOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            AppButton(
                text = name,
                onClick = { scope.launch { position.animateTo(if (position.targetValue > 0.5f) 0f else 1f, spring.unit) } },
                variant = ButtonVariant.Tinted,
                size = ButtonSize.Small,
            )
            AppFiguresText(text = "response ${spring.response} s, bounce ${"%.2f".format(spring.bounce)}", style = AppTheme.typography.caption1, color = AppTheme.colors.contentSecondary)
        }
        Box(Modifier.fillMaxWidth().height(16.dp).onSizeChanged { track[0] = it.width }) {
            Box(
                Modifier
                    .offset { IntOffset(((track[0] - 16.dp.roundToPx()) * position.value).roundToInt(), 0) }
                    .size(16.dp)
                    .background(AppTheme.colors.accent, AppTheme.shapes.pill),
            )
        }
    }
}
