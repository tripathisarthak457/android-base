package com.base.app.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

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
