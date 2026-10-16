package com.base.app.core.designsystem.component.sheet.stack

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import com.base.app.core.designsystem.theme.LocalReduceMotion
import kotlin.math.roundToInt

/**
 * Recedes this layout behind the sheet above [level], as iOS's card stack does, and draws that
 * sheet's dim over it. One layer (scale, translation and a round-rect outline clip) and one rect,
 * all read in layer and draw blocks: the presenter never recomposes while the sheet moves.
 *
 * The shell applies it at level 0 and a full-screen cover at its depth. What shows around the
 * receded card is whatever lies beneath it, which the presenter's host turns black while it recedes.
 */
fun Modifier.cardStackPresenter(stack: SheetStack, level: Int = 0): Modifier =
    this then CardStackLayerElement(stack, level) then CardStackDimElement(stack, level)

private data class CardStackLayerElement(val stack: SheetStack, val level: Int) : ModifierNodeElement<CardStackLayerNode>() {
    override fun create() = CardStackLayerNode(stack, level)

    override fun update(node: CardStackLayerNode) {
        node.stack = stack
        node.level = level
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "cardStackPresenter"
        properties["level"] = level
    }
}

private class CardStackLayerNode(var stack: SheetStack, var level: Int) :
    Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    private var safeTop = 0f
    private var displayCorner = 0f

    /** Outline shapes by whole pixel of radius: a few dozen at most, never one per frame. */
    private val shapes = HashMap<Int, Shape>()

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val expansion = stack.recede(level)
        if (expansion <= 0f || currentValueOf(LocalReduceMotion).value) {
            scaleX = 1f
            scaleY = 1f
            translationY = 0f
            clip = false
            shape = RectangleShape
        } else {
            transformOrigin = TopCentre
            val scale = CardStackMath.scale(expansion)
            scaleX = scale
            scaleY = scale
            translationY = CardStackMath.translationY(expansion, safeTop)
            val radius = CardStackMath.corner(expansion, displayCorner, density).roundToInt()
            shape = shapes.getOrPut(radius) { RoundedCornerShape(radius.toFloat()) }
            clip = true
        }
    }

    override fun onAttach() {
        displayCorner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            currentValueOf(LocalView).rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius?.toFloat() ?: 0f
        } else {
            0f
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            safeTop = WindowInsetsRulers.SafeDrawing.current.top.current(0f)
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }
}

private data class CardStackDimElement(val stack: SheetStack, val level: Int) : ModifierNodeElement<CardStackDimNode>() {
    override fun create() = CardStackDimNode(stack, level)

    override fun update(node: CardStackDimNode) = node.update(stack, level)

    override fun InspectorInfo.inspectableProperties() {
        name = "cardStackDim"
        properties["level"] = level
    }
}

/** Inside the card's layer, so the dim recedes with it; it claims the level so the sheets' own scrims stay clear. */
private class CardStackDimNode(private var stack: SheetStack, private var level: Int) : Modifier.Node(), DrawModifierNode {

    fun update(stack: SheetStack, level: Int) {
        if (stack === this.stack && level == this.level) return
        if (isAttached) this.stack.release(this.level)
        this.stack = stack
        this.level = level
        if (isAttached) stack.claim(level)
    }

    override fun onAttach() = stack.claim(level)

    override fun onDetach() = stack.release(level)

    override fun ContentDrawScope.draw() {
        drawContent()
        val dim = stack.dim(level)
        if (dim.alpha > 0f) drawRect(dim)
    }
}

private val TopCentre = TransformOrigin(0.5f, 0f)
