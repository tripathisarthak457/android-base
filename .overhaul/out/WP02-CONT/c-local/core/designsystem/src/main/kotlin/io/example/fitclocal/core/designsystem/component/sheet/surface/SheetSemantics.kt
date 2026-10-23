package io.example.fitclocal.core.designsystem.component.sheet.surface

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import io.example.fitclocal.core.designsystem.component.sheet.SheetController
import io.example.fitclocal.core.designsystem.component.sheet.chrome.SheetLabels
import io.example.fitclocal.core.designsystem.component.sheet.chrome.sheetActions
import kotlinx.coroutines.launch

/**
 * The surface's semantics: the pane title, first in the sheet's traversal, and the grabber's
 * state and actions when no grabber is drawn. `dialog()` marks a modal sheet for tests; screen
 * readers use the pane title and the focus moved into the sheet.
 */
internal fun Modifier.sheetSemantics(
    controller: SheetController,
    paneTitle: String,
    labels: SheetLabels,
    grabberShown: Boolean,
    modal: Boolean,
): Modifier = semantics {
    this.paneTitle = paneTitle
    traversalIndex = 0f
    if (modal) dialog()
    if (!grabberShown) sheetActions(controller, labels)
}.then(ParkedSemanticsElement(controller))

/**
 * While the sheet is hidden or parked, nothing in it is announced. The flag is read from a flow and
 * applied by invalidating these semantics, so a tab switch recomposes nothing in the sheet.
 */
private data class ParkedSemanticsElement(val controller: SheetController) : ModifierNodeElement<ParkedSemanticsNode>() {
    override fun create() = ParkedSemanticsNode(controller)

    override fun update(node: ParkedSemanticsNode) {
        node.controller = controller
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "sheetParkedSemantics"
    }
}

private class ParkedSemanticsNode(var controller: SheetController) : Modifier.Node(), SemanticsModifierNode {
    private var hidden = false

    override val shouldClearDescendantSemantics: Boolean
        get() = hidden

    override fun onAttach() {
        coroutineScope.launch {
            snapshotFlow {
                val state = controller.sheetState
                !state.isVisible || state.parkedInstantly || state.parkProgress >= 1f
            }.collect { gone ->
                if (gone != hidden) {
                    hidden = gone
                    invalidateSemantics()
                }
            }
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() = Unit
}
