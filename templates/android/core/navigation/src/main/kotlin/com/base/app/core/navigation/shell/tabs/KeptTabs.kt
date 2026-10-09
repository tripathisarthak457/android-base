package com.base.app.core.navigation.shell.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.base.app.core.designsystem.foundation.LocalTabVisible

/**
 * Every tab opened in this session stays composed, and a switch shows another one instead of
 * building it again: rebuilding composed a whole screen on the frame of the tap. The switch is
 * instant, as on iOS; the bar's indicator is the motion.
 *
 * A hidden tab is neither measured nor placed, so it costs nothing in layout or drawing, and its
 * lifecycle is held at CREATED: flows collected with the lifecycle pause, and a screen that acts on
 * being seen does not act while out of sight. [LocalTabVisible] is false inside it, so shimmer and
 * spinners stop their clocks.
 *
 * A hidden tab also stops answering Back. Navigation 3 registers each stack's back handler for as
 * long as the stack is composed, and the most recently registered enabled handler wins: a tab left a
 * few screens deep would take the swipe meant for the visible one.
 *
 * A new [generation] (sign-out, another account) starts with nothing kept.
 */
@Composable
internal fun KeptTabs(
    selected: Int,
    generation: Int,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int) -> Unit,
) {
    // Grows only when [selected] changes, which recomposes this anyway; written after composition so
    // composition itself stays free of side effects.
    val opened = remember(generation) { mutableStateListOf<Int>() }
    if (selected !in opened) SideEffect { opened += selected }

    Box(modifier) {
        (opened + selected).distinct().forEach { index ->
            key(generation, index) {
                val active = index == selected
                CompositionLocalProvider(
                    LocalLifecycleOwner provides rememberTabLifecycle(active),
                    *rememberTabBackDispatcher(active),
                ) {
                    Box(
                        Modifier.layout { measurable, constraints ->
                            if (!active) return@layout layout(constraints.maxWidth, constraints.maxHeight) {}
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        },
                    ) {
                        CompositionLocalProvider(LocalTabVisible provides active) { content(index) }
                    }
                }
            }
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
    SideEffect {
        owner.active = active
        owner.follow(parent.lifecycle.currentState)
    }
    return owner
}

/** Its parent's lifecycle, but never past CREATED while [active] is false. */
internal class CappedLifecycle : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    var active = false

    fun follow(parent: Lifecycle.State) {
        if (registry.currentState == Lifecycle.State.DESTROYED) return
        val cap = if (active) Lifecycle.State.RESUMED else Lifecycle.State.CREATED
        val target = minOf(parent, cap)
        // A lifecycle cannot be destroyed before it was created; the parent reports DESTROYED only
        // on its way out, and end() takes care of that.
        if (target != Lifecycle.State.DESTROYED) registry.currentState = target
    }

    fun end() {
        if (registry.currentState.isAtLeast(Lifecycle.State.CREATED)) registry.currentState = Lifecycle.State.DESTROYED
    }
}
