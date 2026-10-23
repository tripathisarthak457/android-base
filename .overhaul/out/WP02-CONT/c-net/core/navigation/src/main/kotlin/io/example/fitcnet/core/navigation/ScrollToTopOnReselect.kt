package io.example.fitcnet.core.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshotFlow
import io.example.fitcnet.core.designsystem.foundation.scroll.animateScrollToTop
import kotlinx.coroutines.flow.drop

internal val LocalTabReselections = compositionLocalOf<IntState?> { null }

/**
 * Scrolls [state] back to the top when the tab of this screen is tapped while it is already at its
 * root, as iOS does. Call it from a tab's root screen with its primary list's state.
 */
@Composable
fun ScrollToTopOnReselect(state: LazyListState) {
    val reselections = LocalTabReselections.current ?: return
    LaunchedEffect(reselections, state) {
        snapshotFlow { reselections.intValue }.drop(1).collect { state.animateScrollToTop() }
    }
}
