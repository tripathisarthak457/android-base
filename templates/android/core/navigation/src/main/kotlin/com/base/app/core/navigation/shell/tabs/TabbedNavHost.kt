package com.base.app.core.navigation.shell.tabs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.navigation.LocalTabReselections
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.ShellState
import com.base.app.core.navigation.ShellTab
import com.base.app.core.navigation.shell.MoreList
import com.base.app.core.navigation.stack.StackHost
import com.base.app.core.navigation.transition.ScreenMotion

/**
 * Every opened tab's host, kept composed, with only the selected one drawn; and the More list, kept
 * the same way after the last tab.
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
    KeptTabs(
        selected = if (state.moreShown) more else state.selectedIndex,
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
    ) { index ->
        if (index == more) {
            MoreList(tabs = tabs, labels = labels, from = state.overflowFrom, onSelect = state::select)
        } else {
            CompositionLocalProvider(LocalTabReselections provides state.reselections[index]) {
                StackHost(
                    stack = state.tabs[index],
                    registry = registry,
                    motion = motion,
                    hidesBar = hidesBar,
                    anchor = state.anchors[index],
                    screenBars = state.screenBars[index],
                )
            }
        }
    }
}
