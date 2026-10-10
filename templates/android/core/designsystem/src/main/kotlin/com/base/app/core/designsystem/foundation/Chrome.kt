package com.base.app.core.designsystem.foundation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
 * Where a screen's own bottom bar reports its settled height. The shell gives every screen its own
 * holder and, while that screen is the settled one in front with the tab bar hidden, reports this
 * height through [LocalBottomChrome], so toasts stay above the action bar.
 */
val LocalScreenBottomBar = staticCompositionLocalOf { ChromeInset() }

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

/** [base] plus the chrome at both ends, for a scrolling container's contentPadding. */
@Composable
fun chromeContentPadding(base: PaddingValues = PaddingValues(0.dp)): PaddingValues {
    val top = LocalTopChrome.current
    val bottom = LocalBottomChrome.current
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = base.calculateStartPadding(direction),
        top = base.calculateTopPadding() + top.value,
        end = base.calculateEndPadding(direction),
        bottom = base.calculateBottomPadding() + bottom.value,
    )
}
