package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.base.app.core.designsystem.theme.AppMotion
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.rememberReduceMotion

/**
 * The display, and nothing else.
 *
 * This is the *only* file in the project that names Navigation 3. Feature modules see
 * [AppNavKey], [AppNavigator] and [navGraph]; if the library is replaced, this file and
 * [NavTransitions] are the extent of the change.
 *
 * It deliberately does not collect [AppNavigator] commands — [AppNavigationHost] does that for a
 * single-stack app, and `AppShell` does it for a tabbed one, where the same command has to land
 * in whichever tab's stack is in front. Folding the collection in here would make the two
 * mutually exclusive.
 *
 * ## The transition belongs to the destination, not to the host
 *
 * Each entry carries its own spec as metadata, so a cart that should rise from the bottom and a
 * detail screen that should slide in from the side each get the right treatment without the host
 * knowing what either of them is. A host that decided centrally would need a `when` over every
 * key — the exact coupling this design removes.
 */
@Composable
fun AppNavHost(
    backStack: AppBackStack,
    registry: NavRegistry,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {
        if (backStack.canGoBack) backStack.apply(NavCommand.Up)
    },
) {
    val motion = AppTheme.motion
    val reduceMotion = rememberReduceMotion()

    NavDisplay(
        backStack = backStack.entries,
        modifier = modifier,
        onBack = onBack,
        transitionSpec = if (reduceMotion) NavTransitions.none() else NavTransitions.push(motion),
        popTransitionSpec = if (reduceMotion) NavTransitions.none() else NavTransitions.pop(motion),
        predictivePopTransitionSpec = { _ ->
            val spec = if (reduceMotion) NavTransitions.none() else NavTransitions.pop(motion)
            spec()
        },
        entryProvider = { key ->
            val destination = registry.destinationFor(key)
            NavEntry(
                key = key,
                metadata = metadataFor(destination.transition, motion, reduceMotion),
                content = { destination.content(it) },
            )
        },
    )
}

/**
 * The single-stack host: one back stack, fed by the navigator.
 *
 * What an app without tabs uses. `AppShell` replaces it when there are tabs, because a command
 * then has to be applied to the stack of whichever tab is showing.
 *
 * ## Back is the stack's decision
 *
 * It pops when there is something to pop and calls [onExitRequested] when there is not, rather
 * than letting the display empty the stack. A display with nothing to show crashes, and "the user
 * pressed back at the root" is an application decision — confirm, exit, move to a home tab — that
 * this module should not make.
 */
@Composable
fun AppNavigationHost(
    backStack: AppBackStack,
    registry: NavRegistry,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    onExitRequested: () -> Unit = {},
) {
    LaunchedEffect(navigator, backStack) {
        navigator.commands.collect { command -> backStack.apply(command) }
    }

    AppNavHost(
        backStack = backStack,
        registry = registry,
        modifier = modifier,
        onBack = {
            if (backStack.canGoBack) backStack.apply(NavCommand.Up) else onExitRequested()
        },
    )
}

/**
 * Turns this module's [NavTransitionStyle] into the metadata map Navigation 3 reads.
 *
 * `Push` produces no metadata at all: it is what the host's own defaults already do, and an
 * entry that overrides them with an identical spec is one more thing to keep in sync for no
 * behavioural difference.
 */
private fun metadataFor(
    style: NavTransitionStyle,
    motion: AppMotion,
    reduceMotion: Boolean,
): Map<String, Any> {
    if (style == NavTransitionStyle.Push && !reduceMotion) return emptyMap()

    val enter = NavTransitions.forStyle(style, motion, reduceMotion)
    val exit = NavTransitions.popForStyle(style, motion, reduceMotion)

    return NavDisplay.transitionSpec { enter() } +
        NavDisplay.popTransitionSpec { exit() } +
        NavDisplay.predictivePopTransitionSpec { _ -> exit() }
}
