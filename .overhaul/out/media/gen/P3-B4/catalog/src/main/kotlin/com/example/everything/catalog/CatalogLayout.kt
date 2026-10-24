package com.example.everything.catalog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.example.everything.core.designsystem.foundation.layout.chromeContentPadding

/**
 * A page's scroll padding: [chromeContentPadding] of [base], with the frame-time readout's reach
 * added at the bottom, so the end of every page scrolls clear of the readout standing on the
 * navigation bar. The chrome's bottom already includes that bar, and the chrome is still read when
 * the container measures, so a bar settling lays the page out again without recomposing it.
 */
@Composable
fun catalogContentPadding(base: PaddingValues): PaddingValues {
    val chrome = chromeContentPadding(base)
    val reach = frameTimesReadoutReach()
    return remember(chrome, reach) { ReadoutClearance(chrome, reach) }
}

@Stable
private class ReadoutClearance(private val chrome: PaddingValues, private val reach: Dp) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        chrome.calculateLeftPadding(layoutDirection)

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        chrome.calculateRightPadding(layoutDirection)

    override fun calculateTopPadding(): Dp = chrome.calculateTopPadding()

    override fun calculateBottomPadding(): Dp = chrome.calculateBottomPadding() + reach
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
 * [name] with a zero-width break opportunity before each word inside it, so an identifier longer
 * than its tile wraps between its words ("background" over "Grouped") rather than wherever the
 * line runs out ("backgroundGrou" over "ped").
 */
fun wrappableIdentifier(name: String): String = name.replace(WordStart, "​")

private val WordStart = Regex("(?<=[a-z0-9])(?=[A-Z])")

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

/**
 * Tiles in reading order, in as many equal columns as fit at [minTileWidth] or wider, sharing the
 * whole width: a row of fixed-width tiles leaves a ragged gap at its end, half the page at 2x text.
 * Each row is as tall as its tallest tile, so a label that wraps never pushes its neighbours out of
 * line. Placed relative to the layout direction.
 */
@Composable
fun TileGrid(
    minTileWidth: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
        val columns = ((width + gap) / (minTileWidth.roundToPx() + gap)).coerceAtLeast(1)
        val tileWidth = (width - gap * (columns - 1)) / columns
        val tile = Constraints(minWidth = tileWidth, maxWidth = tileWidth)
        val placeables = measurables.map { it.measure(tile) }
        val rowHeights = IntArray((placeables.size + columns - 1) / columns)
        placeables.forEachIndexed { index, placeable ->
            val row = index / columns
            rowHeights[row] = maxOf(rowHeights[row], placeable.height)
        }
        val height = (rowHeights.sum() + gap * (rowHeights.size - 1)).coerceAtLeast(0)
        layout(width, height) {
            var y = 0
            placeables.forEachIndexed { index, placeable ->
                val column = index % columns
                if (index > 0 && column == 0) y += rowHeights[index / columns - 1] + gap
                placeable.placeRelative(column * (tileWidth + gap), y)
            }
        }
    }
}
