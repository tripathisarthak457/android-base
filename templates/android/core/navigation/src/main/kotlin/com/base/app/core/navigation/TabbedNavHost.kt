package com.base.app.core.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.base.app.core.common.navigation.AppNavKey
import kotlinx.coroutines.flow.drop

/** Every opened tab's host, kept composed, with only the selected one measured and drawn. */
@Composable
internal fun TabbedNavHost(
    state: ShellState,
    registry: NavRegistry,
    motion: ScreenMotion,
    hidesBar: (AppNavKey) -> Boolean,
    modifier: Modifier = Modifier,
) {
    KeptTabs(selected = state.selectedIndex, generation = state.generation, modifier = modifier) { index ->
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

private val LocalTabReselections = compositionLocalOf<IntState?> { null }

/**
 * Scrolls [state] back to the top when the tab of this screen is tapped while it is already at its
 * root, as iOS does. Call it from a tab's root screen with its primary list's state.
 */
@Composable
fun ScrollToTopOnReselect(state: LazyListState) {
    val reselections = LocalTabReselections.current ?: return
    LaunchedEffect(reselections, state) {
        snapshotFlow { reselections.intValue }.drop(1).collect { state.animateScrollToItem(0) }
    }
}
