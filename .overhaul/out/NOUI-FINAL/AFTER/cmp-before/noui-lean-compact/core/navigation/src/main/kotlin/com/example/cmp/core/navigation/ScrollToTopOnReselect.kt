package com.example.cmp.core.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop

/**
 * The reselections ([TabStacks.reselections]) of the tab a screen is in, which a tab host provides
 * around each tab's screens; null outside a tab.
 */
@NavigationHostApi
val LocalTabReselections = compositionLocalOf<IntState?> { null }

/**
 * Scrolls [state] back to the top when the tab of this screen is tapped while it is already at its
 * root, as iOS does. Call it from a tab's root screen with its primary list's state.
 */
@Composable
fun ScrollToTopOnReselect(state: LazyListState) {
    val reselections = LocalTabReselections.current ?: return
    LaunchedEffect(reselections, state) {
        snapshotFlow { reselections.intValue }.drop(1).collect { state.scrollToTop() }
    }
}

/**
 * Jumps to two screens from the top first, then animates the rest: from deep in a long list, an
 * animation over every row would compose each of them on the way up.
 */
private suspend fun LazyListState.scrollToTop() {
    val approach = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) * APPROACH_SCREENS
    if (firstVisibleItemIndex > approach) scrollToItem(approach)
    animateScrollToItem(0)
}

private const val APPROACH_SCREENS = 2
