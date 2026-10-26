package com.base.app.core.designsystem.component.sheet.content

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.sheet.AppSheet
import com.base.app.core.designsystem.component.sheet.LocalSheetSurface
import com.base.app.core.designsystem.component.sheet.SheetController
import com.base.app.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.base.app.core.designsystem.component.sheet.content.SheetColumn
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/** The enclosing sheet's contentPadding; zero outside a sheet. */
@Composable
@ReadOnlyComposable
public fun sheetContentPadding(): PaddingValues =
    LocalSheetSurface.current?.scope?.contentPadding ?: NoPadding

/**
 * A scrolling column with the sheet's contentPadding applied inside the scroll: a form, a settings
 * group. When the sheet's footer cannot stay pinned (large text in a short window), it is composed
 * here, after [content].
 */
@Composable
public fun SheetColumn(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = sheetContentPadding(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(state, flingBehavior = AppScrollDefaults.flingBehavior())
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
    ) {
        content()
        UnpinnedSheetFooter()
    }
}

/** In a lazy list, the sheet's footer as the last item when it cannot stay pinned; nothing otherwise. */
public fun LazyListScope.sheetFooterItem() {
    item(key = FooterItemKey, contentType = FooterItemKey) { UnpinnedSheetFooter() }
}

@Composable
private fun UnpinnedSheetFooter() {
    val controller = LocalSheetSurface.current?.scope as? SheetController ?: return
    if (!controller.footerPinned) SheetFooterContent(controller, controller.callSiteFooter)
}

private val NoPadding = PaddingValues(0.dp)
private const val FooterItemKey = "sheet_footer"

@AppComponentPreviews
@Composable
private fun SheetColumnPreview() {
    Box(
        modifier = Modifier.background(
            AppTheme.colors.background,
        ).fillMaxWidth().height(AppTheme.sizes.rowHeight * 8),
    ) {
        AppSheet(
            visible = true,
            onDismissRequest = {},
            header = { AppSheetHeader(title = "Account") },
        ) {
            SheetColumn(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                AppRow(title = "Name", value = "Ada Lovelace")
                AppRow(title = "Email", value = "ada@example.com")
                AppRow(title = "Sign out", onClick = {}, destructive = true)
            }
        }
    }
}
