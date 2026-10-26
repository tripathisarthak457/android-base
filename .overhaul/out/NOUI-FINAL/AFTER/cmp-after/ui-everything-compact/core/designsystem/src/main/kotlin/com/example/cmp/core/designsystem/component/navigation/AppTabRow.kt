package com.example.cmp.core.designsystem.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cmp.core.designsystem.component.container.AppDivider
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.foundation.interaction.AppHaptic
import com.example.cmp.core.designsystem.foundation.interaction.minimumTouchTarget
import com.example.cmp.core.designsystem.foundation.interaction.rememberAppHaptics
import com.example.cmp.core.designsystem.preview.AppComponentPreviews
import com.example.cmp.core.designsystem.theme.AppTheme
import kotlin.math.roundToInt

/**
 * Tabs inside a screen, with an underline that slides between them. They share the width equally
 * while every label fits its share; when one does not, each tab is as wide as its label needs, so no
 * label is cut while shorter ones have room to spare. The underline's position and the labels'
 * colours are read while drawing and placing, so a switch recomposes nothing per frame.
 */
@Composable
public fun AppTabRow(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    val colors = AppTheme.colors
    val style = AppTheme.typography.subheadline
    val labelPadding = AppTheme.spacing.md
    val haptics = rememberAppHaptics()
    val position = animateFloatAsState(
        targetValue = selected.coerceIn(0, tabs.lastIndex).toFloat(),
        animationSpec = AppTheme.springs.thumb.unit,
        label = "tabIndicator",
    )
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val needed = remember(tabs, style, density, labelPadding) {
        val padding = with(density) { labelPadding.roundToPx() } * 2
        tabs.map {
            measurer.measure(it, style, maxLines = 1, softWrap = false).size.width + padding
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val total = constraints.maxWidth
        val weights = if (needed.max() * tabs.size <=
            total
        ) {
            List(tabs.size) { 1f }
        } else {
            needed.map { it.toFloat() }
        }
        val widths = weights.map { total * it / weights.sum() }
        val starts = widths.runningFold(0f) { start, width -> start + width }

        Row(Modifier.fillMaxWidth().heightIn(min = AppTabRowDefaults.Height).selectableGroup()) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selected
                val content = animateColorAsState(
                    targetValue = if (isSelected) colors.accentContent else colors.contentSecondary,
                    animationSpec = AppTheme.springs.quickFade(),
                    label = "tabContent",
                )
                Box(
                    modifier = Modifier
                        .weight(weights[index])
                        .minimumTouchTarget()
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            indication = null,
                            interactionSource = null,
                            onClick = {
                                if (!isSelected) haptics.perform(AppHaptic.Selection)
                                onSelect(index)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = tab,
                        color = { content.value },
                        modifier = Modifier.padding(horizontal = labelPadding),
                        style = style,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        AppDivider(modifier = Modifier.align(Alignment.BottomStart))

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .layout { measurable, constraints ->
                    // Between two tabs while it slides, the underline takes the in-between width too.
                    val at = position.value
                    val from = at.toInt().coerceIn(0, tabs.lastIndex)
                    val to = (from + 1).coerceAtMost(tabs.lastIndex)
                    val fraction = at - from
                    val tabStart = starts[from] + (starts[to] - starts[from]) * fraction
                    val tabWidth = widths[from] + (widths[to] - widths[from]) * fraction
                    val indicatorWidth = (tabWidth * IndicatorWidthFraction).roundToInt()
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = indicatorWidth, maxWidth = indicatorWidth),
                    )
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.placeRelative(
                            (tabStart + (tabWidth - indicatorWidth) / 2f).roundToInt(),
                            0,
                        )
                    }
                }
                .height(AppTabRowDefaults.IndicatorHeight)
                .background(colors.accentContent, AppTheme.shapes.pill),
        )
    }
}

/** The tab row's height and the rule under the selected tab. */
public object AppTabRowDefaults {
    public val Height: Dp = 44.dp
    public val IndicatorHeight: Dp = 3.dp
}

private const val IndicatorWidthFraction = 0.6f

@AppComponentPreviews
@Composable
private fun AppTabRowPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppTabRow(tabs = listOf("Overview", "Activity", "Files"), selected = 0, onSelect = {})
        AppTabRow(tabs = listOf("Following", "For you"), selected = 1, onSelect = {})
        AppTabRow(
            tabs = listOf("Everything", "Unread conversations", "Mentions and replies", "Archived"),
            selected = 2,
            onSelect = {},
        )
    }
}
