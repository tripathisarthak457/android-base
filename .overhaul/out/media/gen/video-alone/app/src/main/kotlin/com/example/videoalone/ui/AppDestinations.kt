package com.example.videoalone.ui

import com.example.videoalone.R
import com.example.videoalone.core.common.navigation.AppNavKey
import com.example.videoalone.core.designsystem.icon.AppIcons
import com.example.videoalone.core.navigation.ShellTab
import com.example.videoalone.feature.home.R as HomeR
import com.example.videoalone.feature.home.api.HomeListKey

/** The app's shape, in one file. */
object AppDestinations {

    /**
     * The tabs, most visited first: a phone's bar shows five, or four and a More tab holding the
     * rest, so the order decides what stays one tap away.
     */
    val tabs: List<ShellTab> = listOf(
        ShellTab(key = HomeListKey, label = HomeR.string.home_title, icon = AppIcons.Grid, selectedIcon = AppIcons.GridFilled),
    )

    /** Where a cold launch lands once onboarding and sign-in are out of the way. */
    val start: AppNavKey = tabs.firstOrNull()?.key
        ?: HomeListKey

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
