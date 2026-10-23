package com.example.standard.core.designsystem.component.sheet

import androidx.compose.runtime.Immutable

/** Behaviour a sheet seldom changes. */
@Immutable
class AppSheetProperties(
    val placement: SheetPlacement = SheetPlacement.Automatic,
    val contentInteraction: SheetContentInteraction = SheetContentInteraction.Automatic,
    val transition: SheetTransition = SheetTransition.Slide,
    val keyboard: SheetKeyboard = SheetKeyboard.Lift,
    /** False makes a tap on the dim do nothing; drag and Back still close the sheet. */
    val scrimTapDismisses: Boolean = true,
    /**
     * False holds the sheet on its detent: no drag and no resizing from the content's scroll, for a
     * signature pad, a canvas, a pannable chart or a payment in progress. Back, the dim and the close
     * button still follow `dismissible`; the grabber's accessibility actions stay.
     */
    val dragEnabled: Boolean = true,
    /**
     * False keeps the screen behind upright at Large: the dim only. For a video call, a camera
     * preview or a SurfaceView map behind the sheet.
     */
    val recedesPresenter: Boolean = true,
    /**
     * True starts the content at the surface's top edge, under the grabber, for artwork or a map
     * that fills the top corners; the grabber draws over it with a contrast pill.
     */
    val contentUnderGrabber: Boolean = false,
    /**
     * True keeps the content composed, parked, between openings, so opening it again composes
     * nothing. For a sheet opened often from one screen; it holds its memory meanwhile.
     */
    val keepContent: Boolean = false,
) {
    fun copy(
        placement: SheetPlacement = this.placement,
        contentInteraction: SheetContentInteraction = this.contentInteraction,
        transition: SheetTransition = this.transition,
        keyboard: SheetKeyboard = this.keyboard,
        scrimTapDismisses: Boolean = this.scrimTapDismisses,
        dragEnabled: Boolean = this.dragEnabled,
        recedesPresenter: Boolean = this.recedesPresenter,
        contentUnderGrabber: Boolean = this.contentUnderGrabber,
        keepContent: Boolean = this.keepContent,
    ): AppSheetProperties = AppSheetProperties(
        placement = placement,
        contentInteraction = contentInteraction,
        transition = transition,
        keyboard = keyboard,
        scrimTapDismisses = scrimTapDismisses,
        dragEnabled = dragEnabled,
        recedesPresenter = recedesPresenter,
        contentUnderGrabber = contentUnderGrabber,
        keepContent = keepContent,
    )

    override fun equals(other: Any?): Boolean = other is AppSheetProperties &&
        placement == other.placement && contentInteraction == other.contentInteraction &&
        transition == other.transition && keyboard == other.keyboard &&
        scrimTapDismisses == other.scrimTapDismisses && dragEnabled == other.dragEnabled &&
        recedesPresenter == other.recedesPresenter && contentUnderGrabber == other.contentUnderGrabber &&
        keepContent == other.keepContent

    override fun hashCode(): Int {
        var result = placement.hashCode()
        result = 31 * result + contentInteraction.hashCode()
        result = 31 * result + transition.hashCode()
        result = 31 * result + keyboard.hashCode()
        result = 31 * result + scrimTapDismisses.hashCode()
        result = 31 * result + dragEnabled.hashCode()
        result = 31 * result + recedesPresenter.hashCode()
        result = 31 * result + contentUnderGrabber.hashCode()
        return 31 * result + keepContent.hashCode()
    }

    override fun toString(): String = "AppSheetProperties(placement=$placement, " +
        "contentInteraction=$contentInteraction, transition=$transition, keyboard=$keyboard, " +
        "scrimTapDismisses=$scrimTapDismisses, dragEnabled=$dragEnabled, recedesPresenter=$recedesPresenter, " +
        "contentUnderGrabber=$contentUnderGrabber, keepContent=$keepContent)"
}

/**
 * Where the sheet sits. Automatic follows the window: from the bottom on a phone, a centred form sheet on a larger window for a modal sheet, a panel at
 * the start edge on a wide window for a non-modal one.
 */
@JvmInline
value class SheetPlacement private constructor(private val value: Int) {
    override fun toString(): String = when (this) {
        Automatic -> "Automatic"
        Bottom -> "Bottom"
        Center -> "Center"
        Start -> "Start"
        else -> "End"
    }

    companion object {
        val Automatic = SheetPlacement(0)
        val Bottom = SheetPlacement(1)
        val Center = SheetPlacement(2)

        /** The start edge in the layout direction. */
        val Start = SheetPlacement(3)

        /** The end edge in the layout direction. */
        val End = SheetPlacement(4)
    }
}

/** What a vertical scroll inside the sheet does first. */
@JvmInline
value class SheetContentInteraction private constructor(private val value: Int) {
    override fun toString(): String = when (this) {
        Automatic -> "Automatic"
        Resizes -> "Resizes"
        else -> "Scrolls"
    }

    companion object {
        /**
         * Resizes for a modal sheet dragged by touch or stylus; Scrolls for a non-modal sheet, for a
         * wheel, a touchpad or a mouse, and while TalkBack's touch exploration is on.
         */
        val Automatic = SheetContentInteraction(0)

        /** Grows the sheet to its top detent, then scrolls: iOS's default. */
        val Resizes = SheetContentInteraction(1)

        /** Scrolls at every detent; the sheet resizes from its grabber, header, footer and a pull at the content's top. */
        val Scrolls = SheetContentInteraction(2)
    }
}

/** How the sheet arrives and leaves. */
@JvmInline
value class SheetTransition private constructor(private val value: Int) {
    override fun toString(): String = if (this == Slide) "Slide" else "CrossFade"

    companion object {
        val Slide = SheetTransition(0)

        /** A fade in and a quicker fade out at the sheet's detent, from the theme's fade durations. */
        val CrossFade = SheetTransition(1)
    }
}

/** What the sheet does when the keyboard rises for a field inside it. */
@JvmInline
value class SheetKeyboard private constructor(private val value: Int) {
    override fun toString(): String = if (this == Lift) "Lift" else "Expand"

    companion object {
        /** Rises with the keyboard, keeping its height, up to Large. */
        val Lift = SheetKeyboard(0)

        /** Goes to its tallest detent while the keyboard is up, and back when it hides. */
        val Expand = SheetKeyboard(1)
    }
}
