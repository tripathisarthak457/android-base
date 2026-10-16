package com.base.app.core.designsystem.component.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.base.app.core.designsystem.component.overlay.HostedOverlay
import com.base.app.core.designsystem.component.sheet.layout.SheetSession

/**
 * A sheet in the app's design language with iOS 26 behaviour. It rises to one of [state]'s detents,
 * follows the finger between them, floats inset with rounded corners at partial detents and attaches
 * to the window's edges at [SheetDetent.Large], where the screen behind recedes into a card on a
 * phone. The person closes it by dragging it down, tapping the dimmed screen, Back (the predictive
 * shrink), Escape, or a close button.
 *
 * Keep it composed and drive [visible]: do not put the call inside an `if` or a lazy item. Showing
 * it again while it leaves turns it around at the speed it had. [onDismissRequest] runs once, when
 * the person has decided to close it, and the sheet is already leaving: set [visible] to false there.
 * [visible] is the truth: a sheet closed from [AppSheetState.hide] while [visible] is still true
 * calls [onDismissRequest] too.
 *
 * The content is laid out in a box as tall as the sheet at [SheetDetent.Large], children at the top
 * start. Bring one root: a lazy list or [com.base.app.core.designsystem.component.sheet.content.SheetColumn],
 * given [AppSheetScope.contentPadding], which keeps the gutter at the sides and the last item above
 * the footer, the navigation bar and the keyboard. The content is never re-measured during a drag.
 * The sheet adds no scroll container.
 *
 * A modal sheet draws over the whole window, the tab bar included, and takes focus while it is up.
 * A non-modal sheet stays within its pane and rests above the tab bar at its undimmed detents.
 *
 * @param modifier applied to the sheet's surface, for test tags, semantics and focus. The sheet owns
 *   its size, position, padding, clip and layers; such modifiers here are ignored where they conflict.
 * @param state detents and position; one per call site.
 * @param dismissible false: the person cannot close the sheet. A drag down rubber-bands, Back on a
 *   modal sheet is consumed, and attempts go to a [com.base.app.core.designsystem.component.sheet.content.SheetDismissGuard]
 *   in the content. To ask before closing a dirty form, use a guard or
 *   [com.base.app.core.designsystem.component.sheet.content.SheetDismissConfirmation] instead. Code
 *   can always close it.
 * @param paneTitle what screen readers call the sheet. Wins over the header's title; without either,
 *   "Sheet".
 * @param onDetentChange runs when the person settles the sheet on a different detent; not for moves
 *   made from code.
 * @param onHidden runs once the sheet has left the screen, however it was closed: show the next
 *   sheet from here.
 * @param grabber drawn over the top band; null for none. The sheet keeps the grabber's touch target,
 *   focus, click and accessibility actions whatever this draws, and hides it when the sheet has one
 *   active detent, is a form or side sheet, or is covered by another sheet.
 * @param header pinned above the content. Content can replace it with
 *   [com.base.app.core.designsystem.component.sheet.content.ProvideSheetHeader].
 * @param footer pinned at the visible bottom, above the navigation bar and the keyboard. Content can
 *   replace it with [com.base.app.core.designsystem.component.sheet.content.ProvideSheetFooter].
 */
@Composable
fun AppSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    state: AppSheetState = rememberAppSheetState(),
    modality: SheetModality = SheetModality.Modal,
    dismissible: Boolean = true,
    properties: AppSheetProperties = AppSheetProperties(),
    style: AppSheetStyle = AppSheetDefaults.style(),
    paneTitle: String? = null,
    onDetentChange: ((SheetDetent) -> Unit)? = null,
    onHidden: (() -> Unit)? = null,
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
    header: (@Composable AppSheetScope.() -> Unit)? = null,
    footer: (@Composable AppSheetScope.() -> Unit)? = null,
    content: @Composable AppSheetScope.() -> Unit,
) {
    HostedSheet(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        state = state,
        modality = modality,
        dismissible = dismissible,
        properties = properties,
        style = style,
        paneTitle = paneTitle,
        onDetentChange = onDetentChange,
        onHidden = onHidden,
        grabber = grabber,
        header = header,
        footer = footer,
        content = content,
    )
}

/**
 * The `visible` sheet, hosted in the window's overlay layer: what [AppSheet] and the presets draw
 * through. [asDialog] makes it arrive on the dialog spring, as alerts and action sheets do.
 */
@Composable
internal fun HostedSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    state: AppSheetState,
    modality: SheetModality,
    dismissible: Boolean,
    properties: AppSheetProperties,
    style: AppSheetStyle,
    paneTitle: String?,
    onDetentChange: ((SheetDetent) -> Unit)?,
    onHidden: (() -> Unit)?,
    grabber: (@Composable AppSheetScope.() -> Unit)?,
    header: (@Composable AppSheetScope.() -> Unit)?,
    footer: (@Composable AppSheetScope.() -> Unit)?,
    content: @Composable AppSheetScope.() -> Unit,
    asDialog: Boolean = false,
) {
    LaunchedEffect(state, visible) {
        if (visible) {
            state.show()
        } else {
            state.restoredDetent = null
            state.hide()
        }
    }
    if (!visible && !state.isVisible && !properties.keepContent) return
    HostedOverlay(modal = { state.blocking }) {
        SheetSession(
            state = state,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            modality = modality,
            dismissible = dismissible,
            properties = properties,
            style = style,
            paneTitle = paneTitle,
            onDetentChange = onDetentChange,
            onHidden = onHidden,
            callerShows = visible,
            grabber = grabber,
            header = header,
            footer = footer,
            content = content,
            asDialog = asDialog,
        )
    }
}

/**
 * A sheet for [item] while it is not null, like SwiftUI's `sheet(item:)`. The last item stays
 * composed during the exit. A different item while one is shown closes the sheet, then presents
 * the new one: one sheet at a time, as the HIG asks.
 */
@Composable
fun <T : Any> AppSheet(
    item: T?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    state: AppSheetState = rememberAppSheetState(),
    modality: SheetModality = SheetModality.Modal,
    dismissible: Boolean = true,
    properties: AppSheetProperties = AppSheetProperties(),
    style: AppSheetStyle = AppSheetDefaults.style(),
    paneTitle: String? = null,
    onDetentChange: ((SheetDetent) -> Unit)? = null,
    onHidden: (() -> Unit)? = null,
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
    header: (@Composable AppSheetScope.(T) -> Unit)? = null,
    footer: (@Composable AppSheetScope.(T) -> Unit)? = null,
    content: @Composable AppSheetScope.(T) -> Unit,
) {
    var shown by remember { mutableStateOf(item) }
    LaunchedEffect(state, item) {
        when {
            item == null -> state.hide()
            shown != null && shown != item && state.isVisible -> {
                state.hide()
                shown = item
                state.show()
            }
            else -> {
                shown = item
                state.show()
            }
        }
    }
    val current = shown ?: return
    if (item == null && !state.isVisible && !properties.keepContent) return
    HostedOverlay(modal = { state.blocking }) {
        SheetSession(
            state = state,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            modality = modality,
            dismissible = dismissible,
            properties = properties,
            style = style,
            paneTitle = paneTitle,
            onDetentChange = onDetentChange,
            onHidden = onHidden,
            // While one item gives way to another the caller still holds an item, but the close is
            // the sheet's own: it must not ask the caller to clear it.
            callerShows = item != null && item == current,
            grabber = grabber,
            header = header?.let { slot -> { slot(current) } },
            footer = footer?.let { slot -> { slot(current) } },
            content = { content(current) },
        )
    }
}
