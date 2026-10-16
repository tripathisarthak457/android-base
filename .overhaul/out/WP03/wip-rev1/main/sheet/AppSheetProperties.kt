package com.base.app.core.designsystem.component.sheet

import androidx.compose.runtime.Immutable

/** Behaviour a sheet seldom changes. */
@Immutable
data class AppSheetProperties(
    val placement: SheetPlacement = SheetPlacement.Automatic,
    val contentInteraction: SheetContentInteraction = SheetContentInteraction.Automatic,
    val transition: SheetTransition = SheetTransition.Slide,
    val keyboard: SheetKeyboard = SheetKeyboard.Lift,
    /** False makes a tap on the dim do nothing; drag and Back still close the sheet. */
    val scrimTapDismisses: Boolean = true,
)

/**
 * Where the sheet sits. Automatic follows the window, as iOS 27's presentationPlacement(.automatic):
 * from the bottom on a phone, a centred form sheet on a tablet for a modal sheet, a panel at the
 * start edge on a wide window for a non-modal one. Start and End are iOS's leading and trailing.
 */
enum class SheetPlacement { Automatic, Bottom, Center, Start, End }

/** What a vertical scroll inside the sheet does first. */
enum class SheetContentInteraction {
    /** Resizes for a modal sheet on touch; Scrolls for a non-modal sheet and for a window with only a mouse. */
    Automatic,

    /** Grows the sheet to its top detent, then scrolls: iOS's default. */
    Resizes,

    /** Scrolls at every detent; the sheet resizes from its grabber, header, footer and a pull at the content's top. */
    Scrolls,
}

/** How the sheet arrives and leaves. Reduce motion turns every transition into CrossFade. */
@Immutable
sealed interface SheetTransition {
    data object Slide : SheetTransition

    data object CrossFade : SheetTransition
}

/** What the sheet does when the keyboard rises. */
enum class SheetKeyboard {
    /** Rises with the keyboard, keeping its height, up to Large. */
    Lift,

    /** Goes to its tallest detent while the keyboard is up, and back when it hides. */
    Expand,
}
