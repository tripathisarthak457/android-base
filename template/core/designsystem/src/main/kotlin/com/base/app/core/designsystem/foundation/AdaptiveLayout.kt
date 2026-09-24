package com.base.app.core.designsystem.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.WindowWidth

/**
 * Fills the width up to [maxWidth], then stops and centres. Content that stretches across a tablet
 * or a desktop window is content nobody can read: lines too long to follow, buttons a room apart.
 *
 * ```
 * Column(Modifier.centeredMaxWidth(AppTheme.layout.formMaxWidth)) { … }
 * ```
 */
fun Modifier.centeredMaxWidth(maxWidth: Dp): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = maxWidth)

/**
 * Whether a screen with two halves — a picture and its words, a list and its detail — should put
 * them side by side. True from Expanded width, and on anything Medium or wider that is short: a
 * phone held sideways, where stacking the halves would push the second one off screen.
 */
val shouldUseTwoPanes: Boolean
    @Composable get() {
        val window = AppTheme.windowSize
        return window.hasRoomForTwoPanes || (window.isShort && window.width >= WindowWidth.Medium)
    }

/**
 * Two halves of one screen, side by side when [shouldUseTwoPanes] says there is room, stacked
 * otherwise. [firstWeight] is the first half's share of the width when they are side by side.
 */
@Composable
fun AppTwoPane(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    firstWeight: Float = 0.5f,
    sideBySide: Boolean = shouldUseTwoPanes,
    spacing: Dp = AppTheme.spacing.xl,
) {
    if (sideBySide) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(firstWeight)) { first() }
            Box(modifier = Modifier.weight(1f - firstWeight)) { second() }
        }
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
            first()
            second()
        }
    }
}

/**
 * Items laid out in as many equal columns as fit, never narrower than [minCellWidth]: one column on
 * a phone, two or three on a tablet, more on a desktop window. For a handful of items; a long or
 * paged list wants `LazyVerticalGrid(GridCells.Adaptive(…))` instead.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppResponsiveGrid(
    itemCount: Int,
    modifier: Modifier = Modifier,
    minCellWidth: Dp = AppTheme.layout.gridMinCellWidth,
    spacing: Dp = AppTheme.spacing.md,
    item: @Composable (index: Int) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fitting = ((maxWidth + spacing) / (minCellWidth + spacing)).toInt()
        val columns = fitting.coerceIn(1, itemCount.coerceAtLeast(1))
        val cellWidth = (maxWidth - spacing * (columns - 1)) / columns
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            maxItemsInEachRow = columns,
        ) {
            repeat(itemCount) { index ->
                Box(modifier = Modifier.width(cellWidth)) { item(index) }
            }
        }
    }
}
