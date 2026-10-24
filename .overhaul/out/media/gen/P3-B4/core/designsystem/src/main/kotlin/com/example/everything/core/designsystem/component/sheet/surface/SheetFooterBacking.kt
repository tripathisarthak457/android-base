package com.example.everything.core.designsystem.component.sheet.surface

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import com.example.everything.core.designsystem.component.sheet.SheetController
import com.example.everything.core.designsystem.component.sheet.SheetSurface

/**
 * Carries a footer's [color] on below it, over the strip a sheet keeps clear at its bottom: the
 * navigation bar, or the tab bar a non-modal sheet rests on. The sheet pins its footer above that
 * strip while the content scrolls on under both, so without this the rows show through between the
 * footer and the window's edge. It draws outside the footer's bounds and inside the surface's clip,
 * which cuts it wherever the surface ends higher: a floating sheet, a form sheet.
 * Outside a sheet it adds nothing.
 */
internal fun Modifier.sheetFooterBacking(surface: SheetSurface?, color: Color): Modifier =
    if (surface == null) this else this then SheetFooterBackingElement(surface, color)

private data class SheetFooterBackingElement(val surface: SheetSurface, val color: Color) :
    ModifierNodeElement<SheetFooterBackingNode>() {
    override fun create() = SheetFooterBackingNode(surface, color)

    override fun update(node: SheetFooterBackingNode) {
        node.surface = surface
        node.color = color
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "sheetFooterBacking"
        properties["color"] = color
    }
}

private class SheetFooterBackingNode(var surface: SheetSurface, var color: Color) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        // The frame changes only with a new layout, so this re-records on a settle, never per frame.
        val strip = (surface.scope as? SheetController)?.frame?.bottomInset ?: 0f
        if (strip > 0f) drawRect(color, topLeft = Offset(0f, size.height), size = Size(size.width, strip))
        drawContent()
    }
}
