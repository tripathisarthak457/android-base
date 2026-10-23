package com.example.standard.core.navigation.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.rememberNavigationEventState
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.example.standard.core.common.navigation.AppNavKey
import com.example.standard.core.designsystem.foundation.layout.ChromeInset
import com.example.standard.core.designsystem.foundation.layout.LocalBottomChrome
import com.example.standard.core.designsystem.foundation.layout.LocalPaintedBackground
import com.example.standard.core.designsystem.foundation.layout.LocalScreenChrome
import com.example.standard.core.designsystem.foundation.layout.LocalTabBarVisible
import com.example.standard.core.designsystem.theme.AppTheme
import com.example.standard.core.navigation.Destination
import com.example.standard.core.navigation.LocalNavigationSettled
import com.example.standard.core.navigation.NavRegistry
import com.example.standard.core.navigation.Presentation
import com.example.standard.core.navigation.ShellState
import com.example.standard.core.navigation.back.BackCardState
import com.example.standard.core.navigation.back.BackGate
import com.example.standard.core.navigation.back.GateSettle
import com.example.standard.core.navigation.back.StackBackHandler
import com.example.standard.core.navigation.back.StackChange
import com.example.standard.core.navigation.stack.ScreenChromes
import com.example.standard.core.navigation.stack.rememberScreenChrome
import com.example.standard.core.navigation.stack.toNavEntries
import com.example.standard.core.navigation.transition.NavTransitions
import com.example.standard.core.navigation.transition.ReportMotion
import com.example.standard.core.navigation.transition.ScreenMotion
import com.example.standard.core.navigation.transition.arrival

/**
 * Sheets and covers, above the tab bar (bottom to top: content, bar, presented, overlays, toasts). A key
 * registered with a [Presentation] lands here whoever navigates to it, so a feature never draws a
 * sheet under the bar by presenting it inside its own tab.
 *
 * Composed only while something is presented or leaving: with nothing up there is no host to
 * intercept a touch or cost a frame. Registered after the tab hosts, its Back handler is asked first
 * while anything is presented, and a back gesture shrinks a cover into the back card
 * ([PresentationCard]).
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
    val host = remember(presented, registry, sheetStrategy) {
        PresentationCard(presented) { key ->
            sheetStrategy == null || registry.presentationOf(key) == Presentation.Cover
        }
    }
    // A committed card fades out over the shell after its cover has gone. Derived, so a frame that
    // counts itself in or out recomposes the host only when that decides whether the host stays.
    val gone by remember(presented, host) { derivedStateOf { presented.size == 0 && live == 0 && !host.card.active } }
    if (gone) return
    SideEffect { host.motion = motion }

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
    val provider = remember(registry, motion, state, host) {
        { key: AppNavKey, contentKey: String ->
            if (key == PresentationBase) {
                NavEntry(key, contentKey) { Box(Modifier.fillMaxSize()) }
            } else {
                val destination = registry.destinationFor(key)
                NavEntry(key = key, contentKey = contentKey, metadata = destinationMetadata(destination)) { shownKey ->
                    PresentedFrame(
                        key = shownKey,
                        contentKey = contentKey,
                        motion = motion,
                        gate = gate,
                        card = host.card,
                        screens = state.presentedScreens,
                        onLive = { live += it },
                    ) {
                        destination.content(shownKey)
                    }
                }
            }
        }
    }
    val entries = rememberDecoratedNavEntries(
        entries = remember(shown, provider) { shown.toNavEntries(provider) },
        entryDecorators = decorators,
    )
    val reduceMotion = AppTheme.reduceMotion
    val strategies = remember(sheetStrategy) { listOfNotNull(sheetStrategy) }
    val sceneState = rememberSceneState(entries, strategies, emptyList(), null, onBack = { gate.onBack(pop) })
    // Bound to no handler, so NavDisplay never seeks a predictive pop of its own: the back card
    // takes the gesture (StackBackHandler below).
    val inert = rememberNavigationEventState(sceneState)
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val scrim = host.card.scrim.value
                if (scrim > 0f) drawRect(Color.Black, alpha = scrim)
            },
    ) {
        val settled = remember(gate) { { !gate.moving } }
        CompositionLocalProvider(LocalNavigationSettled provides settled) {
            NavDisplay(
                sceneState = sceneState,
                navigationEventState = inert,
                transitionSpec = if (reduceMotion) {
                    NavTransitions.noneSpec
                } else {
                    remember(motion) { NavTransitions.present(motion) }
                },
                popTransitionSpec = if (reduceMotion) {
                    NavTransitions.noneSpec
                } else {
                    remember(motion, gate) { NavTransitions.dismiss(motion) { gate.lastChange == StackChange.GesturePop } }
                },
                predictivePopTransitionSpec = NavTransitions.noneDuringPredictive,
            )
        }
    }
    StackBackHandler(presented, host, pop, cardEnabled = !reduceMotion)
    GateSettle(gate, pop)
}

/**
 * A presented screen: opaque, with no tab bar under it, reporting its motion to the gate, and the
 * back card while a gesture shrinks it.
 */
@Composable
private fun PresentedFrame(
    key: AppNavKey,
    contentKey: String,
    motion: ScreenMotion,
    gate: BackGate,
    card: BackCardState,
    screens: ScreenChromes,
    onLive: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    val transition = LocalNavAnimatedContentScope.current.transition
    ReportMotion(transition, transition.arrival(motion), gate)
    DisposableEffect(Unit) {
        onLive(1)
        onDispose { onLive(-1) }
    }
    val screen = rememberScreenChrome(screens, contentKey)
    val background = AppTheme.colors.background
    CompositionLocalProvider(
        LocalScreenChrome provides screen,
        LocalPaintedBackground provides background,
        LocalTabBarVisible provides false,
        LocalBottomChrome provides remember { ChromeInset() },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { if (card.active && card.subject == key) with(card) { applyCard() } }
                // The cover's own layer: under the back card, a texture the card's pose moves and fades.
                .graphicsLayer { compositingStrategy = if (card.textures(key)) CompositingStrategy.Offscreen else CompositingStrategy.Auto }
                .background(background),
        ) {
            content()
        }
    }
}

private fun destinationMetadata(destination: Destination): Map<String, Any> =
    destination.presentation?.let { mapOf(PRESENTATION_METADATA to it) }.orEmpty()

/** Read by the sheet strategy: which presentation, and with it the detents of a sheet. */
internal val NavEntry<*>.presentation: Presentation?
    get() = metadata[PRESENTATION_METADATA] as? Presentation

internal const val PRESENTATION_METADATA = "com.example.standard.navigation.presentation"

/** The transparent bottom of the presentation stack, so the last sheet or cover can animate away. */
internal data object PresentationBase : AppNavKey
