package com.base.app.ui

import com.base.app.R
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.navigation.ShellTab
// <generated:start-destination-import>
// <opt:sample>
import com.base.app.feature.sample.api.SampleListKey
// </opt:sample>
// <opt:paging>
import com.base.app.feature.feed.api.FeedKey
// </opt:paging>
// <opt:search>
import com.base.app.feature.search.api.SearchKey
// </opt:search>
// <opt:profile>
import com.base.app.feature.profile.api.ProfileKey
// </opt:profile>
// <opt:settings>
import com.base.app.feature.settings.api.SettingsKey
// </opt:settings>
// <opt:auth>
import com.base.app.feature.auth.api.SignInKey
// </opt:auth>
// <opt:onboarding>
import com.base.app.feature.onboarding.api.OnboardingKey
// </opt:onboarding>

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

    private fun resolveSignIn(): AppNavKey {
        // <opt:auth>
        return SignInKey
        // </opt:auth>
        // <opt:!auth>        return start
    }

    private fun resolveOnboarding(): AppNavKey {
        // <opt:onboarding>
        return OnboardingKey
        // </opt:onboarding>
        // <opt:!onboarding>        return start
    }
}
