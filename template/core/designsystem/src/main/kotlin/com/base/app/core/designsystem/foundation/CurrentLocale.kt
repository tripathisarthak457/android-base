package com.base.app.core.designsystem.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/**
 * The device's current locale, as something a composable can react to.
 *
 * `Locale.getDefault()` is a plain static read: Compose has no way to know it was consulted, so a
 * screen that formats a date or picks a first-day-of-week from it keeps the old answer when the
 * user changes their language and comes back. Reading `LocalConfiguration` makes the dependency
 * visible, and the affected composables recompose.
 *
 * Every locale-dependent component in the design system goes through here, so the mistake is
 * impossible to make in only one of them.
 */
@Composable
fun rememberCurrentLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) { configuration.locales[0] }
}
