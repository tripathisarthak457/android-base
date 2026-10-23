package com.example.lean.core.designsystem.foundation.layout

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

/**
 * Height of floating chrome over one edge. The holder never changes; its value is snapshot state written with
 * settled heights, so a change recomposes only the code that reads it rather than every screen below the provider.
 */
@Stable
class ChromeInset {
    var value: Dp by mutableStateOf(0.dp)
}

/**
 * Chrome over the bottom edge: the tab bar, its accessory, or the keyboard when it hides the bar. One holder per
 * window, provided by AppTheme and written only by the shell, so a toast host above the shell can read it too.
 */
val LocalBottomChrome = staticCompositionLocalOf { ChromeInset() }

/**
 * Chrome over the top edge of the current screen: its top bar. Every AppScaffold provides its own holder, so two
 * screens composed during a push or pop never write the same state.
 */
val LocalTopChrome = staticCompositionLocalOf { ChromeInset() }

/**
 * The top bar of the settled screen in front, for the whole window, the way [LocalBottomChrome] publishes the
 * bottom: an overlay above the navigation hosts (a debug badge, a banner) keeps clear of a large title and its
 * search field with it. Written only by the shell, from [ScreenChrome.topBar], once the screens have stopped moving.
 */
val LocalWindowTopChrome = staticCompositionLocalOf { ChromeInset() }

/**
 * Whether the screen it is provided to is on display: its tab is in front and nothing covers it in its stack. Hidden
 * tabs and covered screens stay composed, so anything that runs on a clock (shimmer, a spinner, a blinking caret)
 * pauses while [isVisible] is false, and hidden content costs nothing per frame.
 *
 * The holder a screen gets never changes, so a tab switch or a push recomposes nothing through it. Read [isVisible]
 * in an effect (`snapshotFlow`) or a draw block.
 */
@Stable
class TabVisibility(private val visible: () -> Boolean) {
    val isVisible: Boolean get() = visible()
}

/** Whether the content here is on screen: false inside a hidden tab or a covered screen; see [TabVisibility]. */
val LocalTabVisible = staticCompositionLocalOf { AlwaysVisible }

private val AlwaysVisible = TabVisibility { true }

/**
 * True where the tab bar floats over the content: tab screens, pushed or not. False on a full-screen
 * destination, in a sheet or a cover, and outside the tab shell. A screen-level bottom bar
 * (AppBottomActionBar) belongs only where this is false; on a tab screen the bar's accessory slot
 * takes that role.
 */
val LocalTabBarVisible = compositionLocalOf { false }

/**
 * What a screen tells the chrome around it. The shell gives every screen in its stacks one of its
 * own and draws the window's chrome from the screen in front: its own bottom bar becomes the
 * window's bottom chrome while the tab bar is hidden, its top bar is published as
 * [LocalWindowTopChrome], the bottom edge fade blends into its [background], and a docked tab bar
 * shows its surface only while [contentBelow] (iOS's scroll edge appearance).
 *
 * The scaffold writes [bottomBar], [topBar] and [background]; the screen's primary list reports
 * [contentBelow] through [ReportScrollEdge]. Outside a navigation stack nothing reads them.
 */
@Stable
class ScreenChrome {
    /** The settled height of the screen's own bottom bar (an AppBottomActionBar); 0 without one. */
    var bottomBar: Dp by mutableStateOf(0.dp)

    /** The settled height of the screen's top bar, status bar included; 0 without one. */
    var topBar: Dp by mutableStateOf(0.dp)

    /** The colour the screen paints behind its content; Unspecified is the theme's background. */
    var background: Color by mutableStateOf(Color.Unspecified)

    internal var scrollEdge: ScrollableState? by mutableStateOf(null)

    /**
     * Whether the screen's content runs on past its bottom edge, under the chrome there. True for a
     * screen that reports nothing, so its chrome keeps the look that hides what passes under it.
     * Read it in an effect or a draw block: it flips as the list reaches or leaves its end.
     */
    val contentBelow: Boolean get() = scrollEdge?.canScrollForward ?: true
}

/** Provided to every screen of a navigation stack; outside one, a holder nobody reads. */
val LocalScreenChrome = staticCompositionLocalOf { ScreenChrome() }

/**
 * Reports [state], the screen's primary scrolling container, as what runs under the bottom chrome:
 * a docked tab bar is clear and the bottom edge fade is gone while the list rests on its end, and
 * both return as soon as content scrolls under them. Call it once per screen, beside the list.
 */
@Composable
fun ReportScrollEdge(state: ScrollableState) {
    val screen = LocalScreenChrome.current
    DisposableEffect(screen, state) {
        screen.scrollEdge = state
        onDispose { if (screen.scrollEdge === state) screen.scrollEdge = null }
    }
}

/** Which pane of its navigation page a screen is shown in (the shell's `NavPane`). */
enum class PaneRole { Single, List, Detail, Extra, Main, Supporting }

/**
 * The pane a screen is shown in. [beside] is true while another pane of its page shares the window
 * with it: a detail beside its list needs no Back arrow for the first level, since Back on the list's
 * side already leaves it, and a list marks the row of [selectedKey], the navigation key of the detail
 * open beside it. [detailDepth] counts the details opened in the pane, 1 for the first.
 */
@Immutable
data class PaneContext(
    val role: PaneRole = PaneRole.Single,
    val beside: Boolean = false,
    val detailDepth: Int = 0,
    val selectedKey: Any? = null,
)

/** Provided to every screen of a navigation stack; the default is a screen on its own. */
val LocalPaneContext = staticCompositionLocalOf { PaneContext() }

/**
 * The colour the host of the current screen has already painted under it edge to edge (a navigation
 * entry's frame, a presented sheet's or cover's), or Unspecified where nothing has. A scaffold on
 * such a screen paints no background of its own: the second fill would cost the GPU a whole window
 * on every frame of every transition, where it is least affordable.
 */
val LocalPaintedBackground = staticCompositionLocalOf { Color.Unspecified }

/**
 * [base] plus the chrome at both ends, for a scrolling container's contentPadding. The bottom is
 * never less than the navigation bar, so the last row clears the gesture handle and a three-button
 * bar where no chrome is written: beside a rail, on a screen that hides the tab bar, in a cover.
 *
 * The chrome is read when the container measures its padding, as window insets are: a bar settling
 * or the keyboard rising lays the list out again and recomposes nothing.
 */
@Composable
fun chromeContentPadding(base: PaddingValues = PaddingValues(0.dp)): PaddingValues {
    val top = LocalTopChrome.current
    val bottom = LocalBottomChrome.current
    val navigationBars = WindowInsets.navigationBars
    val density = LocalDensity.current
    return remember(base, top, bottom, navigationBars, density) {
        ChromePadding(base, top, bottom, navigationBars, density)
    }
}

@Stable
private class ChromePadding(
    private val base: PaddingValues,
    private val top: ChromeInset,
    private val bottom: ChromeInset,
    private val navigationBars: WindowInsets,
    private val density: Density,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = base.calculateLeftPadding(layoutDirection)

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = base.calculateRightPadding(layoutDirection)

    override fun calculateTopPadding(): Dp = base.calculateTopPadding() + top.value

    override fun calculateBottomPadding(): Dp {
        val system = with(density) { navigationBars.getBottom(this).toDp() }
        return base.calculateBottomPadding() + max(bottom.value, system)
    }
}
