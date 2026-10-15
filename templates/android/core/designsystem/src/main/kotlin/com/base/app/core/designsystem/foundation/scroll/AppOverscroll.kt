package com.base.app.core.designsystem.foundation.scroll

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppSpring
import com.base.app.core.designsystem.theme.AppSprings
import com.base.app.core.designsystem.theme.LocalAppSprings
import com.base.app.core.designsystem.theme.tokens.DesignTokens
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * The iOS edge, which `AppTheme` installs for every scrolling container: content pulled past its
 * end follows the finger with growing resistance and springs back when let go, at the speed it
 * was moving; a fling that reaches the end carries on past it and is caught.
 *
 * A screen that wants Android's stretch instead provides the platform's own factory below it:
 *
 * ```
 * CompositionLocalProvider(LocalOverscrollFactory provides rememberPlatformOverscrollFactory()) { … }
 * ```
 *
 * The offset is applied when the content is placed, so a pull moves the list's layer and recomposes,
 * re-measures and redraws nothing.
 */
object AppOverscroll : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = RubberBandOverscroll()

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 3
}

private class RubberBandOverscroll : OverscrollEffect {
    /** How far the finger has gone past the edge; what is drawn is this with resistance applied. */
    private var pull by mutableStateOf(Offset.Zero)

    /** What is left of a release or a caught fling while it springs back; added to the pull. */
    private var released by mutableStateOf(Offset.Zero)
    private var settling: Job? = null
    private val placement = RubberBandNode(this)

    val offset: Offset
        get() = Offset(resist(pull.x, placement.size.width), resist(pull.y, placement.size.height)) + released

    override val isInProgress: Boolean
        get() = pull != Offset.Zero || released != Offset.Zero

    override val node: DelegatableNode
        get() = placement

    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset {
        catchRelease()
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
        val springs = placement.springs()
        if (pull != Offset.Zero) {
            // Let go while pulled: what is showing springs back from where it is, moving as fast as
            // the content was under the finger, which is the finger's speed through the band.
            val moving = Offset(
                velocity.x * slope(pull.x, placement.size.width),
                velocity.y * slope(pull.y, placement.size.height),
            )
            val spec = if (moving.getDistance() < placement.minimumReleaseVelocity()) {
                springs.overscrollRelease.forOffset()
            } else {
                springs.bounceBack.offsetSpec()
            }
            settle(from = offset, velocity = moving, spec = spec)
            return
        }
        val left = performFling(velocity)
        if (left != Velocity.Zero) {
            val carried = Offset(
                left.x.coerceIn(-MaxEdgeVelocity, MaxEdgeVelocity),
                left.y.coerceIn(-MaxEdgeVelocity, MaxEdgeVelocity),
            )
            settle(from = released, velocity = carried, spec = springs.bounceBack.offsetSpec())
        }
    }

    private suspend fun settle(from: Offset, velocity: Offset, spec: SpringSpec<Offset>) {
        settling?.cancel()
        pull = Offset.Zero
        released = from
        coroutineScope {
            settling = launch {
                animate(Offset.VectorConverter, from, Offset.Zero, velocity, spec) { value, _ -> released = value }
            }
        }
    }

    /** A finger landing on content still springing back takes it from where it is. */
    private fun catchRelease() {
        if (released == Offset.Zero) return
        settling?.cancel()
        settling = null
        val shown = offset
        released = Offset.Zero
        pull = Offset(unresist(shown.x, placement.size.width), unresist(shown.y, placement.size.height))
    }

    private fun relax(pulled: Float, delta: Float): Float =
        if (pulled == 0f || sign(pulled) == sign(delta)) 0f else -sign(pulled) * minOf(abs(pulled), abs(delta))
}

/** The rubber band iOS uses: the further past the edge, the less the content follows. */
private fun resist(distance: Float, dimension: Int): Float {
    if (distance == 0f || dimension == 0) return 0f
    val d = dimension.toFloat()
    return sign(distance) * (1f - 1f / (abs(distance) * Resistance / d + 1f)) * d
}

private fun unresist(shown: Float, dimension: Int): Float {
    if (shown == 0f || dimension == 0) return 0f
    val d = dimension.toFloat()
    val y = abs(shown).coerceAtMost(d * 0.999f)
    return sign(shown) * y / (Resistance * (1f - y / d))
}

/** How far the content moves per pixel of finger travel at this pull: the band's slope. */
private fun slope(distance: Float, dimension: Int): Float {
    if (dimension == 0) return Resistance
    val stretch = abs(distance) * Resistance / dimension + 1f
    return Resistance / (stretch * stretch)
}

private fun SpringSpec<Float>.forOffset(): SpringSpec<Offset> = spring(dampingRatio, stiffness, OffsetThreshold)

private fun AppSpring.offsetSpec(): SpringSpec<Offset> =
    spring(dampingRatio, stiffness, OffsetThreshold)

private class RubberBandNode(private val effect: RubberBandOverscroll) :
    Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    var size = IntSize.Zero
        private set

    fun springs(): AppSprings = currentValueOf(LocalAppSprings)

    fun minimumReleaseVelocity(): Float = with(currentValueOf(LocalDensity)) { MinimumReleaseVelocity.toPx() }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        size = IntSize(placeable.width, placeable.height)
        return layout(placeable.width, placeable.height) {
            // Moved by placement rather than by the layer's translation: a moved layer is what makes
            // Compose ask an adaptive-refresh display for its high rate while the edge springs back.
            val offset = effect.offset
            placeable.placeWithLayer(offset.x.roundToInt(), offset.y.roundToInt())
        }
    }
}

private val Resistance = DesignTokens.Motion.rubberBand
private val OffsetThreshold = Offset(0.5f, 0.5f)

/** Below this a release is a let-go, not a throw: it decays back instead of carrying a speed in. */
private val MinimumReleaseVelocity = 50.dp

/**
 * The most of a fling's leftover speed that carries past the end, in pixels a second. Faster than
 * this would throw the content a large part of the screen away before the spring caught it.
 */
private const val MaxEdgeVelocity = 5000f
