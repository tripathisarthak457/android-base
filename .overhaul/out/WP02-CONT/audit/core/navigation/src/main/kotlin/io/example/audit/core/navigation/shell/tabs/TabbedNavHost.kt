package io.example.audit.core.navigation.shell.tabs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.example.audit.core.common.navigation.AppNavKey
import io.example.audit.core.designsystem.foundation.layout.LocalScreenChrome
import io.example.audit.core.navigation.LocalTabReselections
import io.example.audit.core.navigation.NavRegistry
import io.example.audit.core.navigation.ShellState
import io.example.audit.core.navigation.ShellTab
import io.example.audit.core.navigation.shell.MoreList
import io.example.audit.core.navigation.stack.StackHost
import io.example.audit.core.navigation.transition.ScreenMotion

/**
 * Every opened tab's host, kept composed, with only the selected one drawn; and the More list, kept
 * the same way after the last tab. The tabs the chrome shows are built while the app is idle, so
 * the first tap on one is as cheap as any other; those under More wait for their first visit.
 */
@Composable
internal fun TabbedNavHost(
    tabs: List<ShellTab>,
    labels: List<String>,
    state: ShellState,
    registry: NavRegistry,
    motion: ScreenMotion,
    hidesBar: (AppNavKey) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val more = tabs.size
    val warmUp = remember(state.overflowFrom) { List(minOf(state.overflowFrom, MAX_WARM_TABS)) { it } }
    val selected = remember(state, more) { { if (state.moreShown) more else state.selectedIndex } }
    KeptTabs(
        selected = selected,
        generation = state.generation,
        modifier = modifier,
        revealSwitch = state::revealSwitch,
        // More takes the place of the first tab with no room; the tabs it lists open from it, as
        // a push would.
        positionOf = { index ->
            when {
                index == more -> state.overflowFrom
                index >= state.overflowFrom -> state.overflowFrom + 1
                else -> index
            }
        },
        warmUp = warmUp,
    ) { index ->
        if (index == more) {
            CompositionLocalProvider(LocalScreenChrome provides state.moreScreen) {
                MoreList(tabs = tabs, labels = labels, from = state.overflowFrom, onSelect = state::select)
            }
        } else {
            CompositionLocalProvider(LocalTabReselections provides state.reselections[index]) {
                StackHost(
                    stack = state.tabs[index],
                    registry = registry,
                    motion = motion,
                    hidesBar = hidesBar,
                    anchor = state.anchors[index],
                    screens = state.screens[index],
                )
            }
        }
    }
}

/** Five, as many as the bar shows: each warmed tab stays in memory whether or not it is visited. */
private const val MAX_WARM_TABS = 5
