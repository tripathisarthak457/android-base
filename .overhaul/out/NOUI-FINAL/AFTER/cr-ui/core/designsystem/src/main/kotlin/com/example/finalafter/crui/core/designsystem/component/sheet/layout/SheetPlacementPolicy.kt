package com.example.finalafter.crui.core.designsystem.component.sheet.layout

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.LayoutDirection
import com.example.finalafter.crui.core.designsystem.component.sheet.SheetPlacement
import com.example.finalafter.crui.core.designsystem.foundation.adaptive.SizeClass
import com.example.finalafter.crui.core.designsystem.foundation.adaptive.WindowHeight
import com.example.finalafter.crui.core.designsystem.foundation.adaptive.WindowWidth

/**
 * Where a sheet sits in its bounds, decided from the placement
 * class: the window's for a modal sheet, its pane's for a non-modal one, or a hinge half's. Never
 * [SheetPlacement.Automatic].
 *
 * The physical rows come first: on a tabletop crease or in a short window an automatic sheet rises
 * from the bottom. Explicit Bottom and Center are honoured anywhere; Start and End need an expanded
 * width and fall back to the bottom below it, where there is no room for a panel.
 */
internal fun resolvePlacement(
    requested: SheetPlacement,
    modal: Boolean,
    sizeClass: SizeClass,
    tabletop: Boolean,
): SheetPlacement = when (requested) {
    SheetPlacement.Bottom, SheetPlacement.Center -> requested
    SheetPlacement.Start, SheetPlacement.End ->
        if (sizeClass.width >= WindowWidth.Expanded) requested else SheetPlacement.Bottom
    else -> when {
        tabletop -> SheetPlacement.Bottom
        sizeClass.height == WindowHeight.Compact -> SheetPlacement.Bottom
        sizeClass.width == WindowWidth.Compact -> SheetPlacement.Bottom
        modal -> SheetPlacement.Center
        sizeClass.width == WindowWidth.Medium -> SheetPlacement.Bottom
        else -> SheetPlacement.Start
    }
}

/**
 * The half of a window split by a vertical hinge that a sheet lives in, in window pixels, or null
 * when nothing splits it: the half holding [presenterX] (the presenting pane's or the anchor's
 * centre), else the larger half, else the end half in [layoutDirection]. A sheet never lies across
 * the hinge.
 */
internal fun hingeHalf(
    windowWidth: Float,
    windowHeight: Float,
    hinge: Rect?,
    presenterX: Float?,
    layoutDirection: LayoutDirection,
): Rect? {
    if (hinge == null || hinge.height < hinge.width) return null
    val left = Rect(0f, 0f, hinge.left, windowHeight)
    val right = Rect(hinge.right, 0f, windowWidth, windowHeight)
    return when {
        presenterX != null -> if (presenterX < hinge.left) left else right
        left.width > right.width -> left
        right.width > left.width -> right
        layoutDirection == LayoutDirection.Ltr -> right
        else -> left
    }
}

/** The bottom of a horizontal hinge relative to [boundsTop], when it lies inside the bounds. */
internal fun foldBottom(hinge: Rect?, boundsTop: Float, boundsHeight: Float): Float? {
    if (hinge == null || hinge.width < hinge.height) return null
    val bottom = hinge.bottom - boundsTop
    return bottom.takeIf { it > 0f && it < boundsHeight }
}
