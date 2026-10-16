package com.base.app.core.designsystem.component.sheet

import androidx.compose.runtime.Immutable

/** Whether what is behind the sheet stays usable. */
@Immutable
sealed interface SheetModality {
    /** Dims and blocks what is behind at every detent and hides it from screen readers; drawn over the tab bar. */
    data object Modal : SheetModality

    /**
     * What is behind stays usable, undimmed and visible to screen readers while the sheet rests at
     * [upThrough] or lower (every detent when null), as iOS's
     * presentationBackgroundInteraction(.enabled(upThrough:)). Above it the dim fades in over the
     * travel to the next detent up, and the sheet blocks like a modal one. With [aboveBottomChrome]
     * the sheet rests above the floating tab bar at its undimmed detents.
     */
    data class NonModal(
        val upThrough: SheetDetent? = null,
        val aboveBottomChrome: Boolean = true,
    ) : SheetModality
}

/** How the person tried to close a sheet. */
enum class SheetDismissSource { Drag, Scrim, Back, Escape, Request }
