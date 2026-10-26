package com.example.cmp.core.designsystem.component.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.cmp.core.designsystem.component.list.AppRow
import com.example.cmp.core.designsystem.component.sheet.AppSheet
import com.example.cmp.core.designsystem.component.sheet.AppSheetDefaults
import com.example.cmp.core.designsystem.component.sheet.AppSheetProperties
import com.example.cmp.core.designsystem.component.sheet.SheetDetent
import com.example.cmp.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.example.cmp.core.designsystem.component.sheet.content.SheetColumn
import com.example.cmp.core.designsystem.component.sheet.rememberAppSheetState
import com.example.cmp.core.designsystem.preview.AppComponentPreviews
import com.example.cmp.core.designsystem.theme.AppTheme

/**
 * The earlier bottom sheet, shown for as long as it is composed. It is now an [AppSheet] that fits
 * its content: composed inside an `if`, it can only vanish when the `if` turns false, so move to
 * [AppSheet] and drive `visible` to let it slide away.
 */
@Deprecated(
    "Keep the sheet composed and drive visible, so it can slide away before it leaves.",
    ReplaceWith(
        "AppSheet(visible = true, onDismissRequest = onDismissRequest, modifier = modifier, " +
            "state = rememberAppSheetState(listOf(SheetDetent.FitContent))) { SheetColumn { content() } }",
        "com.example.cmp.core.designsystem.component.sheet.AppSheet",
        "com.example.cmp.core.designsystem.component.sheet.SheetDetent",
        "com.example.cmp.core.designsystem.component.sheet.rememberAppSheetState",
        "com.example.cmp.core.designsystem.component.sheet.content.SheetColumn",
    ),
)
@Composable
public fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    showHandle: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val properties = remember(dismissOnClickOutside) { AppSheetProperties(scrimTapDismisses = dismissOnClickOutside) }
    AppSheet(
        visible = true,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        state = rememberAppSheetState(listOf(SheetDetent.FitContent)),
        properties = properties,
        grabber = if (showHandle) AppSheetDefaults.Grabber else null,
        header = title?.let { text -> { AppSheetHeader(title = text) } },
    ) {
        SheetColumn(content = content)
    }
}

@AppComponentPreviews
@Composable
private fun AppBottomSheetPreview() {
    Box(modifier = Modifier.background(AppTheme.colors.background).fillMaxWidth().height(AppTheme.sizes.rowHeight * 8)) {
        AppBottomSheet(onDismissRequest = {}, title = "Sort by") {
            AppRow(title = "Newest first", onClick = {})
            AppRow(title = "Price, lowest first", onClick = {})
        }
    }
}
