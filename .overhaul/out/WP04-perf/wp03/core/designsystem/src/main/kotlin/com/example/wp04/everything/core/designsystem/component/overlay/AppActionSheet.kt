package com.example.wp04.everything.core.designsystem.component.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.wp04.everything.core.designsystem.R
import com.example.wp04.everything.core.designsystem.component.sheet.action.AppActionSheet as SheetActionSheet
import com.example.wp04.everything.core.designsystem.component.sheet.action.SheetAction as ActionSheetAction

/** One choice in the earlier [AppActionSheet]. */
@Deprecated(
    "Moved to the sheet package, where destructive is spelt without the is.",
    ReplaceWith(
        "SheetAction(label, onClick, icon, destructive = isDestructive, enabled = enabled)",
        "com.example.wp04.everything.core.designsystem.component.sheet.action.SheetAction",
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
        "com.example.wp04.everything.core.designsystem.component.sheet.action.AppActionSheet",
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
