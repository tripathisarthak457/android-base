package com.base.app.core.designsystem.foundation.scroll

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import com.base.app.core.designsystem.theme.LocalAppSprings
import kotlin.math.abs
import kotlin.math.sign

/**
 * The iOS edge, opt-in: content pulled past its end follows the finger with growing resistance and
 * springs back when let go, and a fling that reaches the end carries on a little and returns.
 * Android's own stretch stays the default; a screen or the whole app chooses this one with
 *
 * ```
 * CompositionLocalProvider(LocalOverscrollFactory provides AppOverscroll) { … }
 * ```
 *
 * The offset is applied when the content is placed, so a pull redraws the list and recomposes and
 * re-measures nothing.
 */
object AppOverscroll : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = RubberBandOverscroll()

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 3
}

private class RubberBandOverscroll : OverscrollEffect {
    /** How far the finger has gone past the edge; what is drawn is this with resistance applied. */
    private var pull by mutableStateOf(Offset.Zero)
    private val settle = Animatable(Offset.Zero, Offset.VectorConverter)
    private val placement = RubberBandNode(this)

    val offset: Offset
        get() = Offset(resist(pull.x, placement.size.width), resist(pull.y, placement.size.height)) + settle.value

    override val isInProgress: Boolean
        get() = pull != Offset.Zero || settle.isRunning

    override val node: DelegatableNode
        get() = placement

    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset {
        // Scrolling back towards the content takes up the pull before it moves the content again.
        val relaxed = Offset(relax(pull.x, delta.x), relax(pull.y, delta.y))
        pull -= relaxed
        val remaining = delta - relaxed
        val consumed = performScroll(remaining)
        val left = remaining - consumed
        if (source == NestedScrollSource.UserInput && left != Offset.Zero) pull += left
        return relaxed + consumed + if (source == NestedScrollSource.UserInput) left else Offset.Zero
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        val layout = placement.springs().layout
        val spring = spring(layout.dampingRatio, layout.stiffness, visibilityThreshold = Offset(0.5f, 0.5f))
        if (pull != Offset.Zero) {
            // Let go while pulled: what is showing springs back from where it is.
            val shown = offset
            pull = Offset.Zero
            settle.snapTo(shown)
            settle.animateTo(Offset.Zero, spring)
            return
        }
        val left = performFling(velocity)
        if (left != Velocity.Zero) {
            settle.animateTo(Offset.Zero, spring, initialVelocity = Offset(left.x, left.y) * BOUNCE_VELOCITY)
        }
    }

    private fun relax(pulled: Float, delta: Float): Float =
        if (pulled == 0f || sign(pulled) == sign(delta)) 0f else -sign(pulled) * minOf(abs(pulled), abs(delta))

    /** The rubber band iOS uses: the further past the edge, the less the content follows. */
    private fun resist(distance: Float, dimension: Int): Float {
        if (distance == 0f || dimension == 0) return 0f
        val d = dimension.toFloat()
        return sign(distance) * (1f - 1f / (abs(distance) * RESISTANCE / d + 1f)) * d
    }
}

private class RubberBandNode(private val effect: RubberBandOverscroll) :
    Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    var size = IntSize.Zero
        private set

    fun springs() = currentValueOf(LocalAppSprings)

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        size = IntSize(placeable.width, placeable.height)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val offset = effect.offset
                translationX = offset.x
                translationY = offset.y
            }
        }
    }
}

private const val RESISTANCE = 0.55f

// A fling's leftover velocity, scaled down: the full speed would carry the content off screen.
private const val BOUNCE_VELOCITY = 0.12f
