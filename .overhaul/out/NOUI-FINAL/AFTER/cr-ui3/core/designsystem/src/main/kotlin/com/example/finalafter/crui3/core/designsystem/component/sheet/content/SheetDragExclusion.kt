package com.example.finalafter.crui3.core.designsystem.component.sheet.content

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity

/**
 * A region of the content where a drag never moves the sheet: a canvas, a signature, a map, a
 * chart. Its own pointer input and scrolling are untouched as long as they come after this in the
 * modifier chain or in its children, which see every event first.
 */
fun Modifier.sheetDragExclusion(): Modifier = this
    .nestedScroll(KeepScroll)
    .then(DragExclusionElement)

/**
 * Consumes every pointer change after the region's own handlers on the Main pass, which runs from the
 * innermost node outward: the sheet's draggable, further out, sees them consumed and never starts.
 */
private object DragExclusionElement : ModifierNodeElement<DragExclusionNode>() {
    override fun create() = DragExclusionNode()

    override fun update(node: DragExclusionNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "sheetDragExclusion"
    }

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = other === this
}

private class DragExclusionNode : Modifier.Node(), PointerInputModifierNode {
    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass == PointerEventPass.Main) pointerEvent.changes.forEach { it.consume() }
    }

    override fun onCancelPointerInput() = Unit
}

/** What a scroll inside the region leaves over stays in it, so none reaches the sheet. */
private object KeepScroll : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}
