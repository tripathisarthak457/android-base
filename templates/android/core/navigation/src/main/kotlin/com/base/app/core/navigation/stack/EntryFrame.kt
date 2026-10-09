package com.base.app.core.navigation.stack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.foundation.ChromeInset
import com.base.app.core.designsystem.foundation.LocalBottomChrome
import com.base.app.core.designsystem.foundation.LocalScreenBottomBar
import com.base.app.core.designsystem.foundation.LocalTabBarVisible
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.transition.ScreenMotion
import com.base.app.core.navigation.transition.arrival
import com.base.app.core.navigation.transition.coverScrim
import com.base.app.core.navigation.transition.coverScrimAlpha

/**
 * What every screen is drawn in: an opaque background (two screens composed at once would
 * otherwise show through each other), the scrim it draws over the screen it covers, and its own
 * holder for a screen-level bottom bar. A screen that hides the tab bar gets no bottom chrome.
 */
@Composable
internal fun EntryFrame(
    key: AppNavKey,
    motion: ScreenMotion,
    hidesBar: Boolean,
    screenBars: ScreenBars?,
    content: @Composable () -> Unit,
) {
    val arrived = LocalNavAnimatedContentScope.current.transition.arrival(motion)
    val colors = AppTheme.colors
    val ownBar = remember { ChromeInset() }
    if (screenBars != null) {
        DisposableEffect(screenBars, key, ownBar) {
            screenBars.heights[key] = ownBar
            onDispose { if (screenBars.heights[key] === ownBar) screenBars.heights.remove(key) }
        }
    }
    val frame = Modifier
        .fillMaxSize()
        .coverScrim(arrived, maxAlpha = coverScrimAlpha(colors.isLight), shadow = EdgeShadow)
        .background(colors.background)
    if (hidesBar) {
        CompositionLocalProvider(
            LocalScreenBottomBar provides ownBar,
            LocalTabBarVisible provides false,
            LocalBottomChrome provides NoChrome,
        ) { Box(frame) { content() } }
    } else {
        CompositionLocalProvider(LocalScreenBottomBar provides ownBar) { Box(frame) { content() } }
    }
}

/** Never written: the bottom chrome of a screen that has no tab bar under it. */
private val NoChrome = ChromeInset()

private val EdgeShadow = 8.dp
