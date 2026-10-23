package com.example.everything.core.designsystem.foundation.adaptive

import androidx.compose.runtime.Immutable
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass

/** Android's width classes, narrowest first, so they compare: `width >= WindowWidth.Expanded`. */
enum class WindowWidth { Compact, Medium, Expanded, Large, ExtraLarge }

/** Android's height classes. Compact is a phone held sideways, or a small split-screen pane. */
enum class WindowHeight { Compact, Medium, Expanded }

/**
 * How much room a window or a pane has, in Android's size classes and with Android's breakpoints,
 * so the app changes layout where every other app on the device does.
 *
 * Layouts decide from this, never from the device, its orientation or its screen: the same phone
 * can be a small split-screen pane, a foldable is two devices in one, and a tablet window can be
 * resized to anything on a desktop. Two sizes in the same classes are equal, so code that reads a
 * size class recomposes when a breakpoint is crossed, not on every pixel of a resize.
 */
@Immutable
data class SizeClass(val width: WindowWidth, val height: WindowHeight) {

    /** Room for two panes side by side: a list and its detail, a picture and its words. */
    val hasRoomForTwoPanes: Boolean get() = width >= WindowWidth.Expanded

    /** Short enough that stacking content vertically wastes the width and hides the rest. */
    val isShort: Boolean get() = height == WindowHeight.Compact

    companion object {
        /** A typical phone held upright, for previews and tests that render outside a real window. */
        val Phone = SizeClass(WindowWidth.Compact, WindowHeight.Medium)

        fun of(widthDp: Float, heightDp: Float): SizeClass {
            val sizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(widthDp, heightDp)
            val width = when {
                sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND) -> WindowWidth.ExtraLarge
                sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> WindowWidth.Large
                sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> WindowWidth.Expanded
                sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> WindowWidth.Medium
                else -> WindowWidth.Compact
            }
            val height = when {
                sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND) -> WindowHeight.Expanded
                sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> WindowHeight.Medium
                else -> WindowHeight.Compact
            }
            return SizeClass(width, height)
        }
    }
}
