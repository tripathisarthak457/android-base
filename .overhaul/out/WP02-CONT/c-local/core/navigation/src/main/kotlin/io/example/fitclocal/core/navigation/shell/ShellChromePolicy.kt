package io.example.fitclocal.core.navigation.shell

import io.example.fitclocal.core.designsystem.foundation.adaptive.WindowHeight
import io.example.fitclocal.core.designsystem.foundation.adaptive.WindowWidth
import io.example.fitclocal.core.navigation.NavigationChrome

/**
 * Which chrome a window gets. A short window (a phone held sideways, a short split pane) has no
 * height to spare for a bar, so it gets a rail; a compact width gets the floating bar; wider
 * windows a rail, and from the large widths a sidebar, which a person can fold to the rail.
 * [sidebarOpen] is that person's choice, null until they make one: open from the large widths,
 * closed below them.
 */
internal fun chromeFor(width: WindowWidth, height: WindowHeight, sidebarOpen: Boolean?): NavigationChrome = when {
    height == WindowHeight.Compact -> NavigationChrome.Rail
    width == WindowWidth.Compact -> NavigationChrome.Bar
    width == WindowWidth.Medium -> NavigationChrome.Rail
    sidebarOpen ?: (width >= WindowWidth.Large) -> NavigationChrome.Sidebar
    else -> NavigationChrome.Rail
}

/** Whether the window is wide enough to offer the sidebar at all. */
internal fun sidebarAvailable(width: WindowWidth, height: WindowHeight): Boolean =
    height != WindowHeight.Compact && width >= WindowWidth.Expanded

/**
 * How many of [count] tabs the chrome shows; the rest are listed under More, which takes the last
 * place. Five fit a bar (Apple's and Google's guidance), seven a rail; the sidebar shows them all.
 */
internal fun NavigationChrome.tabsShown(count: Int): Int = when (this) {
    NavigationChrome.Bar -> if (count > MAX_BAR_TABS) MAX_BAR_TABS - 1 else count
    NavigationChrome.Rail -> if (count > MAX_RAIL_TABS) MAX_RAIL_TABS - 1 else count
    NavigationChrome.Sidebar -> count
}

private const val MAX_BAR_TABS = 5
private const val MAX_RAIL_TABS = 7
