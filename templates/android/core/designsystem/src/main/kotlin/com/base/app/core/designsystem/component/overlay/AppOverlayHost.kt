package com.base.app.core.designsystem.component.overlay

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.base.app.core.designsystem.component.sheet.stack.LocalSheetStack
import com.base.app.core.designsystem.component.sheet.stack.SheetStack
import com.base.app.core.designsystem.component.sheet.surface.LocalSheetBackdrop
import com.base.app.core.designsystem.component.sheet.surface.SheetBackdropHolder
import com.base.app.core.designsystem.foundation.interaction.LocalContainerActions
import com.base.app.core.designsystem.foundation.layout.LocalEntryVisibility

/**
 * The top two layers of the window. Layer 4 holds in-screen sheets, alerts, HUDs and the loading
 * overlay; layer 5 the toasts. Both sit over everything else the app draws, the tab bar and presented
 * sheets included, which is where iOS puts them too.
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

    /** The window's sheets, for whatever recedes and dims behind them. */
    internal val sheetStack = SheetStack()

    /** Where the liquid glass stage hands Glass sheets their live backdrop. */
    internal val sheetBackdrop = SheetBackdropHolder()

    /** The app content beneath the overlays, which saves and gets back its focus around a modal overlay. */
    internal val appFocus = FocusRequester()

    /** True while a modal overlay is up: the content beneath is hidden from screen readers and from focus. */
    internal val modal: Boolean
        get() = overlays.any { it.modal() }

    /** Read from the focus properties of the content beneath, so Tab never leaves a modal overlay. */
    internal var trapsFocus = false

    internal fun saveFocus() {
        appFocus.saveFocusedChild()
    }

    internal fun restoreFocus() {
        appFocus.restoreFocusedChild()
    }
}

/**
 * One overlay, keyed by the call site that shows it and composed in that call site's saved state, so
 * what its content saved survives recreation and process death.
 */
internal class OverlayEntry(
    val key: Any,
    val modal: () -> Boolean,
    val parked: () -> Boolean,
    val content: @Composable () -> Unit,
)

/** The window's overlay host, or null outside an `AppTheme` (a bare preview). */
val LocalOverlayHost = staticCompositionLocalOf<OverlayHostState?> { null }

/** Installed by the outermost `AppTheme` around everything the app draws. */
@Composable
internal fun AppOverlayHost(content: @Composable () -> Unit) {
    val state = remember { OverlayHostState() }
    CompositionLocalProvider(
        LocalOverlayHost provides state,
        LocalSheetStack provides state.sheetStack,
        LocalSheetBackdrop provides state.sheetBackdrop,
    ) {
        Box(propagateMinConstraints = true) {
            Box(
                modifier = Modifier
                    .hiddenBeneathOverlays(state)
                    .focusRequester(state.appFocus)
                    .focusProperties { onEnter = { if (state.trapsFocus) cancelFocusChange() } }
                    .focusGroup(),
                propagateMinConstraints = true,
            ) {
                content()
            }
            // The layers are siblings of the app's root, outside whatever exposes its test tags, so
            // they expose their own: benchmarks find a sheet's grabber or a toast by resource id.
            // No pointer modifiers here: a touch that misses every placed overlay reaches the app.
            Box(Modifier.semantics { testTagsAsResourceId = true }, propagateMinConstraints = true) {
                state.overlays.forEach { entry ->
                    key(entry.key) {
                        Box(Modifier.parkedOverlay(entry.parked), propagateMinConstraints = true) { entry.content() }
                    }
                }
                ToastLayer(state.toasts)
            }
        }
    }
}

/**
 * Draws [content] in the window's overlay layer for as long as this is composed, or in place when
 * there is no host. [modal] hides everything beneath from screen readers and focus while it returns
 * true; it is read where those apply, so a change recomposes nothing.
 *
 * The content keeps the caller's composition locals although it is drawn at the window's root: a
 * sheet opened from a screen gets that screen's ViewModels, lifecycle and saved state, and a nested
 * theme or a right-to-left page reaches it too. It is parked, laid out but neither drawn nor
 * announced, while the caller's tab is not the selected one. Calls made in a loop need `key(…)`
 * around them, as `rememberSaveable` does.
 */
@Composable
internal fun HostedOverlay(modal: () -> Boolean, content: @Composable () -> Unit) {
    val host = LocalOverlayHost.current
    if (host == null) {
        content()
        return
    }
    val latest = rememberUpdatedState(content)
    val locals = rememberUpdatedState(currentCompositionLocalContext)
    val isModal = rememberUpdatedState(modal)
    val holder: SaveableStateHolder = rememberSaveableStateHolder()
    val callSite = currentCompositeKeyHashCode
    val visibility = LocalEntryVisibility.current
    val entry = remember(host, holder) {
        OverlayEntry(
            key = callSite,
            modal = { isModal.value() },
            parked = { !visibility.isTabSelected },
        ) {
            CompositionLocalProvider(locals.value) {
                holder.SaveableStateProvider(callSite) {
                    // An overlay is not inside the row that opened it: its controls carry no row actions.
                    CompositionLocalProvider(LocalContainerActions provides null) { latest.value() }
                }
            }
        }
    }
    DisposableEffect(host, entry) {
        host.overlays.add(entry)
        onDispose { host.overlays.remove(entry) }
    }
}
