package com.base.app.core.designsystem.component.sheet.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.button.AppButtonDefaults
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.sheet.AppSheet
import com.base.app.core.designsystem.component.sheet.LocalSheetSurface
import com.base.app.core.designsystem.component.sheet.SheetController
import com.base.app.core.designsystem.component.sheet.chrome.AppSheetCloseButton
import com.base.app.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/**
 * Asks the enclosing sheet to close (guarded). Emits nothing while the person cannot close the sheet
 * at all (`dismissible = false` and no enabled guard), and outside a sheet, so a screen shown both as
 * a sheet and full screen can keep it.
 */
@Composable
public fun AppSheetCloseButton(modifier: Modifier = Modifier) {
    val scope = LocalSheetSurface.current?.scope ?: return
    if ((scope as? SheetController)?.closable == false) return
    AppIconButton(
        icon = AppIcons.Close,
        contentDescription = stringResource(R.string.designsystem_close),
        onClick = scope::requestDismiss,
        modifier = modifier,
        variant = ButtonVariant.Tinted,
        size = ButtonSize.Small,
        colors = AppButtonDefaults.iconButtonColors(ButtonVariant.Tinted, content = AppTheme.colors.contentSecondary),
    )
}

@AppComponentPreviews
@Composable
private fun AppSheetCloseButtonPreview() {
    Box(modifier = Modifier.background(AppTheme.colors.background).fillMaxWidth().height(AppTheme.sizes.rowHeight * 6)) {
        AppSheet(
            visible = true,
            onDismissRequest = {},
            header = { AppSheetHeader(title = "Share", trailing = { AppSheetCloseButton() }) },
        ) {
            AppRow(title = "Copy link", onClick = {})
        }
    }
}
