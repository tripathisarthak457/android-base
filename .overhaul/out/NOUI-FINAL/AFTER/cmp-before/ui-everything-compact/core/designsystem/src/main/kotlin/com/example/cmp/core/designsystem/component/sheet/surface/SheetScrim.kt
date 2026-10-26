package com.example.cmp.core.designsystem.component.sheet.surface

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import com.example.cmp.core.designsystem.R
import com.example.cmp.core.designsystem.component.sheet.SheetController
import com.example.cmp.core.designsystem.component.sheet.SheetDismissSource

/**
 * What lies over the sheet's bounds while the sheet blocks them: it takes the taps beside the
 * sheet, carries the "close" action for screen readers, and draws the dim only when no presenter
 * draws it, as on a catalog page or a preview without the shell, or for a sheet bounded by a pane.
 * It is placed only while it blocks, so an undimmed non-modal sheet lets every touch through.
 */
@Composable
internal fun SheetScrim(controller: SheetController) {
    val label = stringResource(R.string.designsystem_sheet_close_scrim)
    val dismisses = controller.properties.scrimTapDismisses
    Box(
        Modifier
            .testTag(ScrimTag)
            .semantics {
                // A screen reader dismisses through the sheet's own action (sheetActions), so the strip
                // left above a tall sheet, often thinner than a finger, is no target of its own.
                hideFromAccessibility()
                traversalIndex = 1f
                if (dismisses) {
                    contentDescription = label
                    onClick(label) {
                        controller.attemptOrClose(SheetDismissSource.Scrim)
                        true
                    }
                }
            }
            .pointerInput(controller) {
                detectTapGestures {
                    if (controller.properties.scrimTapDismisses) controller.attemptOrClose(SheetDismissSource.Scrim)
                }
            }
            .drawBehind {
                val stacked = controller.stacked
                val claimed = controller.stack?.isClaimed(stacked.level - 1) == true
                if (!claimed || stacked.paneBounded) {
                    val scrim = controller.style?.colors?.scrim ?: return@drawBehind
                    val presence = stacked.presence()
                    if (presence > 0f) drawRect(scrim, alpha = presence)
                }
            },
    )
}

internal const val ScrimTag = "sheet_scrim"
