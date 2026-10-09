package com.base.app.core.navigation.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.foundation.ChromeInset
import com.base.app.core.designsystem.foundation.LocalBottomChrome
import com.base.app.core.designsystem.foundation.LocalScreenBottomBar
import com.base.app.core.designsystem.foundation.LocalTabBarVisible
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.rememberReduceMotion
import com.base.app.core.navigation.Destination
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.Presentation
import com.base.app.core.navigation.ShellState
import com.base.app.core.navigation.back.BackGate
import com.base.app.core.navigation.back.GateEffects
import com.base.app.core.navigation.stack.ScreenBars
import com.base.app.core.navigation.transition.NavTransitions
import com.base.app.core.navigation.transition.ReportMotion
import com.base.app.core.navigation.transition.ScreenMotion
import com.base.app.core.navigation.transition.arrival

/**
 * Sheets and covers, above the tab bar (bottom to top: content, bar, presented, overlays, toasts). A key
 * registered with a [Presentation] lands here whoever navigates to it, so a feature never draws a
 * sheet under the bar by presenting it inside its own tab.
 *
 * Composed only while something is presented or leaving: with nothing up there is no host to
 * intercept a touch or cost a frame. Registered after the tab hosts, its Back handler is asked first
 * while anything is presented.
 *
 * A sheet is drawn by [sheetStrategy]; without one, a sheet key is presented as a cover.
 */
@Composable
internal fun PresentationHost(
    state: ShellState,
    registry: NavRegistry,
    motion: ScreenMotion,
    sheetStrategy: SceneStrategy<AppNavKey>?,
) {
    val presented = state.presented
    var live by remember { mutableIntStateOf(0) }
    if (presented.size == 0 && live == 0) return

    // The host starts with only its empty base, so the first presentation slides in rather than
    // appearing with the host.
    var shown by remember { mutableStateOf(listOf<AppNavKey>(PresentationBase)) }
    LaunchedEffect(presented) {
        snapshotFlow { presented.keys }.collect { shown = listOf(PresentationBase) + it }
    }

    val gate = presented.gate
    val pop = remember(presented) { { count: Int -> presented.pop(count) } }
    val saveable = rememberSaveableStateHolderNavEntryDecorator<AppNavKey>()
    val viewModels = rememberViewModelStoreNavEntryDecorator<AppNavKey>()
    val decorators = remember(saveable, viewModels) { listOf(saveable, viewModels) }
    val entries = rememberDecoratedNavEntries(
        backStack = shown,
        entryDecorators = decorators,
        entryProvider = remember(registry, motion, state) {
            { key ->
                if (key == PresentationBase) {
                    NavEntry(key) { Box(Modifier.fillMaxSize()) }
                } else {
                    val destination = registry.destinationFor(key)
                    NavEntry(key = key, metadata = destinationMetadata(destination)) { shownKey ->
                        PresentedFrame(shownKey, motion, gate, state.presentedBars, onLive = { live += it }) {
                            destination.content(shownKey)
                        }
                    }
                }
            }
        },
    )
    val reduceMotion = rememberReduceMotion()
    val height = LocalWindowInfo.current.containerSize.height
    val predictiveMillis = remember(motion, height) { motion.settleMillis(height.toFloat()) }
    val strategies = remember(sheetStrategy) { listOfNotNull(sheetStrategy) }
    NavDisplay(
        entries = entries,
        sceneStrategies = strategies,
        transitionSpec = if (reduceMotion) NavTransitions.noneSpec else remember(motion) { NavTransitions.present(motion) },
        popTransitionSpec = if (reduceMotion) NavTransitions.noneSpec else remember(motion) { NavTransitions.dismiss(motion) },
        predictivePopTransitionSpec = if (reduceMotion) {
            NavTransitions.noneDuringPredictive
        } else {
            remember(predictiveMillis) { NavTransitions.predictiveDismiss(predictiveMillis) }
        },
        onBack = { gate.onBack(pop) },
    )
    GateEffects(gate, pop)
}

/** A presented screen: opaque, with no tab bar under it, reporting its motion to the gate. */
@Composable
private fun PresentedFrame(
    key: AppNavKey,
    motion: ScreenMotion,
    gate: BackGate,
    screenBars: ScreenBars,
    onLive: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    val transition = LocalNavAnimatedContentScope.current.transition
    ReportMotion(transition, transition.arrival(motion), gate)
    DisposableEffect(Unit) {
        onLive(1)
        onDispose { onLive(-1) }
    }
    val ownBar = remember { ChromeInset() }
    DisposableEffect(screenBars, key, ownBar) {
        screenBars.heights[key] = ownBar
        onDispose { if (screenBars.heights[key] === ownBar) screenBars.heights.remove(key) }
    }
    CompositionLocalProvider(
        LocalScreenBottomBar provides ownBar,
        LocalTabBarVisible provides false,
        LocalBottomChrome provides remember { ChromeInset() },
    ) {
        Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) { content() }
    }
}

private fun destinationMetadata(destination: Destination): Map<String, Any> =
    destination.presentation?.let { mapOf(PRESENTATION_METADATA to it) }.orEmpty()

/** Read by the sheet strategy: which presentation, and with it the detents of a sheet. */
internal val NavEntry<*>.presentation: Presentation?
    get() = metadata[PRESENTATION_METADATA] as? Presentation

internal const val PRESENTATION_METADATA = "com.base.app.navigation.presentation"

/** The transparent bottom of the presentation stack, so the last sheet or cover can animate away. */
internal data object PresentationBase : AppNavKey
