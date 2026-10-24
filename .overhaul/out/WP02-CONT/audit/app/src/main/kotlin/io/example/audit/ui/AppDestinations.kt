package io.example.audit.ui

import io.example.audit.R
import io.example.audit.core.common.navigation.AppNavKey
import io.example.audit.core.designsystem.icon.AppIcons
import io.example.audit.core.navigation.ShellTab
import io.example.audit.feature.abcdefghijklmnopqrstuvwxyzabcd.R as AbcdefghijklmnopqrstuvwxyzabcdR
import io.example.audit.feature.abcdefghijklmnopqrstuvwxyzabcd.api.AbcdefghijklmnopqrstuvwxyzabcdListKey
import io.example.audit.feature.order.R as OrderR
import io.example.audit.feature.order.api.OrderListKey
import io.example.audit.feature.sample.api.SampleListKey

/** The app's shape, in one file. */
object AppDestinations {

    /**
     * The tabs, most visited first: a phone's bar shows five, or four and a More tab holding the
     * rest, so the order decides what stays one tap away.
     */
    val tabs: List<ShellTab> = listOf(
        ShellTab(key = SampleListKey, label = R.string.tab_home, icon = AppIcons.Home),
        ShellTab(key = OrderListKey, label = OrderR.string.order_title, icon = AppIcons.Package, selectedIcon = AppIcons.PackageFilled),
        ShellTab(key = AbcdefghijklmnopqrstuvwxyzabcdListKey, label = AbcdefghijklmnopqrstuvwxyzabcdR.string.abcdefghijklmnopqrstuvwxyzabcd_title, icon = AppIcons.Grid, selectedIcon = AppIcons.GridFilled),
    )

    /** Where a cold launch lands once onboarding and sign-in are out of the way. */
    val start: AppNavKey = tabs.firstOrNull()?.key
        ?: SampleListKey

    /** Where an unauthenticated launch lands, and where sign-out returns to. */
    val signIn: AppNavKey = resolveSignIn()

    /** Where a first launch starts. */
    val onboarding: AppNavKey = resolveOnboarding()

    private fun resolveSignIn(): AppNavKey {
        return start
    }

    private fun resolveOnboarding(): AppNavKey {
        return start
    }
}
