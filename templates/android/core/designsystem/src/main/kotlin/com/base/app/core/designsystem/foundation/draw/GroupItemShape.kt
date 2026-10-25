package com.base.app.core.designsystem.foundation.draw

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

/**
 * The shape of one row in a group drawn as a single card from separate items (a lazy list cannot
 * wrap its rows in one background): the first row takes the card's top corners, the last its bottom
 * ones, and the rows between are square.
 */
public fun Shape.groupItem(first: Boolean, last: Boolean): Shape {
    if (this !is CornerBasedShape) return this
    val none = CornerSize(0)
    return when {
        first && last -> this
        first -> copy(bottomStart = none, bottomEnd = none)
        last -> copy(topStart = none, topEnd = none)
        else -> RectangleShape
    }
}
