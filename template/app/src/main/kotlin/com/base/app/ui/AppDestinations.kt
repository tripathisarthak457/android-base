package com.base.app.ui

import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.navigation.AppNavKey
import com.base.app.core.navigation.ShellTab
// <generated:start-destination-import>
// <opt:sample>
import com.base.app.feature.sample.SampleListKey
// </opt:sample>
// <opt:paging>
import com.base.app.feature.feed.FeedKey
// </opt:paging>
// <opt:search>
import com.base.app.feature.search.SearchKey
// </opt:search>
// <opt:profile>
import com.base.app.feature.profile.ProfileKey
// </opt:profile>
// <opt:settings>
import com.base.app.feature.settings.SettingsKey
// </opt:settings>
// <opt:auth>
import com.base.app.feature.auth.SignInKey
// </opt:auth>

/** The app's shape, in one file. */
object AppDestinations {

    /** The bottom-bar tabs, or an empty list for a single-stack app. */
    val tabs: List<ShellTab> = listOf(
        // <opt:sample>
        ShellTab(key = SampleListKey, label = "Home", icon = AppIcons.Home),
        // </opt:sample>
        // <opt:paging>
        ShellTab(key = FeedKey, label = "Feed", icon = AppIcons.ListView),
        // </opt:paging>
        // <opt:search>
        ShellTab(key = SearchKey, label = "Search", icon = AppIcons.Search),
        // </opt:search>
        // <generated:shell-tabs>
        // <opt:profile>
        ShellTab(key = ProfileKey, label = "Profile", icon = AppIcons.User),
        // </opt:profile>
        // Settings sits last because that is where people look for it.
        // <opt:settings>
        ShellTab(key = SettingsKey, label = "Settings", icon = AppIcons.Settings),
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
