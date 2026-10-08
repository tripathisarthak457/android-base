package com.base.app.core.designsystem.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/** The device's current locale, as something a composable can react to. */
@Composable
fun rememberCurrentLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) { configuration.locales[0] }
}
