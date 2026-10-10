package com.base.app.core.navigation.shell.tabs

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.sign

/**
 * The tab arriving in front after a tap: placed [SHIFT] toward the direction of travel under a
 * veil in the background colour at [VEIL_ALPHA], both springing away on iOS's segmented-thumb
 * spring (two thirds done at 100 ms). Only the incoming tab is drawn, so there is no double
 * exposure, and the veil is one rectangle, not an alpha on the whole screen. A veil that started
 * opaque would put a blank frame on screen at the tap, which reads as a flash.
 */
@Stable
internal class TabReveal(selected: Int) {
    /** The tab being revealed, or -1. */
    var index by mutableIntStateOf(-1)
        private set

    /** +1 when the tab came from the end side, -1 from the start side. */
    var direction = 0f
        private set

    /** 0 at the switch, 1 revealed. Read in layer and draw blocks only. */
    var progress by mutableFloatStateOf(1f)
        private set

    private var shown = selected

    /** [position] is where a tab sits along the chrome, which the nudge follows. */
    fun switchTo(selected: Int, animate: Boolean, position: (Int) -> Int) {
        if (selected == shown) return
        if (animate) {
            index = selected
            direction = sign((position(selected) - position(shown)).toFloat())
            progress = 0f
        } else {
            index = -1
            progress = 1f
        }
        shown = selected
    }

    suspend fun run(selected: Int) {
        if (index != selected || progress >= 1f) return
        animate(progress, 1f, animationSpec = RevealSpring) { value, _ -> progress = value }
    }

    companion object {
        val SHIFT = 12.dp
        const val VEIL_ALPHA = 0.6f
    }
}

/**
 * One kept tab: parked while hidden (placed, layer at alpha 0 off the window, semantics cleared),
 * and nudged and veiled while [reveal] brings it in. Everything it changes per frame is read in its
 * layer and draw blocks.
 */
internal fun Modifier.keptTab(parked: Boolean, reveal: TabReveal, index: Int, veil: Color): Modifier =
    this then KeptTabElement(parked, reveal, index, veil)

private data class KeptTabElement(
    val parked: Boolean,
    val reveal: TabReveal,
    val index: Int,
    val veil: Color,
) : ModifierNodeElement<KeptTabNode>() {
    override fun create() = KeptTabNode(parked, reveal, index, veil)

    override fun update(node: KeptTabNode) {
        if (node.parked != parked) {
            node.parked = parked
            node.invalidateSemantics()
        }
        node.reveal = reveal
        node.index = index
        if (node.veil != veil) {
            node.veil = veil
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "keptTab"
        properties["parked"] = parked
        properties["index"] = index
    }
}

private class KeptTabNode(
    parked: Boolean,
    var reveal: TabReveal,
    var index: Int,
    var veil: Color,
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode, SemanticsModifierNode {

    // State, so the layer block that reads it runs again when it changes, without a new layout.
    private var parkedState by mutableStateOf(parked)
    var parked: Boolean
        get() = parkedState
        set(value) {
            parkedState = value
        }

    private var towardEnd = 1f

    private val layer: GraphicsLayerScope.() -> Unit = {
        if (parkedState) {
            alpha = 0f
            translationX = -2f * size.width
        } else if (reveal.index == index) {
            translationX = towardEnd * reveal.direction * TabReveal.SHIFT.toPx() * (1f - reveal.progress)
        }
    }

    override val shouldClearDescendantSemantics: Boolean get() = parkedState

    override fun SemanticsPropertyReceiver.applySemantics() = Unit

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        towardEnd = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
        return layout(placeable.width, placeable.height) { placeable.placeWithLayer(0, 0, layerBlock = layer) }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (parkedState || reveal.index != index) return
        val alpha = TabReveal.VEIL_ALPHA * (1f - reveal.progress)
        if (alpha > 0f) drawRect(veil, alpha = alpha)
    }
}

/** iOS's segmented-thumb spring: response 0.28 s, no bounce. */
private val RevealSpring = spring(dampingRatio = 1f, stiffness = 503.5f, visibilityThreshold = 0.001f)
