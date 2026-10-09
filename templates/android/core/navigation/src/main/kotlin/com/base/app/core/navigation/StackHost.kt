package com.base.app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.foundation.ChromeInset
import com.base.app.core.designsystem.foundation.LocalBottomChrome
import com.base.app.core.designsystem.foundation.LocalScreenBottomBar
import com.base.app.core.designsystem.foundation.LocalTabBarVisible
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * The single-stack host: onboarding and the signed-out screens, which have no tab bar. Commands
 * from the navigator are applied while the activity is started, after any transition settles.
 */
@Composable
fun AppNavigationHost(
    backStack: AppBackStack,
    registry: NavRegistry,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    config: ShellConfig = ShellConfig(),
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(navigator, backStack, lifecycle) {
        navigator.applyWhileStarted(lifecycle) { command ->
            while (backStack.submit(command) == Verdict.Wait) {
                snapshotFlow { backStack.gate.moving }.first { !it }
            }
        }
    }
    val motion = remember(config.screenResponse) { config.screenMotion() }
    StackHost(stack = backStack, registry = registry, motion = motion, modifier = modifier)
}

internal fun AppBackStack.submit(command: NavCommand): Verdict {
    val verdict = guard(command, this)
    if (verdict == Verdict.Apply) {
        apply(command)
        gate.changed(change(command))
    }
    return verdict
}

/**
 * One navigation host: a `NavDisplay` over [stack], with the entry beneath the top kept composed
 * (KeepUnderScene), slide-only transitions, and the [BackGate] in front of Back.
 */
@Composable
internal fun StackHost(
    stack: AppBackStack,
    registry: NavRegistry,
    motion: ScreenMotion,
    modifier: Modifier = Modifier,
    hidesBar: (AppNavKey) -> Boolean = NeverHidesBar,
    anchor: BarAnchor? = null,
    screenBars: ScreenBars? = null,
) {
    val gate = stack.gate
    val pop = remember(stack) { { count: Int -> stack.pop(count) } }
    val entries = rememberDecoratedNavEntries(
        backStack = stack.entries,
        entryDecorators = rememberEntryDecorators(),
        entryProvider = rememberEntryProvider(registry, motion, hidesBar, screenBars),
    )
    val twoPanes = AppTheme.windowSize.hasRoomForTwoPanes
    val listPaneWidth = AppTheme.layout.listPaneWidth
    val strategies = remember(twoPanes, listPaneWidth, motion, stack, anchor) {
        listOf(
            ListDetailSceneStrategy(twoPanes, listPaneWidth, motion, gate),
            KeepUnderSceneStrategy(motion, stack, anchor),
        )
    }
    val reduceMotion = rememberReduceMotion()
    val width = LocalWindowInfo.current.containerSize.width
    val predictiveMillis = remember(motion, width) { motion.settleMillis(width.toFloat()) }
    val pushSpec = remember(motion) { NavTransitions.push(motion) }
    val popSpec = remember(motion) { NavTransitions.pop(motion) }
    val predictiveSpec = remember(motion, predictiveMillis) { NavTransitions.predictivePop(motion, predictiveMillis) }

    NavDisplay(
        entries = entries,
        modifier = modifier,
        sceneStrategies = strategies,
        transitionSpec = if (reduceMotion) NavTransitions.noneSpec else pushSpec,
        popTransitionSpec = if (reduceMotion) NavTransitions.noneSpec else popSpec,
        predictivePopTransitionSpec = if (reduceMotion) NavTransitions.noneDuringPredictive else predictiveSpec,
        onBack = { gate.onBack(pop) },
    )
    GateEffects(gate, pop)
}

/**
 * Registered after the display, so it is asked first: while the screens move, Back is counted here
 * and a back swipe gets no half-seeked scene; its release counts as one press.
 */
@Composable
internal fun GateEffects(gate: BackGate, pop: (Int) -> Int) {
    val idle = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(state = idle, isBackEnabled = gate.moving, onBackCompleted = { gate.onBack(pop) })
    LaunchedEffect(gate) {
        snapshotFlow { gate.moving }.collect { moving -> if (!moving) gate.settle(pop) }
    }
    // A change that starts no transition (reduce motion, a destination with none) must not keep
    // Back counted forever.
    val changes = gate.changes
    LaunchedEffect(gate, changes) {
        delay(MOTION_START_WINDOW_MS)
        gate.noMotion()
    }
}

@Composable
private fun rememberEntryDecorators(): List<NavEntryDecorator<AppNavKey>> {
    val saveable = rememberSaveableStateHolderNavEntryDecorator<AppNavKey>()
    val viewModels = rememberViewModelStoreNavEntryDecorator<AppNavKey>()
    return remember(saveable, viewModels) { listOf(saveable, viewModels) }
}

@Composable
private fun rememberEntryProvider(
    registry: NavRegistry,
    motion: ScreenMotion,
    hidesBar: (AppNavKey) -> Boolean,
    screenBars: ScreenBars?,
): (AppNavKey) -> NavEntry<AppNavKey> = remember(registry, motion, hidesBar, screenBars) {
    { key ->
        val destination = registry.destinationFor(key)
        val hides = hidesBar(key)
        NavEntry(key = key, metadata = entryMetadata(key, destination, hides)) { shown ->
            EntryFrame(shown, motion, hides, screenBars) { destination.content(shown) }
        }
    }
}

private fun entryMetadata(key: AppNavKey, destination: Destination, hidesBar: Boolean): Map<String, Any> {
    val own = mapOf(KEY_METADATA to key, PANE_METADATA_KEY to destination.pane, HIDES_BAR_METADATA to hidesBar)
    if (destination.transition == NavTransitionStyle.Push) return own
    return own + NavDisplay.transitionSpec { NavTransitions.none } +
        NavDisplay.popTransitionSpec { NavTransitions.none } +
        NavDisplay.predictivePopTransitionSpec { NavTransitions.none }
}

/**
 * What every screen is drawn in: an opaque background (two screens composed at once would
 * otherwise show through each other), the scrim it draws over the screen it covers, and its own
 * holder for a screen-level bottom bar. A screen that hides the tab bar gets no bottom chrome.
 */
@Composable
private fun EntryFrame(
    key: AppNavKey,
    motion: ScreenMotion,
    hidesBar: Boolean,
    screenBars: ScreenBars?,
    content: @Composable () -> Unit,
) {
    val arrived = LocalNavAnimatedContentScope.current.transition.arrival(motion)
    val colors = AppTheme.colors
    val ownBar = remember { ChromeInset() }
    if (screenBars != null) {
        DisposableEffect(screenBars, key, ownBar) {
            screenBars.heights[key] = ownBar
            onDispose { if (screenBars.heights[key] === ownBar) screenBars.heights.remove(key) }
        }
    }
    val frame = Modifier
        .fillMaxSize()
        .coverScrim(arrived, maxAlpha = coverScrimAlpha(colors.isLight), shadow = EdgeShadow)
        .background(colors.background)
    if (hidesBar) {
        CompositionLocalProvider(
            LocalScreenBottomBar provides ownBar,
            LocalTabBarVisible provides false,
            LocalBottomChrome provides NoChrome,
        ) { Box(frame) { content() } }
    } else {
        CompositionLocalProvider(LocalScreenBottomBar provides ownBar) { Box(frame) { content() } }
    }
}

/** The screen-level bottom bars of one host's screens, for the shell to report the one in front. */
@Stable
internal class ScreenBars {
    val heights = mutableStateMapOf<AppNavKey, ChromeInset>()
}

internal const val KEY_METADATA = "com.base.app.navigation.key"
internal const val HIDES_BAR_METADATA = "com.base.app.navigation.hidesBar"

internal val NavEntry<*>.navKey: AppNavKey get() = metadata.getValue(KEY_METADATA) as AppNavKey

internal val NavEntry<*>.hidesBar: Boolean get() = metadata[HIDES_BAR_METADATA] == true

internal val NeverHidesBar: (AppNavKey) -> Boolean = { false }

/** Never written: the bottom chrome of a screen that has no tab bar under it. */
private val NoChrome = ChromeInset()

private val EdgeShadow = 8.dp

/** How long a stack change may take to start its transition before Back stops being counted. */
private const val MOTION_START_WINDOW_MS = 150L
