package com.base.app.core.designsystem.component.sheet

import androidx.compose.runtime.Immutable

/** Whether what is behind the sheet stays usable. More may be added: a `when` needs `else`. */
@Immutable
public sealed interface SheetModality {
    /**
     * Dims and blocks what is behind at every detent, hides it from screen readers and takes focus;
     * drawn over the tab bar.
     */
    public data object Modal : SheetModality

    /**
     * What is behind stays usable, undimmed and visible to screen readers while the sheet rests at
     * [upThrough] or lower (every detent when null), as iOS's
     * presentationBackgroundInteraction(.enabled(upThrough:)). Above it the dim fades in over the
     * travel to the next detent up, and the sheet blocks like a modal one. [upThrough] must be one
     * of the sheet's detents. With [aboveBottomChrome] the sheet rests above the floating tab bar at
     * its undimmed detents.
     */
    @Immutable
    public class NonModal(
        public val upThrough: SheetDetent? = null,
        public val aboveBottomChrome: Boolean = true,
    ) : SheetModality {
        public fun copy(
            upThrough: SheetDetent? = this.upThrough,
            aboveBottomChrome: Boolean = this.aboveBottomChrome,
        ): NonModal = NonModal(upThrough, aboveBottomChrome)

        override fun equals(other: Any?): Boolean =
            other is NonModal && upThrough == other.upThrough &&
                aboveBottomChrome == other.aboveBottomChrome

        override fun hashCode(): Int = 31 * upThrough.hashCode() + aboveBottomChrome.hashCode()

        override fun toString(): String =
            "NonModal(upThrough=$upThrough, aboveBottomChrome=$aboveBottomChrome)"
    }
}

/**
 * How the person tried to close a sheet. TalkBack's dismiss action reports [Request]; a tap outside
 * an anchored action card reports [Scrim]. More may be added: a `when` needs `else`.
 */
public enum class SheetDismissSource { Drag, Scrim, Back, Escape, Request }
