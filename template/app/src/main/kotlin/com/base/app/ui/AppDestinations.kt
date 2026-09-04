package com.base.app.ui

import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.navigation.AppNavKey
import com.base.app.core.navigation.ShellTab
// <generated:start-destination-import>
// <opt:sample>
import com.base.app.feature.sample.SampleListKey
// </opt:sample>
// <opt:settings>
import com.base.app.feature.settings.SettingsKey
// </opt:settings>
// <opt:auth>
import com.base.app.feature.auth.SignInKey
// </opt:auth>

/**
 * The app's shape, in one file.
 *
 * Every other file asks this object where things are rather than naming a destination itself,
 * which is why adding a tab or changing the entry screen is a one-file change and never a search
 * through the Activity, the DI graph and a deep-link table for three copies of the same key.
 */
object AppDestinations {

    /**
     * The bottom-bar tabs, or an empty list for a single-stack app.
     *
     * Each tab keeps its own back stack, so switching away and back returns the user where they
     * were rather than to the tab root — see `AppShell`.
     */
    val tabs: List<ShellTab> = listOf(
        // <opt:sample>
        ShellTab(key = SampleListKey, label = "Home", icon = AppIcons.Home),
        // </opt:sample>
        // <generated:shell-tabs>
        // Settings sits last because that is where people look for it.
        // <opt:settings>
        ShellTab(key = SettingsKey, label = "Settings", icon = AppIcons.Settings),
        // </opt:settings>
    )

    /**
     * Where a cold launch lands once onboarding and sign-in are out of the way.
     *
     * Derived from [tabs] when there are any: a start destination that is not the first tab puts
     * the user on a screen the bottom bar shows as unselected, and nothing about the UI explains
     * why. The fallback is for single-stack apps, which have no first tab to derive it from.
     */
    val start: AppNavKey = tabs.firstOrNull()?.key
        // <generated:start-destination>
        // <opt:sample>
        ?: SampleListKey
    // </opt:sample>

    /**
     * Where an unauthenticated launch lands, and where sign-out returns to.
     *
     * The app's own start destination in a project with no auth feature, so nothing above has to
     * ask whether there is one.
     */
    val signIn: AppNavKey = resolveSignIn()

    /**
     * Where onboarding hands off. The same place: onboarding runs before sign-in, and cannot name
     * the next screen itself without depending on whichever feature owns it.
     */
    val afterOnboarding: AppNavKey get() = signIn

    private fun resolveSignIn(): AppNavKey {
        // <opt:auth>
        return SignInKey
        // </opt:auth>
        // <opt:!auth>        return start
    }
}
