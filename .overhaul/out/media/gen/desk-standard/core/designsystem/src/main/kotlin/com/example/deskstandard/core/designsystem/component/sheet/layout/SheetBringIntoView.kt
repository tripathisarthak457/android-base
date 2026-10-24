package com.example.deskstandard.core.designsystem.component.sheet.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.relocation.bringIntoView
import com.example.deskstandard.core.designsystem.component.sheet.SheetController

/**
 * The content is measured at Large's height, so at a partial detent a focused row can sit
 * below what shows. A request to bring such a row into view first raises the sheet to the lowest
 * detent that shows it, else the tallest, then goes on to the parent. Keyboard Tab past the last
 * visible row therefore raises the sheet rather than focusing a row off the window.
 */
internal fun Modifier.sheetBringIntoView(controller: SheetController): Modifier = this then SheetBringIntoViewElement(controller)

private data class SheetBringIntoViewElement(val controller: SheetController) :
    ModifierNodeElement<SheetBringIntoViewNode>() {
    override fun create() = SheetBringIntoViewNode(controller)

    override fun update(node: SheetBringIntoViewNode) {
        node.controller = controller
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "sheetBringIntoView"
    }
}

private class SheetBringIntoViewNode(var controller: SheetController) : Modifier.Node(), BringIntoViewModifierNode {
    override suspend fun bringIntoView(childCoordinates: LayoutCoordinates, boundsProvider: () -> Rect?) {
        val rect = boundsProvider() ?: return
        raiseToShow(childCoordinates.localToWindow(Offset(rect.left, rect.bottom)).y)
        val own = requireLayoutCoordinates()
        bringIntoView { boundsProvider()?.let { own.localBoundingBoxOf(childCoordinates).let { box -> it.translate(box.topLeft) } } }
    }

    private suspend fun raiseToShow(bottom: Float) {
        val state = controller.sheetState
        val stops = state.stops ?: return
        val frame = controller.frame ?: return
        if (stops.single || state.isMoving || !state.canMoveProgrammatically()) return
        val placed = state.placedTop()
        val visibleBottom = state.boundsTop + minOf(frame.restBottom, frame.boundsHeight - controller.imeHeight) -
            if (frame.footerPinned) frame.footerHeight else 0f
        if (bottom <= visibleBottom) return
        var choice = stops.detents[0]
        for (index in stops.count - 1 downTo 0) {
            if (bottom - (placed - stops.offsets[index]) <= visibleBottom) {
                choice = stops.detents[index]
                break
            }
        }
        if (choice != state.currentDetent) state.animateTo(choice)
    }
}
