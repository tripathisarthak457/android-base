package com.example.cmp.ui

import com.example.cmp.R
import com.example.cmp.core.common.navigation.AppNavKey
import com.example.cmp.core.designsystem.icon.AppIcons
import com.example.cmp.core.navigation.shell.ShellTab
import com.example.cmp.feature.auth.api.SignInKey
import com.example.cmp.feature.feed.api.FeedKey
import com.example.cmp.feature.onboarding.api.OnboardingKey
import com.example.cmp.feature.profile.api.ProfileKey
import com.example.cmp.feature.sample.api.SampleListKey
import com.example.cmp.feature.search.api.SearchKey
import com.example.cmp.feature.settings.api.SettingsKey

/** The app's shape, in one file. */
object AppDestinations {

    /**
     * The tabs, most visited first: a phone's bar shows five, or four and a More tab holding the
     * rest, so the order decides what stays one tap away.
     */
    val tabs: List<ShellTab> = listOf(
        ShellTab(key = SampleListKey, label = R.string.tab_home, icon = AppIcons.Home),
        ShellTab(key = FeedKey, label = R.string.tab_feed, icon = AppIcons.ListView),
        ShellTab(key = SearchKey, label = R.string.tab_search, icon = AppIcons.Search),
        ShellTab(key = ProfileKey, label = R.string.tab_profile, icon = AppIcons.User),
        ShellTab(key = SettingsKey, label = R.string.tab_settings, icon = AppIcons.Settings),
    )

    /** Where a cold launch lands once onboarding and sign-in are out of the way. */
    val start: AppNavKey = tabs.firstOrNull()?.key
        ?: SampleListKey

    /** Where an unauthenticated launch lands, and where sign-out returns to. */
    val signIn: AppNavKey = resolveSignIn()

    /** Where a first launch starts. */
    val onboarding: AppNavKey = resolveOnboarding()

    private fun resolveSignIn(): AppNavKey = SignInKey

    private fun resolveOnboarding(): AppNavKey = OnboardingKey
}
