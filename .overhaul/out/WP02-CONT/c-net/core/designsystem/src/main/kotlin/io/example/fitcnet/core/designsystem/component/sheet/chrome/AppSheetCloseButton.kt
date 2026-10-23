package io.example.fitcnet.core.designsystem.component.sheet.chrome

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.example.fitcnet.core.designsystem.R
import io.example.fitcnet.core.designsystem.component.button.AppButtonDefaults
import io.example.fitcnet.core.designsystem.component.button.AppIconButton
import io.example.fitcnet.core.designsystem.component.button.ButtonSize
import io.example.fitcnet.core.designsystem.component.button.ButtonVariant
import io.example.fitcnet.core.designsystem.component.sheet.LocalSheetSurface
import io.example.fitcnet.core.designsystem.component.sheet.SheetController
import io.example.fitcnet.core.designsystem.icon.AppIcons
import io.example.fitcnet.core.designsystem.theme.AppTheme

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
