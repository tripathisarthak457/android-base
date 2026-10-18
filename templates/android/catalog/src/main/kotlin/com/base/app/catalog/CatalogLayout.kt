package com.base.app.catalog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.foundation.chromeContentPadding

/**
 * A page's scroll padding: [base] and the top bar, and at the bottom the navigation bar and the
 * frame-time readout standing on it, so the end of every page scrolls clear of the readout.
 */
@Composable
fun catalogContentPadding(base: PaddingValues): PaddingValues {
    val chrome = chromeContentPadding(base)
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = chrome.calculateStartPadding(direction),
        top = chrome.calculateTopPadding(),
        end = chrome.calculateEndPadding(direction),
        bottom = chrome.calculateBottomPadding() + navigationBar + frameTimesReadoutReach(),
    )
}

/**
 * A width that holds a label at the person's font scale: token and icon names are identifiers with
 * no spaces to wrap at, so a fixed width breaks them mid-word at large text. Linear in the scale,
 * because Android 14's nonlinear scaling would barely grow a width given in sp.
 */
@Composable
@ReadOnlyComposable
fun fontScaled(width: Dp): Dp = width * LocalDensity.current.fontScale

/**
 * Children in as many columns as fit at [minColumnWidth] or wider, each placed in whichever column
 * is shortest so far. One column on a phone; on a tablet a page reads like a spread. Placed relative
 * to the layout direction, so a right-to-left page starts on the right.
 */
@Composable
fun MasonryColumns(
    minColumnWidth: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
        val columns = ((width + gap) / (minColumnWidth.roundToPx() + gap)).coerceAtLeast(1)
        val columnWidth = (width - gap * (columns - 1)) / columns
        val heights = IntArray(columns)
        val placed = measurables.map { measurable ->
            val placeable = measurable.measure(Constraints(minWidth = columnWidth, maxWidth = columnWidth))
            val column = heights.indices.minBy { heights[it] }
            val y = heights[column]
            heights[column] = y + placeable.height + gap
            Triple(placeable, column * (columnWidth + gap), y)
        }
        val height = ((heights.maxOrNull() ?: 0) - gap).coerceAtLeast(0)
        layout(width, height) {
            placed.forEach { (placeable, x, y) -> placeable.placeRelative(x, y) }
        }
    }
}
