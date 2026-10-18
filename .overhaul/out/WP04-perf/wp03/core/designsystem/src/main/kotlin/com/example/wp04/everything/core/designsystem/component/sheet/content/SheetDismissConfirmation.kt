package com.example.wp04.everything.core.designsystem.component.sheet.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.wp04.everything.core.designsystem.R
import com.example.wp04.everything.core.designsystem.component.sheet.action.AppActionSheet
import com.example.wp04.everything.core.designsystem.component.sheet.action.SheetAction

/**
 * Asks before the enclosing sheet closes while [enabled]: a drag, the dim, Back, Escape or the close
 * button opens a stacked action sheet with [title], a destructive [discardLabel] that calls
 * [onDiscard], and Keep Editing. Built from [SheetDismissGuard] and [AppActionSheet]; build those
 * yourself for a different question.
 */
@Composable
fun SheetDismissConfirmation(
    enabled: Boolean,
    onDiscard: () -> Unit,
    title: String = stringResource(R.string.designsystem_sheet_discard_title),
    discardLabel: String = stringResource(R.string.designsystem_sheet_discard),
) {
    var asking by rememberSaveable { mutableStateOf(false) }
    SheetDismissGuard(enabled = enabled) { asking = true }
    val discard by rememberUpdatedState(onDiscard)
    val actions = remember(discardLabel) {
        listOf(SheetAction(label = discardLabel, onClick = { discard() }, destructive = true))
    }
    AppActionSheet(
        visible = asking,
        actions = actions,
        onDismissRequest = { asking = false },
        title = title,
        cancelLabel = stringResource(R.string.designsystem_sheet_keep_editing),
    )
}
