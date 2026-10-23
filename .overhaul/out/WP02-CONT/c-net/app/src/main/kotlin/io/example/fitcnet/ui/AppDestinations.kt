package io.example.fitcnet.ui

import io.example.fitcnet.R
import io.example.fitcnet.core.common.navigation.AppNavKey
import io.example.fitcnet.core.designsystem.icon.AppIcons
import io.example.fitcnet.core.navigation.ShellTab
import io.example.fitcnet.feature.abc.R as AbcR
import io.example.fitcnet.feature.abc.api.AbcListKey
import io.example.fitcnet.feature.abcdefghi.R as AbcdefghiR
import io.example.fitcnet.feature.abcdefghi.api.AbcdefghiListKey
import io.example.fitcnet.feature.abcdefghijklmno.R as AbcdefghijklmnoR
import io.example.fitcnet.feature.abcdefghijklmno.api.AbcdefghijklmnoListKey
import io.example.fitcnet.feature.abcdefghijklmnopqrs.R as AbcdefghijklmnopqrsR
import io.example.fitcnet.feature.abcdefghijklmnopqrs.api.AbcdefghijklmnopqrsListKey
import io.example.fitcnet.feature.abcdefghijklmnopqrstuvw.R as AbcdefghijklmnopqrstuvwR
import io.example.fitcnet.feature.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwListKey
import io.example.fitcnet.feature.abcdefghijklmnopqrstuvwxyzabcd.R as AbcdefghijklmnopqrstuvwxyzabcdR
import io.example.fitcnet.feature.abcdefghijklmnopqrstuvwxyzabcd.api.AbcdefghijklmnopqrstuvwxyzabcdListKey

/** The app's shape, in one file. */
object AppDestinations {

    /**
     * The tabs, most visited first: a phone's bar shows five, or four and a More tab holding the
     * rest, so the order decides what stays one tap away.
     */
    val tabs: List<ShellTab> = listOf(
        ShellTab(key = AbcListKey, label = AbcR.string.abc_title, icon = AppIcons.Grid, selectedIcon = AppIcons.GridFilled),
        ShellTab(key = AbcdefghiListKey, label = AbcdefghiR.string.abcdefghi_title, icon = AppIcons.Tag, selectedIcon = AppIcons.TagFilled),
        ShellTab(key = AbcdefghijklmnoListKey, label = AbcdefghijklmnoR.string.abcdefghijklmno_title, icon = AppIcons.Bookmark, selectedIcon = AppIcons.BookmarkFilled),
        ShellTab(key = AbcdefghijklmnopqrsListKey, label = AbcdefghijklmnopqrsR.string.abcdefghijklmnopqrs_title, icon = AppIcons.Star, selectedIcon = AppIcons.StarFilled),
        ShellTab(key = AbcdefghijklmnopqrstuvwListKey, label = AbcdefghijklmnopqrstuvwR.string.abcdefghijklmnopqrstuvw_title, icon = AppIcons.Folder, selectedIcon = AppIcons.FolderFilled),
        ShellTab(key = AbcdefghijklmnopqrstuvwxyzabcdListKey, label = AbcdefghijklmnopqrstuvwxyzabcdR.string.abcdefghijklmnopqrstuvwxyzabcd_title, icon = AppIcons.Globe, selectedIcon = AppIcons.GlobeFilled),
    )

    /** Where a cold launch lands once onboarding and sign-in are out of the way. */
    val start: AppNavKey = tabs.firstOrNull()?.key
        ?: AbcListKey

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
