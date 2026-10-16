package com.base.app.core.designsystem.component.sheet.layout

import androidx.compose.ui.util.lerp

/**
 * The surface's outline as a bottom sheet goes from floating to attached (§6.3 of the sheet
 * specification). At a partial detent a floating sheet sits [inset] in from the sides and the
 * bottom; across the travel to Large ([expansion] 0 to 1) the side gaps close and the bottom edge
 * drops [radius] below the window, so at Large the sheet is square-bottomed and edge-attached
 * while its outline stays one uniform round rect, which the renderer clips without an offscreen
 * buffer.
 *
 * Content is measured once at the sheet's full width; only this outline, which clips it, changes.
 */
internal object SheetGeometry {

    /** The gap at each side: the inset while floating at a partial detent, closing to 0 at Large. */
    fun sideInset(inset: Float, floating: Boolean, expansion: Float): Float =
        if (floating) inset * (1f - expansion) else 0f

    /**
     * b₀, where the surface's bottom rests at a partial detent. A sheet anchored to the window floats
     * [inset] above its bottom or hangs [radius] below it; a non-modal sheet resting on the bottom
     * chrome sits on top of it ([bottomChrome]), floating [inset] above it.
     */
    fun restingBottom(boundsHeight: Float, inset: Float, floating: Boolean, radius: Float, bottomChrome: Float?): Float =
        when {
            bottomChrome != null -> boundsHeight - bottomChrome - if (floating) inset else 0f
            floating -> boundsHeight - inset
            else -> boundsHeight + radius
        }

    /** The surface's bottom edge: from b₀ at a partial detent to [radius] below the window at Large. */
    fun surfaceBottom(restingBottom: Float, boundsHeight: Float, radius: Float, expansion: Float): Float =
        lerp(restingBottom, boundsHeight + radius, expansion)

    /**
     * Writes a bottom sheet's outline into [into]: [frameLeft]..[frameRight] is the sheet's full
     * width, [top] its placed top.
     */
    fun bottomSheet(
        into: SurfaceRect,
        frameLeft: Float,
        frameRight: Float,
        top: Float,
        boundsHeight: Float,
        inset: Float,
        floating: Boolean,
        radius: Float,
        expansion: Float,
        bottomChrome: Float?,
    ) {
        val side = sideInset(inset, floating, expansion)
        val rest = restingBottom(boundsHeight, inset, floating, radius, bottomChrome)
        into.set(
            left = frameLeft + side,
            top = top,
            right = frameRight - side,
            bottom = maxOf(top, surfaceBottom(rest, boundsHeight, radius, expansion)),
            radius = radius,
        )
    }

    /** A form sheet or a side panel: a card of fixed [height] whose top is [top]. */
    fun card(into: SurfaceRect, frameLeft: Float, frameRight: Float, top: Float, height: Float, radius: Float) {
        into.set(left = frameLeft, top = top, right = frameRight, bottom = top + height, radius = radius)
    }
}

/** A round rect written in place every frame, so drawing a moving sheet allocates nothing. */
internal class SurfaceRect {
    var left = 0f
        private set
    var top = 0f
        private set
    var right = 0f
        private set
    var bottom = 0f
        private set
    var radius = 0f
        private set

    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun set(left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
        this.radius = radius
    }
}
