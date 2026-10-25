package com.base.app.core.designsystem.component.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.component.sheet.action.AppActionSheet as SheetActionSheet
import com.base.app.core.designsystem.component.sheet.action.SheetAction as ActionSheetAction

/** One choice in the earlier [AppActionSheet]. */
@Deprecated(
    "Moved to the sheet package, where destructive is spelt without the is.",
    ReplaceWith(
        "SheetAction(label, onClick, icon, destructive = isDestructive, enabled = enabled)",
        "com.base.app.core.designsystem.component.sheet.action.SheetAction",
    ),
)
data class SheetAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val isDestructive: Boolean = false,
    val enabled: Boolean = true,
)

/** The earlier action sheet, shown for as long as it is composed. */
@Deprecated(
    "Keep the action sheet composed and drive visible, so it can slide away before it leaves.",
    ReplaceWith(
        "AppActionSheet(visible = true, actions = actions, onDismissRequest = onDismissRequest, modifier = modifier, " +
            "title = title, message = message)",
        "com.base.app.core.designsystem.component.sheet.action.AppActionSheet",
    ),
)
@Suppress("DEPRECATION")
@Composable
fun AppActionSheet(
    actions: List<SheetAction>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    cancelLabel: String? = stringResource(R.string.designsystem_cancel),
) {
    val converted = remember(actions) {
        actions.map { ActionSheetAction(it.label, it.onClick, it.icon, destructive = it.isDestructive, enabled = it.enabled) }
    }
    SheetActionSheet(
        visible = true,
        actions = converted,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = title,
        message = message,
        cancelLabel = cancelLabel ?: stringResource(R.string.designsystem_cancel),
    )
}

@AppComponentPreviews
@Composable
private fun AppActionSheetPreview() {
    Box(modifier = Modifier.background(AppTheme.colors.background).fillMaxWidth().height(AppTheme.sizes.rowHeight * 8)) {
        AppActionSheet(
            actions = listOf(
                SheetAction(label = "Share", onClick = {}),
                SheetAction(label = "Move to a folder with a long name", onClick = {}),
                SheetAction(label = "Archive", onClick = {}, enabled = false),
                SheetAction(label = "Delete", onClick = {}, icon = AppIcons.Trash, isDestructive = true),
            ),
            onDismissRequest = {},
            title = "Photo",
            message = "Choose what to do with the selected photo.",
        )
    }
}
