package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass

/** Android's width classes, narrowest first, so they compare: `width >= WindowWidth.Expanded`. */
enum class WindowWidth { Compact, Medium, Expanded, Large, ExtraLarge }

/** Android's height classes. Compact is a phone held sideways, or a small split-screen pane. */
enum class WindowHeight { Compact, Medium, Expanded }

/**
 * How much room the app's window has, in Android's window size classes.
 *
 * Layouts decide from this, never from the device, its orientation or its screen: the same phone
 * can be a small split-screen pane, a foldable is two devices in one, and a tablet window can be
 * resized to anything on a desktop. Read it through [AppTheme.windowSize].
 */
@Immutable
class AppWindowSize(val widthDp: Float, val heightDp: Float) {

    private val sizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(widthDp, heightDp)

    val width: WindowWidth = when {
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND) -> WindowWidth.ExtraLarge
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> WindowWidth.Large
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> WindowWidth.Expanded
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> WindowWidth.Medium
        else -> WindowWidth.Compact
    }

    val height: WindowHeight = when {
        sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND) -> WindowHeight.Expanded
        sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> WindowHeight.Medium
        else -> WindowHeight.Compact
    }

    /** Room for two panes side by side: a list and its detail, a picture and its words. */
    val hasRoomForTwoPanes: Boolean get() = width >= WindowWidth.Expanded

    /** Short enough that stacking content vertically wastes the width and hides the rest. */
    val isShort: Boolean get() = height == WindowHeight.Compact

    override fun equals(other: Any?): Boolean =
        other is AppWindowSize && other.widthDp == widthDp && other.heightDp == heightDp

    override fun hashCode(): Int = 31 * widthDp.hashCode() + heightDp.hashCode()

    override fun toString(): String = "AppWindowSize($width × $height, ${widthDp}dp × ${heightDp}dp)"
}

/**
 * Layout measurements that follow the window. Components and screens read these instead of
 * choosing numbers, so one change here reflows the app on every form factor.
 */
@Immutable
data class AppLayout(
    /** The margin between content and the window's edge. */
    val gutter: Dp,
    /** How wide a bottom sheet, dialog or snackbar gets before it stops stretching. */
    val sheetMaxWidth: Dp = 640.dp,
    /** A single form: sign-in, a short edit screen. Wider than this and the fields read as lines. */
    val formMaxWidth: Dp = 480.dp,
    /** Prose and settings-style lists, about 70 characters to the line. */
    val readableMaxWidth: Dp = 720.dp,
    /** The widest a single column of mixed content gets before it is centred. */
    val contentMaxWidth: Dp = 1040.dp,
    /** The list half of a list-detail layout. */
    val listPaneWidth: Dp,
    /** The narrowest a card is allowed to get in a grid; the column count follows from it. */
    val gridMinCellWidth: Dp = 300.dp,
) {
    companion object {
        fun forWindow(size: AppWindowSize): AppLayout = when (size.width) {
            WindowWidth.Compact -> AppLayout(gutter = 16.dp, listPaneWidth = 360.dp)
            WindowWidth.Medium -> AppLayout(gutter = 24.dp, listPaneWidth = 320.dp)
            WindowWidth.Expanded -> AppLayout(gutter = 24.dp, listPaneWidth = 360.dp)
            WindowWidth.Large, WindowWidth.ExtraLarge -> AppLayout(gutter = 32.dp, listPaneWidth = 412.dp)
        }
    }
}

/** A typical phone held upright, for previews and tests that render outside a real window. */
internal val PhoneWindow = AppWindowSize(widthDp = 411f, heightDp = 891f)

internal val LocalAppWindowSize = staticCompositionLocalOf { PhoneWindow }
internal val LocalAppLayout = staticCompositionLocalOf { AppLayout.forWindow(PhoneWindow) }

/** The size of the window the app is drawn in right now, which changes as it is resized. */
@Composable
internal fun rememberAppWindowSize(): AppWindowSize {
    val container = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return remember(container, density) {
        if (container.width == 0 || container.height == 0) {
            PhoneWindow
        } else {
            with(density) { AppWindowSize(container.width.toDp().value, container.height.toDp().value) }
        }
    }
}
