package io.example.audit.core.designsystem.component.overlay

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.example.audit.core.designsystem.component.sheet.AppSheet
import io.example.audit.core.designsystem.component.sheet.AppSheetDefaults
import io.example.audit.core.designsystem.component.sheet.AppSheetProperties
import io.example.audit.core.designsystem.component.sheet.SheetDetent
import io.example.audit.core.designsystem.component.sheet.chrome.AppSheetHeader
import io.example.audit.core.designsystem.component.sheet.content.SheetColumn
import io.example.audit.core.designsystem.component.sheet.rememberAppSheetState

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
        "io.example.audit.core.designsystem.component.sheet.AppSheet",
        "io.example.audit.core.designsystem.component.sheet.SheetDetent",
        "io.example.audit.core.designsystem.component.sheet.rememberAppSheetState",
        "io.example.audit.core.designsystem.component.sheet.content.SheetColumn",
    ),
)
@Composable
fun AppBottomSheet(
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
