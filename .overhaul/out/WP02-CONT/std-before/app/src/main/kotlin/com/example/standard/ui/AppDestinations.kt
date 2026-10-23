package com.example.standard.ui

import com.example.standard.R
import com.example.standard.core.common.navigation.AppNavKey
import com.example.standard.core.designsystem.icon.AppIcons
import com.example.standard.core.navigation.ShellTab
import com.example.standard.feature.auth.api.SignInKey
import com.example.standard.feature.onboarding.api.OnboardingKey
import com.example.standard.feature.sample.api.SampleListKey
import com.example.standard.feature.settings.api.SettingsKey

/** The app's shape, in one file. */
object AppDestinations {

    /**
     * The tabs, most visited first: a phone's bar shows five, or four and a More tab holding the
     * rest, so the order decides what stays one tap away.
     */
    val tabs: List<ShellTab> = listOf(
        ShellTab(key = SampleListKey, label = R.string.tab_home, icon = AppIcons.Home),
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
