package com.base.app.core.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.base.app.core.designsystem.theme.AppMotion
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.rememberReduceMotion

/**
 * The display, and nothing else. This is the *only* file in the project that names Navigation 3.
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
    val entries = rememberDecoratedNavEntries(
        backStack = backStack.entries,
        entryDecorators = rememberEntryDecorators(),
        entryProvider = rememberEntryProvider(registry),
    )
    StackDisplay(entries = entries, onBack = onBack, modifier = modifier)
}

/** The single-stack host: one back stack, fed by the navigator. */
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

/** Every tab's stack, with only the selected one on screen. */
@Composable
internal fun TabbedNavHost(
    state: ShellState,
    registry: NavRegistry,
    rootBottomInset: Dp,
    insetEveryEntry: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = AppTheme.motion
    val reduceMotion = rememberReduceMotion()
    // Read through state rather than captured: entries are built once per stack change, and the
    // inset changes without one — rotating the phone moves the navigation bar to the side.
    val inset by rememberUpdatedState(rootBottomInset)
    val insetAll by rememberUpdatedState(insetEveryEntry)

    val tabEntries = state.stacks.mapIndexed { index, stack ->
        key(index) {
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = rememberEntryDecorators(),
                entryProvider = rememberEntryProvider(registry) { key ->
                    if (insetAll || stack.firstOrNull() == key) inset else 0.dp
                },
            )
        }
    }

    AnimatedContent(
        targetState = state.selectedIndex,
        modifier = modifier,
        transitionSpec = if (reduceMotion) NavTransitions.none() else NavTransitions.tabSwitch(motion),
        label = "tabSwitch",
    ) { index ->
        StackDisplay(entries = tabEntries[index], onBack = onBack)
    }
}

@Composable
private fun StackDisplay(
    entries: List<NavEntry<AppNavKey>>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = AppTheme.motion
    val reduceMotion = rememberReduceMotion()

    NavDisplay(
        entries = entries,
        modifier = modifier,
        onBack = onBack,
        // The first strategy that claims the top of the stack draws it; everything else is one pane.
        sceneStrategies = listOf(rememberListDetailSceneStrategy(), SinglePaneSceneStrategy()),
        transitionSpec = if (reduceMotion) NavTransitions.none() else NavTransitions.push(motion),
        popTransitionSpec = if (reduceMotion) NavTransitions.none() else NavTransitions.pop(motion),
        predictivePopTransitionSpec = { _ ->
            val spec = if (reduceMotion) NavTransitions.none() else NavTransitions.pop(motion)
            spec()
        },
    )
}

@Composable
private fun rememberEntryDecorators(): List<NavEntryDecorator<AppNavKey>> = listOf(
    rememberSaveableStateHolderNavEntryDecorator(),
    rememberViewModelStoreNavEntryDecorator(),
)

@Composable
private fun rememberEntryProvider(
    registry: NavRegistry,
    bottomInsetFor: (AppNavKey) -> Dp = { 0.dp },
): (AppNavKey) -> NavEntry<AppNavKey> {
    val motion = AppTheme.motion
    val reduceMotion = rememberReduceMotion()

    return { key ->
        val destination = registry.destinationFor(key)
        NavEntry(
            key = key,
            metadata = metadataFor(destination.transition, motion, reduceMotion) +
                (PANE_METADATA_KEY to destination.pane),
            content = {
                val inset = bottomInsetFor(it)
                if (inset > 0.dp) {
                    // The background fills the reserved strip, so the space the bar leaves as it
                    // slides away is the screen's own colour rather than the window's.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppTheme.colors.background)
                            .padding(bottom = inset),
                    ) {
                        destination.content(it)
                    }
                } else {
                    destination.content(it)
                }
            },
        )
    }
}

/** Turns this module's [NavTransitionStyle] into the metadata map Navigation 3 reads. */
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
