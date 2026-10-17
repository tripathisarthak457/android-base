package com.example.modulesmodular.ui

import com.example.modulesmodular.R
import com.example.modulesmodular.core.common.navigation.AppNavKey
import com.example.modulesmodular.core.designsystem.icon.AppIcons
import com.example.modulesmodular.core.navigation.ShellTab
import com.example.modulesmodular.feature.orders.R as OrdersR
import com.example.modulesmodular.feature.orders.api.OrdersListKey
import com.example.modulesmodular.feature.trips.R as TripsR
import com.example.modulesmodular.feature.trips.api.TripsListKey
import com.example.modulesmodular.feature.wallet.R as WalletR
import com.example.modulesmodular.feature.wallet.api.WalletListKey

/** The app's shape, in one file. */
object AppDestinations {

    /**
     * The tabs, most visited first: a phone's bar shows five, or four and a More tab holding the
     * rest, so the order decides what stays one tap away.
     */
    val tabs: List<ShellTab> = listOf(
        ShellTab(key = OrdersListKey, label = OrdersR.string.orders_title, icon = AppIcons.Package, selectedIcon = AppIcons.Package),
        ShellTab(key = TripsListKey, label = TripsR.string.trips_title, icon = AppIcons.MapPin, selectedIcon = AppIcons.MapPin),
        ShellTab(key = WalletListKey, label = WalletR.string.wallet_title, icon = AppIcons.CreditCard, selectedIcon = AppIcons.CreditCard),
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
