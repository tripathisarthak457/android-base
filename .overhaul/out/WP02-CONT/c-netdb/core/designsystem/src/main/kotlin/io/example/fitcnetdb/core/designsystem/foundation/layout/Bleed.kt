package io.example.fitcnetdb.core.designsystem.foundation.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * Lets content run [amount] past its parent's horizontal padding on both sides: a carousel that
 * reaches the screen edges inside a column padded to the gutter.
 */
fun Modifier.bleed(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = amount.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
        ),
    )
    layout(placeable.width - extra, placeable.height) {
        placeable.placeRelative(-extra / 2, 0)
    }
}
