package com.base.app.core.designsystem.component.sheet.back

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.base.app.core.designsystem.component.overlay.LocalOverlayHost
import com.base.app.core.designsystem.component.overlay.OverlayHostState
import com.base.app.core.designsystem.component.sheet.SheetController
import com.base.app.core.designsystem.component.sheet.SheetDismissSource
import com.base.app.core.designsystem.component.sheet.SheetModality

/**
 * The surface as a focus group: it can hold focus itself, so focus moves into a modal
 * sheet without opening a keyboard, Tab and the arrows stay inside it, and an Escape no field took
 * closes it as the Back key would. While [host]'s overlays are suspended it lets focus go, to the
 * cover that suspended them.
 */
internal fun Modifier.sheetFocus(controller: SheetController, host: OverlayHostState?): Modifier =
    this
        .onFocusChanged { controller.focusInside = it.hasFocus }
        .focusProperties {
            onExit = {
                val traps = controller.modality is SheetModality.Modal || controller.blocking
                if (traps && host?.suspended != true) cancelFocusChange()
            }
        }
        .focusRequester(controller.surfaceFocus)
        .focusGroup()
        .focusable()
        .onKeyEvent { event ->
            if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                controller.backWithoutPreview(SheetDismissSource.Escape)
                true
            } else {
                false
            }
        }

/**
 * While the sheet blocks what is behind it, focus is in it. On the first moving frame the
 * focus beneath is saved and the surface takes focus, which also closes a keyboard a field beneath
 * held; when the sheet stops blocking, focus goes back where it was.
 */
@Composable
internal fun SheetFocusEffect(controller: SheetController) {
    val host = LocalOverlayHost.current
    LaunchedEffect(controller, host) {
        snapshotFlow {
            val state = controller.sheetState
            state.isVisible && !state.isLeaving && state.parkProgress == 0f &&
                !state.parkedInstantly &&
                controller.blocking && host?.suspended != true
        }.collect { inside ->
            if (inside) {
                host?.saveFocus()
                controller.surfaceFocus.requestFocus()
            } else {
                host?.restoreFocus()
            }
        }
    }
}
