package com.base.app.core.designsystem.foundation.draw

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A solid copy of [shape] drawn behind the element, [distance] down and to the right. The outline
 * is built once per size, not on every frame the element redraws.
 */
public fun Modifier.offsetShadow(shape: Shape, distance: Dp, color: Color): Modifier {
    if (distance <= 0.dp) return this
    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val shift = distance.toPx()
        onDrawBehind {
            translate(left = shift, top = shift) { drawOutline(outline, color) }
        }
    }
}
