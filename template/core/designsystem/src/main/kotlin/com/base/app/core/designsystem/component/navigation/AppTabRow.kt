package com.base.app.core.designsystem.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.container.AppDivider
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import kotlin.math.roundToInt

/**
 * Fixed-width tabs with an underline that slides between them.
 *
 * The indicator travels on a spring, so switching tabs rapidly reverses from wherever it has got
 * to instead of restarting — the same reason the segmented control's pill does.
 *
 * Tabs are equal width. Scrollable, content-width tabs need a measurement pass per tab and a
 * scroll position to keep in sync with the selection; if a screen has enough tabs to need that,
 * it almost always wants a dropdown or a different information architecture instead.
 */
@Composable
fun AppTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return

    val colors = AppTheme.colors
    val position by animateFloatAsState(
        targetValue = selectedIndex.coerceIn(0, tabs.lastIndex).toFloat(),
        animationSpec = AppTheme.motion.sheet(),
        label = "tabIndicator",
    )

    Column(modifier = modifier.fillMaxWidth().background(colors.surface)) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TAB_HEIGHT)
                    .selectableGroup(),
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    val content by animateColorAsState(
                        targetValue = if (selected) colors.accent else colors.contentTertiary,
                        animationSpec = tween(AppTheme.motion.quick),
                        label = "tabContent",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onTabSelected(index) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppText(
                            text = tab,
                            style = AppTheme.typography.titleMedium,
                            color = content,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = AppTheme.spacing.sm),
                        )
                    }
                }
            }

            AppDivider(
                color = colors.divider,
                modifier = Modifier.align(Alignment.BottomStart),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .layout { measurable, constraints ->
                        val tabWidth = constraints.maxWidth / tabs.size
                        // The indicator is inset from the tab's full width so it reads as
                        // underlining the label rather than the column.
                        val indicatorWidth = (tabWidth * INDICATOR_WIDTH_FRACTION).roundToInt()
                        val placeable = measurable.measure(
                            constraints.copy(minWidth = indicatorWidth, maxWidth = indicatorWidth),
                        )
                        layout(constraints.maxWidth, placeable.height) {
                            val offset = position * tabWidth + (tabWidth - indicatorWidth) / 2f
                            placeable.placeRelative(offset.roundToInt(), 0)
                        }
                    }
                    .height(INDICATOR_HEIGHT)
                    .clip(AppTheme.shapes.pill)
                    .background(colors.accent),
            )
        }
    }
}

private val TAB_HEIGHT = 46.dp
private val INDICATOR_HEIGHT = 3.dp
private const val INDICATOR_WIDTH_FRACTION = 0.6f
