package com.example.cmp.core.designsystem.foundation.scroll

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateTo
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
import com.example.cmp.core.designsystem.theme.AppSprings
import com.example.cmp.core.designsystem.theme.LocalAppSprings
import com.example.cmp.core.designsystem.theme.forPixelOffset
import com.example.cmp.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
public object AppOverscroll : OverscrollFactory {
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
        pull += relaxed
        val remaining = delta - relaxed
        val consumed = performScroll(remaining)
        val left = remaining - consumed
        if (source == NestedScrollSource.UserInput && left != Offset.Zero) pull += left
        return relaxed + consumed + if (source == NestedScrollSource.UserInput) left else Offset.Zero
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        // Read before the fling: the container can leave composition while it runs, and a detached
        // node has no composition locals to read.
        val springs = placement.springs()
        val minimum = placement.minimumReleaseVelocity()
        var fling = velocity
        if (pull != Offset.Zero) {
            // Let go while pulled: what is showing moves on from where it is as fast as the content
            // was under the finger, which is the finger's speed through the band.
            val shown = offset
            val moving = Offset(
                velocity.x * slope(pull.x, placement.size.width),
                velocity.y * slope(pull.y, placement.size.height),
            )
            if (abs(moving.x) < minimum && abs(moving.y) < minimum) {
                settle(from = shown, initialVelocity = moving, spec = springs.overscrollRelease.forPixelOffset())
                return
            }
            val spec = springs.bounceBack.forPixelOffset()
            val outward = moving.x * shown.x + moving.y * shown.y >= 0f
            if (outward) {
                settle(from = shown, initialVelocity = moving, spec = spec)
                return
            }
            // Thrown back towards the content: a spring started this fast swings the content past
            // its place and back, against the throw. It stops at its place instead, and the speed
            // it still has there carries the content on, as an iOS scroll view does.
            val through = settle(from = shown, initialVelocity = moving, spec = spec, stopAtRest = true)
            fling = Velocity(through.x.orNoneBelow(minimum), through.y.orNoneBelow(minimum))
            if (fling == Velocity.Zero) return
        }
        // performFling answers with the velocity the content and its parents used. What is left is
        // the speed the content met the end with, or, for a fling that stopped short of it, the
        // decay's resting speed, which carries nothing.
        val left = fling - performFling(fling)
        val carried = Offset(left.x.orNoneBelow(minimum), left.y.orNoneBelow(minimum))
        if (carried != Offset.Zero) {
            settle(from = released, initialVelocity = carried.capped(), spec = springs.bounceBack.forPixelOffset())
        }
    }

    /**
     * Springs what is showing from [from] back to rest. With [stopAtRest] it stops on reaching rest
     * rather than passing it, and answers with how fast it was still moving there; otherwise, or if
     * a finger catches it first, it answers zero.
     */
    private suspend fun settle(
        from: Offset,
        initialVelocity: Offset,
        spec: SpringSpec<Offset>,
        stopAtRest: Boolean = false,
    ): Offset {
        settling?.cancel()
        pull = Offset.Zero
        released = from
        var through = Offset.Zero
        coroutineScope {
            settling = launch {
                AnimationState(Offset.VectorConverter, from, initialVelocity).animateTo(Offset.Zero, spec) {
                    if (stopAtRest && value.x * from.x <= 0f && value.y * from.y <= 0f) {
                        released = Offset.Zero
                        through = velocity
                        cancelAnimation()
                    } else {
                        released = value
                    }
                }
            }
        }
        return through
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

    /** The part of [delta] that heads back towards the content and so eases the band; it has [delta]'s sign. */
    private fun relax(pulled: Float, delta: Float): Float =
        if (pulled == 0f || sign(pulled) == sign(delta)) 0f else -sign(pulled) * minOf(abs(pulled), abs(delta))
}

/**
 * This speed, or none below [minimum]: a fling that slows to rest by itself still reports its decay's
 * resting speed, and a band or fling set moving by it would keep the container "in progress", so the
 * next tap only stops it.
 */
private fun Float.orNoneBelow(minimum: Float): Float = if (abs(this) < minimum) 0f else this

/** A fling's leftover as it carries past the end, at most [MaxEdgeVelocity] on each axis. */
private fun Offset.capped(): Offset =
    Offset(x.coerceIn(-MaxEdgeVelocity, MaxEdgeVelocity), y.coerceIn(-MaxEdgeVelocity, MaxEdgeVelocity))

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

private class RubberBandNode(private val effect: RubberBandOverscroll) :
    Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    var size = IntSize.Zero
        private set

    fun springs(): AppSprings = currentValueOf(LocalAppSprings)

    fun minimumReleaseVelocity(): Float = with(currentValueOf(LocalDensity)) { AppOverscrollDefaults.MinimumReleaseVelocity.toPx() }

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

internal object AppOverscrollDefaults {
    /**
     * The slowest movement that counts as one, in each axis. A release slower than this is a
     * let-go, not a throw: the band decays back instead of carrying a speed in. Slower than this,
     * what is left of a fling at the end, or of a throw when the band comes back to rest, is the
     * fling's resting speed or near enough, and moves nothing further.
     */
    val MinimumReleaseVelocity = 50.dp
}

/**
 * The most of a fling's leftover speed that carries past the end, in pixels a second. Faster than
 * this would throw the content a large part of the screen away before the spring caught it.
 */
private const val MaxEdgeVelocity = 5000f
