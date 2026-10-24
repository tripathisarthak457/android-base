package com.example.deskstandard.core.navigation.stack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.example.deskstandard.core.designsystem.foundation.layout.ChromeInset
import com.example.deskstandard.core.designsystem.foundation.layout.LocalBottomChrome
import com.example.deskstandard.core.designsystem.foundation.layout.LocalPaintedBackground
import com.example.deskstandard.core.designsystem.foundation.layout.LocalScreenChrome
import com.example.deskstandard.core.designsystem.foundation.layout.LocalTabBarVisible
import com.example.deskstandard.core.designsystem.theme.AppTheme

/**
 * What every screen is drawn in: an opaque background (two screens are drawn at once during a
 * slide, and one would show through the other), told to the screen so its scaffold does not paint
 * it a second time, and its own report to the shell ([ScreenChromes]). A screen that hides the tab
 * bar gets no bottom chrome.
 */
@Composable
internal fun EntryFrame(
    contentKey: String,
    hidesBar: Boolean,
    screens: ScreenChromes?,
    content: @Composable () -> Unit,
) {
    val screen = rememberScreenChrome(screens, contentKey)
    val background = AppTheme.colors.background
    val frame = Modifier.fillMaxSize().background(background)
    if (hidesBar) {
        CompositionLocalProvider(
            LocalScreenChrome provides screen,
            LocalPaintedBackground provides background,
            LocalTabBarVisible provides false,
            LocalBottomChrome provides NoChrome,
        ) { Box(frame) { content() } }
    } else {
        CompositionLocalProvider(
            LocalScreenChrome provides screen,
            LocalPaintedBackground provides background,
        ) { Box(frame) { content() } }
    }
}

/** Never written: the bottom chrome of a screen that has no tab bar under it. */
private val NoChrome = ChromeInset()
