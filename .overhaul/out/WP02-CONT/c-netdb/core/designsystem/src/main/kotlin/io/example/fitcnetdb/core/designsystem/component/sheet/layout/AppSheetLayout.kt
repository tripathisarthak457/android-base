package io.example.fitcnetdb.core.designsystem.component.sheet.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.example.fitcnetdb.core.designsystem.component.sheet.AppSheetDefaults
import io.example.fitcnetdb.core.designsystem.component.sheet.AppSheetProperties
import io.example.fitcnetdb.core.designsystem.component.sheet.AppSheetScope
import io.example.fitcnetdb.core.designsystem.component.sheet.AppSheetState
import io.example.fitcnetdb.core.designsystem.component.sheet.AppSheetStyle
import io.example.fitcnetdb.core.designsystem.component.sheet.SheetDetent
import io.example.fitcnetdb.core.designsystem.component.sheet.SheetModality
import io.example.fitcnetdb.core.designsystem.foundation.layout.ShellSeamApi

/**
 * The sheet drawn where it is composed. It appears and leaves only through [state]'s show() and
 * hide(); keep it composed until hide() returns. App code uses AppSheet.
 *
 * For a presenter that owns where a sheet is drawn, such as the navigation host's sheet scene. Such
 * a sheet is a destination: while it leaves, the presses of a rapid Back go to its host's gate rather
 * than being consumed by the sheet.
 */
@ShellSeamApi
@Composable
fun AppSheetLayout(
    state: AppSheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    modality: SheetModality = SheetModality.Modal,
    dismissible: Boolean = true,
    properties: AppSheetProperties = AppSheetProperties(),
    style: AppSheetStyle = AppSheetDefaults.style(),
    paneTitle: String? = null,
    onDetentChange: ((SheetDetent) -> Unit)? = null,
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
    header: (@Composable AppSheetScope.() -> Unit)? = null,
    footer: (@Composable AppSheetScope.() -> Unit)? = null,
    content: @Composable AppSheetScope.() -> Unit,
) {
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
        onHidden = null,
        callerShows = null,
        grabber = grabber,
        header = header,
        footer = footer,
        content = content,
        destination = true,
    )
}
