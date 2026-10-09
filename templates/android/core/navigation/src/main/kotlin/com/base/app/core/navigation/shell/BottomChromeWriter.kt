package com.base.app.core.navigation.shell

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.navigation.AppTabBarState
import com.base.app.core.designsystem.foundation.LocalBottomChrome
import com.base.app.core.navigation.ShellState

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BottomChromeWriter(state: ShellState, barState: AppTabBarState, wide: Boolean) {
    val chrome = LocalBottomChrome.current
    val density = LocalDensity.current
    val keyboard = WindowInsets.imeAnimationTarget
    // The holder is the window's: once the shell is gone (signed out), nothing covers the bottom edge.
    DisposableEffect(chrome) {
        onDispose { chrome.value = 0.dp }
    }
    LaunchedEffect(state, barState, wide, chrome, density) {
        snapshotFlow {
            if (state.moving) {
                null
            } else {
                with(density) {
                    bottomChrome(
                        presented = state.presentedKeys.lastOrNull()?.let { state.presentedBars.heights[it]?.value },
                        keyboard = keyboard.getBottom(this).toDp(),
                        screenBar = state.current.current.takeIf { state.anchors[state.selectedIndex].progress() >= 1f }
                            ?.let { state.screenBars[state.selectedIndex].heights[it]?.value },
                        bar = if (wide) 0.dp else barState.settledHeight.toDp(),
                    )
                }
            }
        }.collect { height -> if (height != null) chrome.value = height }
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
