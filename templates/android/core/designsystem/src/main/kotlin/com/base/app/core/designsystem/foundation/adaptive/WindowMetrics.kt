package com.base.app.core.designsystem.foundation.adaptive

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The window's exact size, for code that needs more than a size class: a transition that moves a
 * screen by its width, a layout that splits at a hinge. Read [sizeDp] inside a lambda that runs in
 * layout, drawing or an animation; read in composition it would recompose on every pixel of a
 * resize, which is what size classes exist to avoid.
 */
@Stable
public class WindowMetrics internal constructor(private val size: () -> DpSize) {
    public val sizeDp: DpSize get() = size()
}

internal val PhoneMetrics = WindowMetrics { DpSize(411.dp, 891.dp) }
