package com.example.wp04.base.ui

import com.example.wp04.base.R
import com.example.wp04.base.core.designsystem.icon.AppIcons
import com.example.wp04.base.core.navigation.AppNavKey
import com.example.wp04.base.core.navigation.ShellTab
import com.example.wp04.base.feature.orders.OrdersListKey
import com.example.wp04.base.feature.orders.R as OrdersR
import com.example.wp04.base.feature.wallet.WalletListKey
import com.example.wp04.base.feature.wallet.R as WalletR
import com.example.wp04.base.feature.sample.SampleListKey
import com.example.wp04.base.feature.feed.FeedKey
import com.example.wp04.base.feature.search.SearchKey
import com.example.wp04.base.feature.profile.ProfileKey
import com.example.wp04.base.feature.settings.SettingsKey
import com.example.wp04.base.feature.auth.SignInKey

/** The app's shape, in one file. */
object AppDestinations {
    /** The bottom-bar tabs, or an empty list for a single-stack app. */
    val tabs: List<ShellTab> = listOf(
        ShellTab(key = SampleListKey, label = R.string.tab_home, icon = AppIcons.Home),
        ShellTab(key = FeedKey, label = R.string.tab_feed, icon = AppIcons.ListView),
        ShellTab(key = SearchKey, label = R.string.tab_search, icon = AppIcons.Search),
        ShellTab(key = OrdersListKey, label = OrdersR.string.orders_title, icon = AppIcons.Grid),
        ShellTab(key = WalletListKey, label = WalletR.string.wallet_title, icon = AppIcons.Grid),
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
