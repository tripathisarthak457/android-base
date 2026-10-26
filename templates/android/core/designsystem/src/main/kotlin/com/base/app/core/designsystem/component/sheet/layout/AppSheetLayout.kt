package com.base.app.core.designsystem.component.sheet.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.sheet.AppSheetDefaults
import com.base.app.core.designsystem.component.sheet.AppSheetProperties
import com.base.app.core.designsystem.component.sheet.AppSheetScope
import com.base.app.core.designsystem.component.sheet.AppSheetState
import com.base.app.core.designsystem.component.sheet.AppSheetStyle
import com.base.app.core.designsystem.component.sheet.SheetDetent
import com.base.app.core.designsystem.component.sheet.SheetModality
import com.base.app.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.base.app.core.designsystem.component.sheet.content.SheetColumn
import com.base.app.core.designsystem.component.sheet.rememberAppSheetState
import com.base.app.core.designsystem.foundation.layout.ShellSeamApi
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

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
public fun AppSheetLayout(
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

@AppComponentPreviews
@Composable
private fun AppSheetLayoutPreview() {
    Box(
        modifier = Modifier.background(
            AppTheme.colors.background,
        ).fillMaxWidth().height(AppTheme.sizes.rowHeight * 8),
    ) {
        AppSheetLayout(
            state = rememberAppSheetState(),
            onDismissRequest = {},
            header = { AppSheetHeader(title = "New list") },
        ) {
            SheetColumn {
                AppRow(title = "A sheet that is a destination of its own")
            }
        }
    }
}
