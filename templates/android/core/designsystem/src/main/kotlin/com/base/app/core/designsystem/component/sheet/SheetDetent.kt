package com.base.app.core.designsystem.component.sheet

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.foundation.adaptive.SizeClass

/**
 * A height a sheet can rest at. A sheet lists one or more; the person moves between them by
 * dragging, flicking, tapping the grabber, or with the grabber's accessibility actions.
 *
 * Heights are measured within the safe area: above the navigation bar, or above the tab bar for a
 * non-modal sheet that rests on it. They resolve in the sheet's measure pass whenever the window,
 * the fold, the font scale or the content's height changes, never during a drag and never from the
 * keyboard. A detent may resolve to nothing: an inactive detent is skipped, as iOS skips medium in
 * compact height.
 *
 * More detents will be added: a `when` over a SheetDetent must carry an `else` branch.
 */
@Immutable
sealed interface SheetDetent {
    /** Nearly the whole window; on a phone the screen behind recedes into a card. Always active. */
    data object Large : SheetDetent

    /** About half the window: the sheet's top at the middle. Inactive in Compact height. */
    data object Medium : SheetDetent

    /**
     * The sheet's own height: grabber band, header, content and footer as measured without the
     * keyboard, capped at Large's height. Content that fills the height it is given (a lazy list)
     * is as tall as Large and behaves as Large. When the content's height changes, a sheet resting
     * here animates to the new height.
     */
    data object FitContent : SheetDetent

    /**
     * The grabber band and the header as measured: a peek that shows the header and nothing more,
     * and grows with the font scale. Without a header, the minimum height (48 dp).
     */
    data object Peek : SheetDetent

    /** [fraction] of the height Large shows, in (0, 1]. */
    data class Fraction(val fraction: Float) : SheetDetent {
        init {
            require(fraction > 0f && fraction <= 1f) { "A fraction is in (0, 1]: $fraction" }
        }
    }

    /** A fixed height. Font scale does not change it: a peek with text in it is [Peek]. */
    data class Height(val height: Dp) : SheetDetent

    /**
     * A height from [resolve], or null to leave the detent out in that context, as iOS's custom
     * resolvers do. Two Custom detents with the same [id] are the same detent; the latest [resolve]
     * passed with that id is the one used, so it may capture values from composition. Remember the
     * detent if its call site recomposes often. [resolve] runs in measure and must read no snapshot
     * state.
     */
    class Custom(val id: String, val resolve: SheetDetentContext.() -> Dp?) : SheetDetent {
        override fun equals(other: Any?): Boolean = other is Custom && other.id == id

        override fun hashCode(): Int = id.hashCode()

        override fun toString(): String = "Custom($id)"
    }
}

/** What a [SheetDetent.Custom] resolver can read. Every value is settled; none changes during a drag. */
interface SheetDetentContext : Density {
    /** Large's height: the most any detent shows. iOS's maximumDetentValue. */
    val maxHeight: Dp

    /** The height of the sheet's bounds: the window, the pane of a non-modal sheet, or a hinge half. */
    val containerHeight: Dp

    /** Grabber band, header, content and footer as measured without the keyboard. Always measured. */
    val contentHeight: Dp

    /** Grabber band and header as measured: what [SheetDetent.Peek] resolves to. */
    val headerHeight: Dp

    /** The placement class: the window's for a modal sheet; the pane's for a non-modal one. */
    val sizeClass: SizeClass

    /** The bottom of a horizontal hinge, from the top of the bounds, or null. */
    val foldBottom: Dp?

    /** Another built-in detent's height in this context, or null when it is inactive. Null for Custom. */
    fun heightOf(detent: SheetDetent): Dp?
}
