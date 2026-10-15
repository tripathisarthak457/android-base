package com.base.app.core.navigation.stack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.base.app.core.designsystem.foundation.layout.ChromeInset
import com.base.app.core.designsystem.foundation.layout.LocalBottomChrome
import com.base.app.core.designsystem.foundation.layout.LocalScreenChrome
import com.base.app.core.designsystem.foundation.layout.LocalTabBarVisible
import com.base.app.core.designsystem.theme.AppTheme

/**
 * What every screen is drawn in: an opaque background (two screens are drawn at once during a
 * slide, and one would show through the other), and its own report to the shell ([ScreenChromes]).
 * A screen that hides the tab bar gets no bottom chrome.
 */
@Composable
internal fun EntryFrame(
    contentKey: String,
    hidesBar: Boolean,
    screens: ScreenChromes?,
    content: @Composable () -> Unit,
) {
    val screen = rememberScreenChrome(screens, contentKey)
    val frame = Modifier.fillMaxSize().background(AppTheme.colors.background)
    if (hidesBar) {
        CompositionLocalProvider(
            LocalScreenChrome provides screen,
            LocalTabBarVisible provides false,
            LocalBottomChrome provides NoChrome,
        ) { Box(frame) { content() } }
    } else {
        CompositionLocalProvider(LocalScreenChrome provides screen) { Box(frame) { content() } }
    }
}

/** Never written: the bottom chrome of a screen that has no tab bar under it. */
private val NoChrome = ChromeInset()
