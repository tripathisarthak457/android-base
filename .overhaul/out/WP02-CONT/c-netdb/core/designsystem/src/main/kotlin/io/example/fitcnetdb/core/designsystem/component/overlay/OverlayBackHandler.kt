package io.example.fitcnetdb.core.designsystem.component.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * Back for an overlay: an alert, a sheet, the loading overlay, the image viewer.
 *
 * It runs at overlay priority, ahead of every screen's handler and the navigator's, so Back closes
 * what is on top before anything pops. It joins the dispatcher of the composition that calls it,
 * which is the screen's even when the overlay draws in the window's top layer: an overlay left up
 * in a kept tab that is not on screen is disabled with that tab and never takes Back from the tab
 * the user is looking at.
 *
 * [onProgress] follows a predictive back gesture from 0 to 1 and [onCancel] ends one the user took
 * back; a Back key or button goes straight to [onBack]. An unhandled Escape on a hardware keyboard
 * arrives as Back too (the platform's key-character map falls Escape back to it). While the
 * window's overlays are suspended ([SuspendOverlays]) it takes no Back: the cover over them does.
 */
@Composable
internal fun OverlayBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit = {},
    onCancel: () -> Unit = {},
    onBack: () -> Unit,
) {
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
    // Read here, so the handler is switched when the cover comes or goes; that is rare.
    val suspended = LocalOverlayHost.current?.suspended == true
    val progress = rememberUpdatedState(onProgress)
    val cancel = rememberUpdatedState(onCancel)
    val back = rememberUpdatedState(onBack)
    val handler = remember {
        object : NavigationEventHandler<NavigationEventInfo>(NavigationEventInfo.None, isBackEnabled = enabled) {
            override fun onBackStarted(event: NavigationEvent) = progress.value(event.progress)
            override fun onBackProgressed(event: NavigationEvent) = progress.value(event.progress)
            override fun onBackCancelled() = cancel.value()
            override fun onBackCompleted() = back.value()
        }
    }
    SideEffect { handler.isBackEnabled = enabled && !suspended }
    DisposableEffect(dispatcher) {
        dispatcher.addHandler(handler, NavigationEventDispatcher.PRIORITY_OVERLAY)
        onDispose { handler.remove() }
    }
}
