package io.example.fitcnet.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import io.example.fitcnet.core.designsystem.theme.AppTheme
import io.example.fitcnet.core.designsystem.theme.ThemeMode

/** Robolectric's SDK for these tests: it refuses the newest levels on the JDKs it supports. */
const val TestSdk = 35

/** [content] in the app theme with motion on, as on a device with default settings. */
fun ComposeContentTestRule.setThemed(content: @Composable () -> Unit) {
    setContent { AppTheme(mode = ThemeMode.Light, reduceMotion = false, content = content) }
}
