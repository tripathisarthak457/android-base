package com.base.app.core.navigation.shell.tabs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.navigation.LocalTabReselections
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.ShellState
import com.base.app.core.navigation.stack.StackHost
import com.base.app.core.navigation.transition.ScreenMotion

/** Every opened tab's host, kept composed, with only the selected one drawn. */
@Composable
internal fun TabbedNavHost(
    state: ShellState,
    registry: NavRegistry,
    motion: ScreenMotion,
    hidesBar: (AppNavKey) -> Boolean,
    modifier: Modifier = Modifier,
) {
    KeptTabs(
        selected = state.selectedIndex,
        generation = state.generation,
        modifier = modifier,
        revealSwitch = state::revealSwitch,
    ) { index ->
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
