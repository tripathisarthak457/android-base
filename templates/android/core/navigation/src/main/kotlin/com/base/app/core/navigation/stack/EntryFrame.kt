package com.base.app.core.navigation.stack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.foundation.layout.ChromeInset
import com.base.app.core.designsystem.foundation.layout.LocalBottomChrome
import com.base.app.core.designsystem.foundation.layout.LocalScreenBottomBar
import com.base.app.core.designsystem.foundation.layout.LocalTabBarVisible
import com.base.app.core.designsystem.theme.AppTheme

/**
 * What every screen is drawn in: an opaque background (two screens are drawn at once during a
 * slide, and one would show through the other), and its own holder for a screen-level bottom bar.
 * A screen that hides the tab bar gets no bottom chrome.
 */
@Composable
internal fun EntryFrame(
    key: AppNavKey,
    hidesBar: Boolean,
    screenBars: ScreenBars?,
    content: @Composable () -> Unit,
) {
    val ownBar = remember { ChromeInset() }
    if (screenBars != null) {
        DisposableEffect(screenBars, key, ownBar) {
            screenBars.heights[key] = ownBar
            onDispose { if (screenBars.heights[key] === ownBar) screenBars.heights.remove(key) }
        }
    }
    val frame = Modifier.fillMaxSize().background(AppTheme.colors.background)
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
