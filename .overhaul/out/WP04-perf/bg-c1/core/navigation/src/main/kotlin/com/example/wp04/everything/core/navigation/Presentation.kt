package com.example.wp04.everything.core.navigation

/**
 * A destination shown above the tab bar instead of inside a tab: registered with
 * `entry<K>(presentation = …)`, it goes to the shell's presentation stack whoever navigates to it,
 * so no feature picks a layer by hand.
 */
sealed interface Presentation {

    /**
     * A sheet over the current screen, which stays composed and visible beneath it. Drawn by the
     * shell's `sheetStrategy`; a shell without one presents it as a [Cover], and [detents] then
     * have no effect.
     */
    data class Sheet(val detents: List<SheetDetent> = listOf(SheetDetent.Large)) : Presentation

    /** A full-screen cover that rises from the bottom edge and hides the tab bar while it is up. */
    data object Cover : Presentation
}

/** Heights a sheet rests at. */
enum class SheetDetent {
    /** About half the window, the screen beneath still in view. */
    Medium,

    /** Nearly the whole window, a strip of the screen beneath showing at the top. */
    Large,
}
