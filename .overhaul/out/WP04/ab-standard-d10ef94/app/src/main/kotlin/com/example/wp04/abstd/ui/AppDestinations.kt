package com.example.wp04.abstd.ui

import com.example.wp04.abstd.R
import com.example.wp04.abstd.core.common.navigation.AppNavKey
import com.example.wp04.abstd.core.designsystem.icon.AppIcons
import com.example.wp04.abstd.core.navigation.ShellTab
import com.example.wp04.abstd.feature.sample.api.SampleListKey
import com.example.wp04.abstd.feature.profile.api.ProfileKey
import com.example.wp04.abstd.feature.settings.api.SettingsKey
import com.example.wp04.abstd.feature.auth.api.SignInKey
import com.example.wp04.abstd.feature.onboarding.api.OnboardingKey

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

    /** Where a first launch starts. */
    val onboarding: AppNavKey = resolveOnboarding()

    private fun resolveSignIn(): AppNavKey {
        return SignInKey
    }

    private fun resolveOnboarding(): AppNavKey {
        return OnboardingKey
    }
}
