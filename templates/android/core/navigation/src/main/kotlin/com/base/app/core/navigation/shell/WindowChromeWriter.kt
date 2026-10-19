package com.base.app.core.navigation.shell

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.navigation.AppTabBarState
import com.base.app.core.designsystem.foundation.layout.LocalBottomChrome
import com.base.app.core.designsystem.foundation.layout.LocalWindowTopChrome
import com.base.app.core.navigation.ShellState
import com.base.app.core.navigation.stack.lastContentKey

/**
 * Publishes the chrome of the settled screen in front for the whole window: what covers the bottom
 * edge ([LocalBottomChrome]) and the screen's top bar ([LocalWindowTopChrome]). Nothing is written
 * while the screens move, so what reads them is laid out again once per change, not per frame.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WindowChromeWriter(state: ShellState, barState: AppTabBarState, wide: Boolean) {
    val bottom = LocalBottomChrome.current
    val top = LocalWindowTopChrome.current
    val density = LocalDensity.current
    val keyboard = WindowInsets.imeAnimationTarget
    // Derived, so the last stretch of a slide, after the gate has let go, runs the block below once
    // as the bar is covered or uncovered rather than on each of its frames.
    val barCovered = remember(state) { derivedStateOf { state.barAnchor.progress() >= 1f } }
    // The holders are the window's: once the shell is gone (signed out), nothing covers either edge.
    DisposableEffect(bottom, top) {
        onDispose {
            bottom.value = 0.dp
            top.value = 0.dp
        }
    }
    LaunchedEffect(state, barState, wide, bottom, density, barCovered) {
        snapshotFlow {
            if (state.moving) {
                null
            } else {
                with(density) {
                    bottomChrome(
                        presented = state.presentedScreens[state.presentedKeys.lastContentKey()]?.bottomBar,
                        keyboard = keyboard.getBottom(this).toDp(),
                        screenBar = state.tabFront?.takeIf { barCovered.value }?.bottomBar,
                        bar = if (wide) 0.dp else barState.settledHeight.toDp(),
                    )
                }
            }
        }.collect { height -> if (height != null) bottom.value = height }
    }
    LaunchedEffect(state, top) {
        snapshotFlow {
            if (state.moving) {
                null
            } else {
                val presented = state.presentedKeys.lastContentKey()
                val screen = if (presented != null) state.presentedScreens[presented] else state.tabFront
                screen?.topBar ?: 0.dp
            }
        }.collect { height -> if (height != null) top.value = height }
    }
}

/**
 * The chrome over the bottom edge, in order of what covers what: a presented screen's own bottom
 * bar, the keyboard, a screen-level bar where the tab bar is hidden, the tab bar.
 */
internal fun bottomChrome(presented: Dp?, keyboard: Dp, screenBar: Dp?, bar: Dp): Dp = when {
    presented != null -> presented
    keyboard > 0.dp -> keyboard
    screenBar != null -> screenBar
    else -> bar
}
