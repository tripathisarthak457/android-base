package com.example.wp04.everything.core.designsystem.component.sheet.layout

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.WindowInsetsRulers

/**
 * The keyboard as a sheet reads it, in the sheet's placement only: its height this frame, where its
 * animation ends, and whether one runs. Production reads the window's IME rulers, which only a
 * placement scope can read; tests drive a double on their own clock, because a JVM test can inject
 * only static insets and no inset animation.
 */
internal interface SheetImeSource {
    /** The keyboard's height from the bottom of a layout [height] tall, this frame. */
    fun Placeable.PlacementScope.current(height: Float): Float

    /** The height the keyboard's running animation ends at; [current] when none runs. */
    fun Placeable.PlacementScope.target(height: Float): Float
}

internal object RulerImeSource : SheetImeSource {
    override fun Placeable.PlacementScope.current(height: Float): Float {
        val top = WindowInsetsRulers.Ime.current.bottom.current(height)
        return (height - top).coerceAtLeast(0f)
    }

    override fun Placeable.PlacementScope.target(height: Float): Float {
        val animation = WindowInsetsRulers.Ime.getAnimation(this)
        if (!animation.isAnimating) return current(height)
        val top = animation.target.bottom.current(height)
        return (height - top).coerceAtLeast(0f)
    }
}

/** Swapped by tests for a keyboard on the test clock. */
internal val LocalSheetImeSource = staticCompositionLocalOf<SheetImeSource> { RulerImeSource }
