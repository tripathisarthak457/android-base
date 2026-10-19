package com.base.app.core.navigation.interim

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.theme.AppSprings
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.AppBackStack
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.NavTransitionStyle

/**
 * One navigation host: a `NavDisplay` over [backStack], each entry with its own saved state and
 * ViewModels, and an iOS push on the screen spring. Screens only slide: the covered one moves by
 * the parallax fraction and nothing fades, so no full-screen layer is ever drawn offscreen.
 *
 * It has none of the Android shell's kept tabs, Back coalescing during motion or presentation
 * layer, and holds no gesture during a push.
 */
@Composable
internal fun StackHost(
    backStack: AppBackStack,
    registry: NavRegistry,
    modifier: Modifier = Modifier,
) {
    val springs = AppTheme.springs
    val background = AppTheme.colors.background
    NavDisplay(
        backStack = backStack.entries,
        modifier = modifier,
        onBack = { backStack.pop(1) },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { push(springs) },
        popTransitionSpec = { pop(springs) },
        predictivePopTransitionSpec = { pop(springs) },
        entryProvider = { key: AppNavKey ->
            val destination = registry.destinationFor(key)
            NavEntry(key, metadata = destination.transition.metadata()) {
                // Opaque, so the screen beneath never shows through a translucent one mid-slide.
                Box(Modifier.fillMaxSize().background(background)) { destination.content(key) }
            }
        },
    )
}

private fun push(springs: AppSprings): ContentTransform {
    val parallax = springs.navigation.parallax
    return slideInHorizontally(springs.screen.offset) { it } togetherWith
        slideOutHorizontally(springs.screen.offset) { -(it * parallax).toInt() }
}

private fun pop(springs: AppSprings): ContentTransform {
    val parallax = springs.navigation.parallax
    return (
        slideInHorizontally(springs.screen.offset) { -(it * parallax).toInt() } togetherWith
            slideOutHorizontally(springs.screen.offset) { it }
        ).apply { targetContentZIndex = -1f }
}

/** A destination that replaces what was there rather than covering it appears without moving. */
private fun NavTransitionStyle.metadata(): Map<String, Any> = when (this) {
    NavTransitionStyle.Push -> emptyMap()
    NavTransitionStyle.None ->
        NavDisplay.transitionSpec { Instant } +
            NavDisplay.popTransitionSpec { Instant } +
            NavDisplay.predictivePopTransitionSpec { Instant }
}

private val Instant = ContentTransform(EnterTransition.None, ExitTransition.None)
