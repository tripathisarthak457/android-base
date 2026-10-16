package com.base.app.core.designsystem.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A solid copy of [shape] drawn behind the element, [distance] down and to the right. */
fun Modifier.offsetShadow(shape: Shape, distance: Dp, color: Color): Modifier {
    if (distance <= 0.dp) return this
    return drawBehind {
        val shift = distance.toPx()
        val outline = shape.createOutline(size, layoutDirection, this)
        translate(left = shift, top = shift) { drawOutline(outline, color) }
    }
}
