package com.example.cmp.core.designsystem.component.refresh

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import com.example.cmp.core.designsystem.component.feedback.ActivitySpokes
import com.example.cmp.core.designsystem.component.feedback.drawActivitySpokes
import kotlin.math.ceil

/** What the pull shows: how far the content has moved down, and the state of the spinner in the gap. */
@Stable
internal class RefreshIndicator {
    var gap by mutableFloatStateOf(0f)

    /** The brightest spoke while the spinner turns. */
    var head by mutableIntStateOf(0)

    /** A refresh is showing, or a finished one is still closing over its spinner. */
    var spinning by mutableStateOf(false)

    /** The refresh has ended and the gap is closing; the spinner shrinks with it. */
    var retracting by mutableStateOf(false)
}

internal data class RefreshSpinnerElement(
    val indicator: RefreshIndicator,
    val color: ColorProducer,
) : ModifierNodeElement<RefreshSpinnerNode>() {
    override fun create() = RefreshSpinnerNode(indicator, color)

    override fun update(node: RefreshSpinnerNode) = node.update(color)

    override fun InspectorInfo.inspectableProperties() {
        name = "refreshSpinner"
    }
}

/**
 * The spinner, in the part of the gap below its top edge: its spokes appear one by one as the pull
 * nears the trigger, then turn while the refresh runs, and shrink as a finished refresh closes.
 */
internal class RefreshSpinnerNode(
    private val indicator: RefreshIndicator,
    private var color: ColorProducer,
) : Modifier.Node(), DrawModifierNode {

    fun update(color: ColorProducer) {
        if (color == this.color) return
        this.color = color
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val gap = indicator.gap
        // The gap opens from the top bar down; the spinner starts SpinnerTop below it.
        val uncovered = gap - AppPullToRefreshDefaults.SpinnerTop.toPx()
        if (uncovered <= 0.5f) return
        val spinning = indicator.spinning
        val retracting = indicator.retracting
        val radius = size.minDimension / 2f
        val pulled = (gap / AppPullToRefreshDefaults.Trigger.toPx()).coerceIn(0f, 1f)
        clipRect(bottom = uncovered) {
            drawActivitySpokes(
                center = center,
                radius = if (retracting) radius * (gap / AppPullToRefreshDefaults.Rest.toPx()).coerceIn(0f, 1f) else radius,
                color = color(),
                head = if (spinning) indicator.head else null,
                revealed = if (spinning) ActivitySpokes else ceil(ActivitySpokes * pulled).toInt(),
                alpha = if (spinning) 1f else FastOutSlowInEasing.transform((pulled / FadeInFraction).coerceAtMost(1f)),
            )
        }
    }
}

/** The spinner fades in over the first part of the pull, then the spokes alone show progress. */
private const val FadeInFraction = 0.35f
