package com.example.wp04.dlplay.core.navigation.shell.tabs

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.example.wp04.dlplay.core.designsystem.foundation.layout.LocalTabVisible
import com.example.wp04.dlplay.core.designsystem.foundation.layout.TabVisibility
import com.example.wp04.dlplay.core.designsystem.theme.rememberReduceMotion

/**
 * Every tab opened in this session stays composed, and a switch shows another one instead of
 * building it again: rebuilding composed a whole screen on the frame of the tap.
 *
 * A hidden tab is parked: still measured and placed, so its layers and display lists survive, but
 * drawn at alpha 0 off the window, out of the semantics tree and out of reach of touch. Showing it
 * again changes two layer properties; nothing is measured or recorded on the switch frame.
 * Skipping its layout instead would drop every layer it had, and the switch would pay to rebuild
 * them. Its lifecycle is held at CREATED, so flows collected with the lifecycle pause and a screen
 * that acts on being seen does not act out of sight; [LocalTabVisible] tells clocks to stop.
 *
 * A hidden tab also stops answering Back: its dispatcher is disabled, or a tab left a few screens
 * deep would take the swipe meant for the visible one.
 *
 * The tabs in [warmUp] that nobody opened yet are built while the app is idle ([TabWarmUp]), so
 * the first visit to one costs what a switch to a kept tab costs.
 *
 * The tab coming to the front on a tap is nudged toward the direction of travel ([TabReveal]);
 * the one leaving disappears at once, so two screens are never drawn together.
 *
 * A new [generation] (sign-out, another account) starts with nothing kept.
 *
 * @param selected the tab in front, read where each tab needs it: the tabs that change with a switch
 *   recompose in the frame of the tap, as the bar does, rather than in the frame after it.
 * @param revealSwitch whether the switch to [selected] is one the user made, which gets the reveal;
 *   a deep link or a programmatic switch shows the tab at once.
 * @param positionOf where a tab sits along the chrome, which decides the side its reveal comes from.
 */
@Composable
internal fun KeptTabs(
    selected: () -> Int,
    generation: Int,
    modifier: Modifier = Modifier,
    revealSwitch: () -> Boolean = { false },
    positionOf: (index: Int) -> Int = { it },
    warmUp: List<Int> = emptyList(),
    content: @Composable (index: Int) -> Unit,
) {
    key(generation) {
        // Grows when [selected] changes or a warmed tab is ready; read in measure, so a change lays
        // the tabs out again without composing anything.
        val front = selected()
        val opened = remember { mutableStateListOf<Int>() }
        if (front !in opened) SideEffect { opened += front }

        val selection = rememberUpdatedState(selected)
        val reveal = remember { TabReveal(front) }
        val reduceMotion = rememberReduceMotion()
        // Applied with the composition that switches, before that frame is laid out and drawn, so
        // the first frame of the incoming tab is already nudged.
        SideEffect { reveal.switchTo(front, animate = revealSwitch() && !reduceMotion, position = positionOf) }
        LaunchedEffect(reveal, front) { reveal.run(front) }

        val latest = rememberUpdatedState(content)
        val slots = remember { SubcomposeLayoutState() }
        val warming = remember { TabWarmUp(slots) }
        // One content lambda per tab: a measure pass that subcomposes a tab again with the same
        // lambda leaves its composition alone, and a warmed tab is taken over as it was built.
        val tabs = remember { HashMap<Int, @Composable () -> Unit>() }
        val tab = { index: Int ->
            tabs.getOrPut(index) { { KeptTab(index, selection, reveal, warming) { latest.value(index) } } }
        }
        WarmUpTabs(warming, warmUp, opened, tab)

        SubcomposeLayout(slots, modifier) { constraints ->
            val shown = (opened + selection.value()).distinct()
            val placeables = shown.associateWith { index ->
                subcompose(index, tab(index)).map { it.measure(constraints) }
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                // A tab drawn once under the one in front goes first, so the screen in front covers it.
                shown.sortedBy { it != warming.underneath }.forEach { index ->
                    placeables.getValue(index).forEach { it.place(0, 0) }
                }
            }
        }
    }
}

/** One kept tab: its lifecycle, Back dispatcher and visibility follow whether it is in front. */
@Composable
private fun KeptTab(
    index: Int,
    selection: State<() -> Int>,
    reveal: TabReveal,
    warming: TabWarmUp,
    content: @Composable () -> Unit,
) {
    // Derived, so a switch recomposes only the two tabs it changes.
    val active by remember { derivedStateOf { selection.value() == index } }
    val visibility = remember { TabVisibility { selection.value() == index } }
    CompositionLocalProvider(
        LocalLifecycleOwner provides rememberTabLifecycle(active),
        LocalTabVisible provides visibility,
        *rememberTabBackDispatcher(active),
    ) {
        Box(
            Modifier
                .keptTab(parked = !active, reveal = reveal, warming = warming, index = index)
                .focusProperties { onEnter = { if (selection.value() != index) cancelFocusChange() } }
                .focusGroup(),
        ) {
            content()
        }
    }
}

/** One tab's own back dispatcher, switched off while the tab is hidden. */
@Composable
private fun rememberTabBackDispatcher(active: Boolean): Array<ProvidedValue<*>> {
    // Missing only where nothing can send Back at all, such as a preview.
    val parent = LocalNavigationEventDispatcherOwner.current ?: return emptyArray()
    val owner = remember(parent) { TabBackDispatcher(parent.navigationEventDispatcher) }
    // Applied with the composition that switches tabs rather than a frame later, so there is no
    // moment in which both tabs, or neither, answer Back.
    SideEffect { owner.navigationEventDispatcher.isEnabled = active }
    DisposableEffect(owner) {
        onDispose { owner.navigationEventDispatcher.dispose() }
    }
    return arrayOf(LocalNavigationEventDispatcherOwner provides owner)
}

private class TabBackDispatcher(parent: NavigationEventDispatcher) : NavigationEventDispatcherOwner {
    override val navigationEventDispatcher = NavigationEventDispatcher(parent)
}

/**
 * A tab's lifecycle: lowered to CREATED the frame after it is hidden, and raised two frames after
 * it comes to the front, once the switch frame is on screen. Changed on the switch frame itself,
 * every collector in both tabs would stop or restart inside the frame that has to show the switch,
 * which starts with the CPU at its idle clock and can least afford it.
 */
@Composable
internal fun rememberTabLifecycle(active: Boolean): LifecycleOwner {
    val parent = LocalLifecycleOwner.current
    val owner = remember(parent) { CappedLifecycle() }
    DisposableEffect(parent, owner) {
        val observer = LifecycleEventObserver { source, _ -> owner.follow(source.lifecycle.currentState) }
        parent.lifecycle.addObserver(observer)
        onDispose {
            parent.lifecycle.removeObserver(observer)
            owner.end()
        }
    }
    if (active) {
        LaunchedEffect(owner) {
            withFrameNanos {}
            withFrameNanos {}
            owner.cap = Lifecycle.State.RESUMED
            owner.follow(parent.lifecycle.currentState)
        }
    } else {
        LaunchedEffect(owner) {
            withFrameNanos {}
            owner.cap = Lifecycle.State.CREATED
            owner.follow(parent.lifecycle.currentState)
        }
    }
    return owner
}

/** Its parent's lifecycle, but never past [cap]. */
internal class CappedLifecycle : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    var cap = Lifecycle.State.CREATED

    fun follow(parent: Lifecycle.State) {
        if (registry.currentState == Lifecycle.State.DESTROYED) return
        val target = minOf(parent, cap)
        // A lifecycle cannot be destroyed before it was created; the parent reports DESTROYED only
        // on its way out, and end() takes care of that.
        if (target != Lifecycle.State.DESTROYED) registry.currentState = target
    }

    fun end() {
        if (registry.currentState.isAtLeast(Lifecycle.State.CREATED)) registry.currentState = Lifecycle.State.DESTROYED
    }
}
