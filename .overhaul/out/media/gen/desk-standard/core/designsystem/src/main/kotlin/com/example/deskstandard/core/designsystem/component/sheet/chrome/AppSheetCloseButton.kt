package com.example.deskstandard.core.designsystem.component.sheet.chrome

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.deskstandard.core.designsystem.R
import com.example.deskstandard.core.designsystem.component.button.AppButtonDefaults
import com.example.deskstandard.core.designsystem.component.button.AppIconButton
import com.example.deskstandard.core.designsystem.component.button.ButtonSize
import com.example.deskstandard.core.designsystem.component.button.ButtonVariant
import com.example.deskstandard.core.designsystem.component.sheet.LocalSheetSurface
import com.example.deskstandard.core.designsystem.component.sheet.SheetController
import com.example.deskstandard.core.designsystem.icon.AppIcons
import com.example.deskstandard.core.designsystem.theme.AppTheme

/**
 * Asks the enclosing sheet to close (guarded). Emits nothing while the person cannot close the sheet
 * at all (`dismissible = false` and no enabled guard), and outside a sheet, so a screen shown both as
 * a sheet and full screen can keep it.
 */
@Composable
fun AppSheetCloseButton(modifier: Modifier = Modifier) {
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
