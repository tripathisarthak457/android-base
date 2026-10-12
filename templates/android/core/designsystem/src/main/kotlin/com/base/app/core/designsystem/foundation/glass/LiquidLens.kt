package com.base.app.core.designsystem.foundation.glass

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangularShape
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Where the liquid lens is along a row of tabs and how hard it is pressed: the state behind the
 * glass tab bar's selection.
 *
 * Positions are in slots: 0 is the first tab, 1 the second, fractions lie between. A tap moves the
 * lens with [select] and presses nothing, so the bar is not re-rendered for a press it never had.
 * A drag [press]es it: the lens swells, trails the finger on the track spring, and on [release]
 * settles on the settle spring at the tab its velocity points to.
 *
 * The motion runs in [animate], one coroutine for the state's lifetime, which integrates the lens
 * frame by frame toward whatever it currently follows. A release or a new selection only changes
 * that target, so the lens keeps its velocity through every change and never loses a frame
 * restarting an animation. The pointer handler writes plain values and starts nothing. Once every
 * value is within its threshold of its target the loop stops asking for frames until an input
 * changes, so a lens at rest draws nothing new.
 */
@Stable
internal class LiquidLensState(
    initialSlot: Int,
    slotCount: Int,
    trackSpring: SpringSpec<Float>,
    settleSpring: SpringSpec<Float>,
    pressSpring: SpringSpec<Float>,
) {
    /** How many tabs the lens moves across. The bar keeps it current. */
    var slotCount: Int = slotCount
        set(value) {
            require(value > 0) { "A lens needs at least one slot." }
            field = value
        }

    private val track = trackSpring.toFloatSpec(SlotThreshold)
    private val settle = settleSpring.toFloatSpec(SlotThreshold)
    private val swell = pressSpring.toFloatSpec(PressureThreshold)

    private var finger by mutableFloatStateOf(initialSlot.toFloat())
    private var restSlot by mutableIntStateOf(initialSlot)
    private var pressureVelocity = 0f

    /** True from [press] to [release]: the lens samples the scene only while this is true. */
    var pressed by mutableStateOf(false)
        private set

    /** The lens's centre, in slots. Read it in layout or draw. */
    var position by mutableFloatStateOf(initialSlot.toFloat())
        private set

    /** Slots per second, for the stretch along the direction of travel. Read it in draw. */
    var velocity by mutableFloatStateOf(0f)
        private set

    /** 0 at rest, 1 fully pressed. Read it in draw or in a layer block. */
    var pressure by mutableFloatStateOf(0f)
        private set

    private val lastSlot: Int
        get() = slotCount - 1

    private val target: Float
        get() = if (pressed) finger else restSlot.coerceIn(0, lastSlot).toFloat()

    private val pressureTarget: Float
        get() = if (pressed) 1f else 0f

    private val atRest: Boolean
        get() = position == target && velocity == 0f &&
            pressure == pressureTarget && pressureVelocity == 0f

    /** Moves the lens to [slot] without pressing it: a tap, or a selection made elsewhere. */
    fun select(slot: Int) {
        restSlot = slot.coerceIn(0, lastSlot)
    }

    /** A finger lands on the lens at [slot]. */
    fun press(slot: Float) {
        finger = slot.coerceIn(0f, lastSlot.toFloat())
        pressed = true
    }

    fun dragBy(slots: Float) {
        if (pressed) finger = (finger + slots).coerceIn(0f, lastSlot.toFloat())
    }

    /**
     * The finger lifts: the lens settles where its velocity projects it, the way a paging scroll
     * view picks its page, and that slot is returned for the bar to select.
     */
    fun release(): Int {
        val slot = (position + velocity * ProjectionSeconds).roundToInt().coerceIn(0, lastSlot)
        restSlot = slot
        pressed = false
        return slot
    }

    /** The gesture was taken by something else: the lens returns to the selected tab. */
    fun cancel() {
        pressed = false
    }

    /** Runs every motion of the lens until cancelled. Launch it once, for the bar's lifetime. */
    suspend fun animate() {
        var lastFrameNanos = NoFrame
        while (true) {
            if (atRest) {
                lastFrameNanos = NoFrame
                snapshotFlow { atRest }.first { !it }
                continue
            }
            val frameNanos = withFrameNanos { it }
            // The first frame after a rest shows where the lens starts; motion begins on the next.
            val elapsed = if (lastFrameNanos == NoFrame) 0L else frameNanos - lastFrameNanos
            lastFrameNanos = frameNanos
            step(elapsed)
        }
    }

    private fun step(elapsedNanos: Long) {
        val goal = target
        val spec = if (pressed) track else settle
        val from = position
        val nextVelocity = spec.getVelocityFromNanos(elapsedNanos, from, goal, velocity)
        val next = spec.getValueFromNanos(elapsedNanos, from, goal, velocity)
        if (abs(goal - next) < SlotThreshold && abs(nextVelocity) < SlotThreshold * StillRate) {
            position = goal
            velocity = 0f
        } else {
            position = next
            velocity = nextVelocity
        }

        val pressureGoal = pressureTarget
        val pressureFrom = pressure
        val nextPressureVelocity =
            swell.getVelocityFromNanos(elapsedNanos, pressureFrom, pressureGoal, pressureVelocity)
        val nextPressure =
            swell.getValueFromNanos(elapsedNanos, pressureFrom, pressureGoal, pressureVelocity)
        if (abs(pressureGoal - nextPressure) < PressureThreshold &&
            abs(nextPressureVelocity) < PressureThreshold * StillRate
        ) {
            pressure = pressureGoal
            pressureVelocity = 0f
        } else {
            pressure = nextPressure
            pressureVelocity = nextPressureVelocity
        }
    }
}

/**
 * The lens itself: the scene, plus whatever [backdrop] combines with it, bent at the edge of
 * [shape]. Only in [GlassQuality.Live], and only while [state] is pressed; at rest the selection
 * is drawn, and nothing samples the scene a second time.
 *
 * It swells with the press through a layer scale and fades [surface] (the drawn indicator's colour)
 * out as the press arrives, so the glass appears by refraction, never by an alpha over a backdrop.
 * Kyant's lens shader only knows rounded rectangles and throws on any other shape from Tiramisu on,
 * hence the shape's type. Build it inside `remember`, as [rememberGlassChrome] does.
 */
internal fun Modifier.liquidLens(
    backdrop: Backdrop,
    shape: RoundedRectangularShape,
    state: LiquidLensState,
    surface: ColorProducer,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = LensEffects,
    highlight = null,
    shadow = null,
    layerBlock = {
        val scale = 1f + (LensSwell - 1f) * state.pressure
        scaleX = scale
        scaleY = scale
    },
    onDrawSurface = { drawRect(surface(), alpha = 1f - state.pressure) },
)

private fun SpringSpec<Float>.toFloatSpec(threshold: Float) =
    FloatSpringSpec(dampingRatio, stiffness, threshold)

private val LensEffects: BackdropEffectScope.() -> Unit = {
    vibrancy()
    blur(LensBlur.toPx())
    lens(LensRefractionHeight.toPx(), LensRefractionAmount.toPx())
}

/** Below half a pixel on a tab slot of the narrowest bar: nothing visible is left to animate. */
private const val SlotThreshold = 0.002f

/** One step of an 8-bit alpha. */
private const val PressureThreshold = 0.004f

/** A value is still once it would move less than its threshold in a 60 Hz frame. */
private const val StillRate = 60f

private const val NoFrame = Long.MIN_VALUE

/**
 * Where a paging scroll view projects a release: with a deceleration of 0.99 per millisecond the
 * distance left is (v / 1000) · 0.99 / 0.01, so v · 0.099 s.
 */
private const val ProjectionSeconds = 0.099f

private const val LensSwell = 1.3f
private val LensBlur = 2.dp
private val LensRefractionHeight = 12.dp
private val LensRefractionAmount = 20.dp
