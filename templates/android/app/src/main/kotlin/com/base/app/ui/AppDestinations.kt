package com.base.app.ui

import com.base.app.R
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.navigation.shell.ShellTab
// <opt:auth>
import com.base.app.feature.auth.api.SignInKey
// </opt:auth>
// <opt:paging>
import com.base.app.feature.feed.api.FeedKey
// </opt:paging>
// <opt:onboarding>
import com.base.app.feature.onboarding.api.OnboardingKey
// </opt:onboarding>
// <opt:profile>
import com.base.app.feature.profile.api.ProfileKey
// </opt:profile>
// <opt:sample>
import com.base.app.feature.sample.api.SampleListKey
// </opt:sample>
// <opt:search>
import com.base.app.feature.search.api.SearchKey
// </opt:search>
// <opt:settings>
import com.base.app.feature.settings.api.SettingsKey
// </opt:settings>
// <generated:start-destination-import>

/** The app's shape, in one file. */
object AppDestinations {

    /**
     * The tabs, most visited first: a phone's bar shows five, or four and a More tab holding the
     * rest, so the order decides what stays one tap away.
     */
    val tabs: List<ShellTab> = listOf(
        // <opt:sample>
        ShellTab(key = SampleListKey, label = R.string.tab_home, icon = AppIcons.Home),
        // </opt:sample>
        // <generated:shell-tabs>
        // <opt:paging>
        ShellTab(key = FeedKey, label = R.string.tab_feed, icon = AppIcons.ListView),
        // </opt:paging>
        // <opt:search>
        ShellTab(key = SearchKey, label = R.string.tab_search, icon = AppIcons.Search),
        // </opt:search>
        // <opt:profile>
        ShellTab(key = ProfileKey, label = R.string.tab_profile, icon = AppIcons.User),
        // </opt:profile>
        // <opt:settings>
        ShellTab(key = SettingsKey, label = R.string.tab_settings, icon = AppIcons.Settings),
        // </opt:settings>
    )

    /** Where a cold launch lands once onboarding and sign-in are out of the way. */
    val start: AppNavKey = tabs.firstOrNull()?.key
        // <generated:start-destination>
        // <opt:sample>
        ?: SampleListKey
    // </opt:sample>

    /** Where an unauthenticated launch lands, and where sign-out returns to. */
    val signIn: AppNavKey = resolveSignIn()

    /** Where a first launch starts. */
    val onboarding: AppNavKey = resolveOnboarding()

    // <opt:auth>
    private fun resolveSignIn(): AppNavKey = SignInKey
    // </opt:auth>
    // <opt:!auth>    private fun resolveSignIn(): AppNavKey = start

    // <opt:onboarding>
    private fun resolveOnboarding(): AppNavKey = OnboardingKey
    // </opt:onboarding>
    // <opt:!onboarding>    private fun resolveOnboarding(): AppNavKey = start
}
