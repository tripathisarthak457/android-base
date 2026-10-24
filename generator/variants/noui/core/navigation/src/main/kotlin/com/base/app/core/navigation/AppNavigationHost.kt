package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.navigation.back.submit

/**
 * One back stack drawn by Navigation 3's own `NavDisplay`, with its default transitions and
 * predictive back. Each entry keeps its saved state and its ViewModels for as long as it is on the
 * stack. Commands from the navigator are applied while the activity is started.
 *
 * Every destination is drawn as a screen of its own: [NavPane] and [Presentation] stay on each
 * registration for the scene strategies a project adds once it has a design system (a list beside
 * its detail on a wide window, a sheet over the screen it opened from), passed to `NavDisplay`
 * here.
 */
@Composable
fun AppNavigationHost(
    backStack: AppBackStack,
    registry: NavRegistry,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(navigator, backStack, lifecycle) {
        navigator.applyWhileStarted(lifecycle) { command -> backStack.applyNow(command) }
    }
    val keys = backStack.entries.toList()
    val saveable = rememberSaveableStateHolderNavEntryDecorator<AppNavKey>()
    val viewModels = rememberViewModelStoreNavEntryDecorator<AppNavKey>()
    val entries = rememberDecoratedNavEntries(
        // The position makes a key that is on the stack twice two screens, each with its own state.
        entries = remember(keys, registry) {
            keys.mapIndexed { index, key ->
                NavEntry(key = key, contentKey = "$index:$key") { shown ->
                    registry.destinationFor(shown).content(shown)
                }
            }
        },
        entryDecorators = remember(saveable, viewModels) { listOf(saveable, viewModels) },
    )
    NavDisplay(
        entries = entries,
        modifier = modifier,
        onBack = { backStack.applyNow(NavCommand.Up) },
    )
}

/**
 * Applies [command] by the stack's rules, at once. `NavDisplay` animates the change itself and
 * tells the stack nothing, so the change is marked settled as soon as it is made; otherwise the
 * next command would wait for a transition nobody reports.
 */
internal fun AppBackStack.applyNow(command: NavCommand) {
    submit(command)
    gate.noMotion()
}
