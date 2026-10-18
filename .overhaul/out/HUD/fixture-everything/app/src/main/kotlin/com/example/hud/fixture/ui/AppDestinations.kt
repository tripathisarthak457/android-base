package com.example.hud.fixture.ui

import com.example.hud.fixture.R
import com.example.hud.fixture.core.common.navigation.AppNavKey
import com.example.hud.fixture.core.designsystem.icon.AppIcons
import com.example.hud.fixture.core.navigation.ShellTab
import com.example.hud.fixture.feature.sample.api.SampleListKey
import com.example.hud.fixture.feature.feed.api.FeedKey
import com.example.hud.fixture.feature.search.api.SearchKey
import com.example.hud.fixture.feature.profile.api.ProfileKey
import com.example.hud.fixture.feature.settings.api.SettingsKey
import com.example.hud.fixture.feature.auth.api.SignInKey

/** The app's shape, in one file. */
object AppDestinations {

    /** The bottom-bar tabs, or an empty list for a single-stack app. */
    val tabs: List<ShellTab> = listOf(
        ShellTab(key = SampleListKey, label = R.string.tab_home, icon = AppIcons.Home),
        ShellTab(key = FeedKey, label = R.string.tab_feed, icon = AppIcons.ListView),
        ShellTab(key = SearchKey, label = R.string.tab_search, icon = AppIcons.Search),
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
        return SignInKey
    }
}
