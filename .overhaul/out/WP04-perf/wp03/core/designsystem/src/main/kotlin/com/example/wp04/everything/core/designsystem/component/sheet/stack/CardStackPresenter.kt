package com.example.wp04.everything.core.designsystem.component.sheet.stack

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import com.example.wp04.everything.core.designsystem.foundation.layout.ShellSeamApi
import com.example.wp04.everything.core.designsystem.theme.LocalReduceMotion
import android.graphics.Path as PlatformPath

/**
 * Recedes this layout behind the sheet above [level], as iOS's card stack does, and draws the dim
 * that reaches [level] over it. Three nodes in this order: a recede layer (scale and translation,
 * no shape, no clip), a draw node (the round-rect and visible-band clip, then the content layer,
 * then the dim rect), and a content layer with no properties. A frame of sheet motion re-records
 * only the draw node's few operations; the presenter never recomposes and its content is never
 * re-recorded.
 *
 * What shows around the receded card is whatever lies beneath it, which the presenter's host turns
 * black while it recedes.
 */
@ShellSeamApi
fun Modifier.cardStackPresenter(stack: SheetStack, level: Int = 0): Modifier =
    this then RecedeElement(stack, level) then DimElement(stack, level) then ContentLayerElement

private data class RecedeElement(val stack: SheetStack, val level: Int) : ModifierNodeElement<RecedeNode>() {
    override fun create() = RecedeNode(stack, level)

    override fun update(node: RecedeNode) {
        node.stack = stack
        node.level = level
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "cardStackPresenter"
        properties["level"] = level
    }
}

private class RecedeNode(var stack: SheetStack, var level: Int) :
    Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    private var safeTop = 0f

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val expansion = stack.recede(level)
        if (expansion <= 0f || currentValueOf(LocalReduceMotion).value) {
            scaleX = 1f
            scaleY = 1f
            translationY = 0f
        } else {
            transformOrigin = TopCentre
            val scale = CardStackMath.scale(expansion)
            scaleX = scale
            scaleY = scale
            translationY = CardStackMath.translationY(expansion, safeTop)
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

private data class DimElement(val stack: SheetStack, val level: Int) : ModifierNodeElement<DimNode>() {
    override fun create() = DimNode(stack, level)

    override fun update(node: DimNode) = node.update(stack, level)

    override fun InspectorInfo.inspectableProperties() {
        name = "cardStackDim"
        properties["level"] = level
    }
}

/**
 * Inside the recede layer, so the clip and the dim recede with the card. It claims its level, so
 * the sheet above draws no dim of its own while a presenter draws it here.
 */
private class DimNode(private var stack: SheetStack, private var level: Int) :
    Modifier.Node(), DrawModifierNode, LayoutModifierNode, CompositionLocalConsumerModifierNode {

    private val geometry = PresenterGeometry()
    private val platformPath = PlatformPath()
    private val clip = platformPath.asComposePath()
    private var displayCorner = 0f

    fun update(stack: SheetStack, level: Int) {
        if (stack === this.stack && level == this.level) return
        if (isAttached) this.stack.release(this.level, geometry)
        this.stack = stack
        this.level = level
        if (isAttached) stack.claim(level, geometry)
    }

    override fun onAttach() = stack.claim(level, geometry)

    override fun onDetach() = stack.release(level, geometry)

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        // Read where a rotation or a move to another display measures again, never only on attach.
        displayCorner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            currentValueOf(LocalView).rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius?.toFloat() ?: 0f
        } else {
            0f
        }
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            geometry.safeTop = WindowInsetsRulers.SafeDrawing.current.top.current(0f)
            geometry.windowTop = coordinates?.positionInWindow()?.y ?: 0f
            placeable.place(0, 0)
        }
    }

    override fun ContentDrawScope.draw() {
        val reduceMotion = currentValueOf(LocalReduceMotion).value
        geometry.reduceMotion = reduceMotion
        val expansion = stack.recede(level)
        val bottom = stack.visibleBottom(level).coerceAtMost(size.height)
        val dim = stack.dim(level)
        clipRect(bottom = bottom) {
            if (expansion > 0f && !reduceMotion) {
                val radius = CardStackMath.corner(expansion, displayCorner, density)
                platformPath.rewind()
                platformPath.addRoundRect(0f, 0f, size.width, size.height, radius, radius, PlatformPath.Direction.CW)
                clipPath(clip) { this@draw.drawContent() }
            } else {
                this@draw.drawContent()
            }
        }
        if (dim.alpha > 0f) drawRect(dim)
    }
}

/** A layer with no properties around the presenter's content, so the dim node's re-record never reaches it. */
private object ContentLayerElement : ModifierNodeElement<ContentLayerNode>() {
    override fun create() = ContentLayerNode()

    override fun update(node: ContentLayerNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "cardStackContent"
    }

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = other === this
}

private class ContentLayerNode : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.placeWithLayer(0, 0) }
    }
}

private val TopCentre = TransformOrigin(0.5f, 0f)
