package com.example.finalafter.crui2.core.designsystem.component.sheet.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.finalafter.crui2.core.designsystem.foundation.layout.BarFrame
import com.example.finalafter.crui2.core.designsystem.foundation.layout.ShellSeamApi

/**
 * The window around a sheet, as its measure reads it. The insets are foundation's snapshot-backed
 * objects, so a measure that reads them follows the system bars; the keyboard is never among them,
 * because a keyboard must not re-measure a sheet on every frame of its animation (it is read in
 * placement, through [SheetImeSource]).
 */
@Stable
@OptIn(ShellSeamApi::class)
internal class SheetMeasureEnvironment(
    /** The status bar, a caption bar and the cutout: the safe area's top and sides. */
    private val safeArea: WindowInsets,
    private val navigationBars: WindowInsets,
    /** Non-zero at the bottom only under three-button navigation, where a sheet never floats. */
    private val tappableElement: WindowInsets,
    val barFrame: BarFrame?,
) {
    fun safeTop(density: Density): Float = safeArea.getTop(density).toFloat()

    fun safeLeft(density: Density, direction: LayoutDirection): Float = safeArea.getLeft(density, direction).toFloat()

    fun safeRight(density: Density, direction: LayoutDirection): Float = safeArea.getRight(density, direction).toFloat()

    fun navigationBottom(density: Density): Float = navigationBars.getBottom(density).toFloat()

    fun buttonNavigation(density: Density): Boolean = tappableElement.getBottom(density) > 0
}

/** The bounds a sheet lives in, within its container, and the hinge that may cross them. */
internal data class SheetBounds(val rect: Rect?, val fold: Rect?)
