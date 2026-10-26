package com.example.cmp.ui.start

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.cmp.core.designsystem.theme.AppTheme

/**
 * The screen the app opens on: the window in the theme's background and nothing else, until the
 * project's first screen takes its place. Tagged "start", which the startup benchmark waits for.
 */
@Composable
internal fun StartScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .testTag("start"),
    )
}
