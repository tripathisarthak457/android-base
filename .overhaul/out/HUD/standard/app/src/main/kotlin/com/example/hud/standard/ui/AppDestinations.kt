package com.example.hud.standard.ui

import com.example.hud.standard.R
import com.example.hud.standard.core.common.navigation.AppNavKey
import com.example.hud.standard.core.designsystem.icon.AppIcons
import com.example.hud.standard.core.navigation.ShellTab
import com.example.hud.standard.feature.sample.api.SampleListKey
import com.example.hud.standard.feature.profile.api.ProfileKey
import com.example.hud.standard.feature.settings.api.SettingsKey

/** The app's shape, in one file. */
object AppDestinations {

    /** The bottom-bar tabs, or an empty list for a single-stack app. */
    val tabs: List<ShellTab> = listOf(
        ShellTab(key = SampleListKey, label = R.string.tab_home, icon = AppIcons.Home),
        ShellTab(key = ProfileKey, label = R.string.tab_profile, icon = AppIcons.User),
        // Settings sits last because that is where people look for it.
        ShellTab(key = SettingsKey, label = R.string.tab_settings, icon = AppIcons.Settings),
    )

    /** Where a cold launch lands once onboarding and sign-in are out of the way. */
    val start: AppNavKey = tabs.firstOrNull()?.key
        ?: SampleListKey

    /** Where an unauthenticated launch lands, and where sign-out returns to. */
    val signIn: AppNavKey = resolveSignIn()

    /**
     * Where onboarding hands off. The same place: onboarding runs before sign-in, and cannot name
     * the next screen itself without depending on whichever feature owns it.
     */
    val afterOnboarding: AppNavKey get() = signIn

    private fun resolveSignIn(): AppNavKey {
        return start
    }
}
