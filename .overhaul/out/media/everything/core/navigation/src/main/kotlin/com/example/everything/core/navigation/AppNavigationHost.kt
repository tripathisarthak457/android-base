package com.example.everything.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.everything.core.navigation.back.Verdict
import com.example.everything.core.navigation.back.submit
import com.example.everything.core.navigation.stack.StackHost
import kotlinx.coroutines.flow.first

/**
 * The single-stack host: onboarding and the signed-out screens, which have no tab bar. Commands
 * from the navigator are applied while the activity is started, after any transition settles.
 *
 * @param receivesCommands false while this host is leaving ([HostCrossfade]), so commands wait for
 *   the host that replaces it.
 */
@Composable
fun AppNavigationHost(
    backStack: AppBackStack,
    registry: NavRegistry,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    config: ShellConfig = ShellConfig(),
    receivesCommands: Boolean = true,
) {
    if (receivesCommands) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(navigator, backStack, lifecycle) {
            navigator.applyWhileStarted(lifecycle) { command ->
                while (backStack.submit(command) == Verdict.Wait) {
                    snapshotFlow { backStack.gate.moving }.first { !it }
                }
            }
        }
    }
    val motion = remember(config.screenResponse) { config.screenMotion() }
    StackHost(stack = backStack, registry = registry, motion = motion, modifier = modifier)
}
