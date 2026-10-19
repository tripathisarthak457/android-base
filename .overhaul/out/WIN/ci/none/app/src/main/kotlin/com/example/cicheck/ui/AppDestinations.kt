package com.example.cicheck.ui

import com.example.cicheck.R
import com.example.cicheck.core.common.navigation.AppNavKey
import com.example.cicheck.core.designsystem.icon.AppIcons
import com.example.cicheck.core.navigation.ShellTab
import com.example.cicheck.feature.orders.OrdersListKey
import com.example.cicheck.feature.orders.R as OrdersR

/** The app's shape, in one file. */
object AppDestinations {

    /** The bottom-bar tabs, or an empty list for a single-stack app. */
    val tabs: List<ShellTab> = listOf(
        ShellTab(key = OrdersListKey, label = OrdersR.string.orders_title, icon = AppIcons.Grid),
        // Settings sits last because that is where people look for it.
    )

    /** Where a cold launch lands once onboarding and sign-in are out of the way. */
    val start: AppNavKey = tabs.firstOrNull()?.key
        ?: OrdersListKey

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
