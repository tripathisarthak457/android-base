package com.example.cmp.core.navigation.shell.back

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * Registers [handler] with the dispatcher in scope while this is composed, as `NavigationBackHandler`
 * does, and keeps it enabled while [enabled] holds.
 *
 * [enabled] is followed outside composition. A stack's handler turns off when the gate lets go at
 * the end of a pop to the root, a frame of its own in the middle of the slide; recomposing there
 * would be the only composition between the pop's first frame and its last.
 */
@Composable
internal fun BackHandlerEffect(handler: NavigationEventHandler<*>, enabled: () -> Boolean) {
    // Missing only where nothing can send Back at all, such as a preview.
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
    val isEnabled by rememberUpdatedState(enabled)
    DisposableEffect(dispatcher, handler) {
        handler.isBackEnabled = isEnabled()
        dispatcher.addHandler(handler)
        onDispose { handler.remove() }
    }
    LaunchedEffect(handler) {
        snapshotFlow { isEnabled() }.collect { handler.isBackEnabled = it }
    }
}
