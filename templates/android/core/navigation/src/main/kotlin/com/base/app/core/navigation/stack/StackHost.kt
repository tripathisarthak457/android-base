package com.base.app.core.navigation.stack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.foundation.ChromeInset
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.rememberReduceMotion
import com.base.app.core.navigation.AppBackStack
import com.base.app.core.navigation.Destination
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.NavTransitionStyle
import com.base.app.core.navigation.back.GateEffects
import com.base.app.core.navigation.scene.KeepUnderSceneStrategy
import com.base.app.core.navigation.scene.ListDetailSceneStrategy
import com.base.app.core.navigation.scene.PANE_METADATA_KEY
import com.base.app.core.navigation.shell.BarAnchor
import com.base.app.core.navigation.transition.NavTransitions
import com.base.app.core.navigation.transition.ScreenMotion

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
