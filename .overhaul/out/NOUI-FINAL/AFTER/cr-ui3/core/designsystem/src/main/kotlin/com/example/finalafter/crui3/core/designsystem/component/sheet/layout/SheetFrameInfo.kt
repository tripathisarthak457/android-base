package com.example.finalafter.crui3.core.designsystem.component.sheet.layout

import androidx.compose.runtime.Immutable
import com.example.finalafter.crui3.core.designsystem.component.sheet.SheetPlacement

/**
 * One sheet's frame as measure resolved it, in pixels of its bounds: what placement, the surface,
 * the footer and the stack read between two layouts. Replaced as a whole and only when a value
 * changes, so a reader in draw follows a new layout and nothing else.
 */
@Immutable
internal data class SheetFrameInfo(
    /** Bottom, Center, Start or End. */
    val placement: SheetPlacement,
    /** The bounds within the sheet's container: the window, a pane or a hinge half. */
    val boundsLeft: Float,
    val boundsTop: Float,
    val boundsWidth: Float,
    val boundsHeight: Float,
    /** The sheet layer's left edge within the bounds, and its size. */
    val sheetLeft: Float,
    val sheetWidth: Float,
    val layerHeight: Float,
    val largeTop: Float,
    val restBottom: Float,
    /** H − restBottom: the navigation bar, or the tab bar a non-modal sheet rests on. */
    val bottomInset: Float,
    /** Resting on the tab bar. */
    val onBar: Boolean,
    /** fl: a bottom sheet that floats at its partial stops. */
    val floats: Boolean,
    /** i₀ and R. */
    val inset: Float,
    val radius: Float,
    /** How far past the window an attached surface reaches, so a pull into the band shows no gap. */
    val bandRoom: Float,
    /** The grabber band or the header above the content, and the header's own height. */
    val bandHeight: Float,
    val headerTop: Float,
    val headerHeight: Float,
    val footerHeight: Float,
    val footerPinned: Boolean,
    /** A form sheet's or a side panel's height; NaN for a bottom sheet. */
    val cardHeight: Float,
    /** A modal side panel travels along x; its offset is its distance from where it rests. */
    val horizontal: Boolean,
    /** The panel's attached edge is the window's left one. */
    val fromLeft: Boolean,
    /** Recedes its presenter at Large: modal, from the bottom of a compact window. */
    val recedes: Boolean,
    /** As wide as the bounds, so at Large nothing of the presenter shows below it. */
    val spansWidth: Boolean,
    val scrollEdgeRamp: Float,
) {
    val isBottom: Boolean get() = placement == SheetPlacement.Bottom
    val isCard: Boolean get() = !cardHeight.isNaN()
}
