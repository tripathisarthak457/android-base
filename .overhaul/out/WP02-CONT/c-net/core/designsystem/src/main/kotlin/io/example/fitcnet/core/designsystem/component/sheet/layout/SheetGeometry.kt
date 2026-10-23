package io.example.fitcnet.core.designsystem.component.sheet.layout

import androidx.compose.ui.util.lerp

/**
 * The surface's outline as a bottom sheet goes from floating to attached, in the sheet layer's
 * own coordinates: its top-left is the placed top of the sheet.
 *
 * At a partial detent a floating sheet sits [inset] in from the sides and the bottom; across the
 * expansion `e` (0 at the highest partial stop, 1 at Large) the side gaps close and the bottom edge
 * drops the corner radius below the bounds, so at Large the sheet is edge-attached while its outline
 * stays one uniform round rect, which the renderer clips without an offscreen buffer. Content is
 * measured once at the sheet's full width; only this outline, which clips it, changes.
 */
internal object SheetGeometry {

    /** fl · (1 − e): how far the sheet floats. 0 attached, so its outline is constant. */
    fun floatAmount(floats: Boolean, expansion: Float): Float = if (floats) 1f - expansion else 0f

    /** The gap at each side. */
    fun sideInset(inset: Float, floatAmount: Float): Float = inset * floatAmount

    /**
     * b₀: where a resting surface's bottom edge is, in bounds pixels. A sheet anchored to the bounds
     * floats [inset] above their bottom or hangs [radius] below it; a sheet resting on the tab bar
     * sits on [restBottom], floating [inset] above it.
     */
    fun restingBottom(
        boundsHeight: Float,
        restBottom: Float,
        inset: Float,
        floats: Boolean,
        radius: Float,
        onBar: Boolean,
    ): Float = when {
        onBar -> restBottom - if (floats) inset else 0f
        floats -> boundsHeight - inset
        else -> boundsHeight + radius
    }

    /**
     * The surface's bottom edge in layer pixels while the sheet floats: from b₀ at a partial stop to
     * [radius] below the bounds at Large. It stays put in the bounds while the sheet moves, so a
     * floating sheet never shows a gap beneath itself.
     */
    fun floatingBottom(restingBottom: Float, boundsHeight: Float, radius: Float, expansion: Float, placedTop: Float): Float =
        lerp(restingBottom, boundsHeight + radius, expansion) - placedTop

    /**
     * The surface's bottom edge in layer pixels while the sheet is attached: past the bounds' bottom
     * however far the sheet is pulled up into its rubber band, so the window cuts it, never a gap.
     */
    fun attachedBottom(layerHeight: Float, radius: Float, bandRoom: Float): Float = layerHeight + radius + bandRoom
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

    fun same(left: Float, top: Float, right: Float, bottom: Float, radius: Float): Boolean =
        this.left == left && this.top == top && this.right == right && this.bottom == bottom && this.radius == radius
}
