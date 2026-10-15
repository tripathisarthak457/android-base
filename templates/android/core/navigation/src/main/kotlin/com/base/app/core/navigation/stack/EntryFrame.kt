package com.base.app.core.navigation.stack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.base.app.core.designsystem.foundation.layout.ChromeInset
import com.base.app.core.designsystem.foundation.layout.LocalBottomChrome
import com.base.app.core.designsystem.foundation.layout.LocalPaintedBackground
import com.base.app.core.designsystem.foundation.layout.LocalScreenBottomBar
import com.base.app.core.designsystem.foundation.layout.LocalTabBarVisible
import com.base.app.core.designsystem.theme.AppTheme

/**
 * What every screen is drawn in: an opaque background (two screens are drawn at once during a
 * slide, and one would show through the other), told to the screen so its scaffold does not paint
 * it a second time, and its own holder for a screen-level bottom bar. A screen that hides the tab
 * bar gets no bottom chrome.
 */
@Composable
internal fun EntryFrame(
    contentKey: String,
    hidesBar: Boolean,
    screenBars: ScreenBars?,
    content: @Composable () -> Unit,
) {
    val ownBar = remember { ChromeInset() }
    if (screenBars != null) {
        DisposableEffect(screenBars, contentKey, ownBar) {
            screenBars.heights[contentKey] = ownBar
            onDispose { if (screenBars.heights[contentKey] === ownBar) screenBars.heights.remove(contentKey) }
        }
    }
    val background = AppTheme.colors.background
    val frame = Modifier.fillMaxSize().background(background)
    if (hidesBar) {
        CompositionLocalProvider(
            LocalScreenBottomBar provides ownBar,
            LocalPaintedBackground provides background,
            LocalTabBarVisible provides false,
            LocalBottomChrome provides NoChrome,
        ) { Box(frame) { content() } }
    } else {
        CompositionLocalProvider(
            LocalScreenBottomBar provides ownBar,
            LocalPaintedBackground provides background,
        ) { Box(frame) { content() } }
    }
}

/** Never written: the bottom chrome of a screen that has no tab bar under it. */
private val NoChrome = ChromeInset()
