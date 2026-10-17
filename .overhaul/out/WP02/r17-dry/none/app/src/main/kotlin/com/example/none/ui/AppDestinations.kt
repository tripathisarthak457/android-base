package com.example.none.ui

import com.example.none.R
import com.example.none.core.common.navigation.AppNavKey
import com.example.none.core.designsystem.icon.AppIcons
import com.example.none.core.navigation.ShellTab
import com.example.none.feature.home.R as HomeR
import com.example.none.feature.home.api.HomeListKey

/** The app's shape, in one file. */
object AppDestinations {

    /**
     * The tabs, most visited first: a phone's bar shows five, or four and a More tab holding the
     * rest, so the order decides what stays one tap away.
     */
    val tabs: List<ShellTab> = listOf(
        ShellTab(key = HomeListKey, label = HomeR.string.home_title, icon = AppIcons.Grid, selectedIcon = AppIcons.Grid),
    )

    /** Where a cold launch lands once onboarding and sign-in are out of the way. */
    val start: AppNavKey = tabs.firstOrNull()?.key
        ?: HomeListKey

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
