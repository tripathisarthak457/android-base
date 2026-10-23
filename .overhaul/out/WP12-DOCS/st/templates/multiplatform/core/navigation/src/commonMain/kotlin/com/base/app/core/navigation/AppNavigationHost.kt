package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.base.app.core.navigation.interim.StackHost

/**
 * A single back stack with no tab bar, for flows outside the signed-in shell: onboarding and
 * signing in. [navigator]'s commands are applied to [backStack].
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
        navigator.applyWhileStarted(lifecycle) { command -> backStack.apply(command) }
    }
    StackHost(backStack = backStack, registry = registry, modifier = modifier)
}
