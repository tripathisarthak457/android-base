package com.base.app.core.designsystem.component.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * The top two layers of the window. Layer 4 holds alerts, HUDs and the loading overlay; layer 5 the
 * toasts. Both sit over everything else the app draws, the tab bar and presented sheets included,
 * which is where iOS puts them too.
 *
 * `AppTheme` installs one host at the window root. Components such as [AppAlert] and
 * [AppLoadingOverlay] are called from a screen as usual and render through it, so a screen never
 * draws them under the bar. A screen shows a toast through [toasts]:
 *
 * ```
 * val overlays = LocalOverlayHost.current
 * overlays?.toasts?.show(ToastMessage(text = saved))
 * ```
 */
@Stable
class OverlayHostState internal constructor() {
    internal val overlays = mutableStateListOf<OverlayEntry>()

    /** The toast queue. Toasts show one at a time, above the bottom chrome. */
    val toasts = ToastQueue()

    /** True while a modal overlay is up: the content beneath is hidden from screen readers too. */
    internal val modal: Boolean
        get() = overlays.any { it.modal }
}

internal class OverlayEntry(val modal: Boolean, val content: @Composable () -> Unit)

/** The window's overlay host, or null outside an `AppTheme` (a bare preview). */
val LocalOverlayHost = staticCompositionLocalOf<OverlayHostState?> { null }

/** Installed by the outermost `AppTheme` around everything the app draws. */
@Composable
internal fun AppOverlayHost(content: @Composable () -> Unit) {
    val state = remember { OverlayHostState() }
    CompositionLocalProvider(LocalOverlayHost provides state) {
        Box(propagateMinConstraints = true) {
            Box(
                modifier = if (state.modal) Modifier.clearAndSetSemantics {} else Modifier,
                propagateMinConstraints = true,
            ) {
                content()
            }
            state.overlays.forEach { entry -> key(entry) { entry.content() } }
            ToastLayer(state.toasts)
        }
    }
}

/**
 * Draws [content] in the window's overlay layer for as long as this is composed, or in place when
 * there is no host. [modal] hides everything beneath from screen readers while it is up.
 */
@Composable
internal fun HostedOverlay(modal: Boolean, content: @Composable () -> Unit) {
    val host = LocalOverlayHost.current
    if (host == null) {
        content()
        return
    }
    val latest = rememberUpdatedState(content)
    DisposableEffect(host, modal) {
        val entry = OverlayEntry(modal) { latest.value() }
        host.overlays.add(entry)
        onDispose { host.overlays.remove(entry) }
    }
}
