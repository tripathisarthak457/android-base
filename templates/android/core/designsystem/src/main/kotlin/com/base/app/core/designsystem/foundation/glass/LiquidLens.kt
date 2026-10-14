package com.base.app.core.designsystem.foundation.glass

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.rememberReduceMotion
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangularShape
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Creates a [LiquidLensState] for a bar of [slotCount] tabs showing [selectedSlot], and runs its
 * motion for as long as it stays in the composition.
 *
 * Every change of [selectedSlot] (a tap, the back stack, a deep link) carries the lens there on
 * [settleSpring]. At [GlassQuality.Live], with animations on, that motion turns the lens to glass
 * on its way ([LiquidLensState.travel]), and so does a finger on it. At any other tier, and when
 * the person has turned animations off, the lens is never glass: it moves, and follows a drag, as
 * a plain indicator. New springs, such as a newly picked motion style's, take over from the lens's
 * next frame without interrupting it.
 *
 * @param trackSpring the spring the lens trails a finger on.
 * @param settleSpring the spring that carries the lens to the selected tab.
 * @param pressSpring the spring a finger swells the lens on, and the swell recedes on.
 */
@Composable
internal fun rememberLiquidLensState(
    selectedSlot: Int,
    slotCount: Int,
    trackSpring: SpringSpec<Float>,
    settleSpring: SpringSpec<Float>,
    pressSpring: SpringSpec<Float>,
): LiquidLensState {
    val state = remember {
        LiquidLensState(selectedSlot, slotCount, trackSpring, settleSpring, pressSpring)
    }
    val reduceMotion = rememberReduceMotion()
    val refracts = LocalGlassQuality.current.refracts && !reduceMotion
    SideEffect {
        state.slotCount = slotCount
        state.setSprings(trackSpring, settleSpring, pressSpring)
        state.followSelection(selectedSlot)
        state.refracts = refracts
    }
    LaunchedEffect(state) { state.animate() }
    return state
}

/**
 * Where the liquid lens is along a row of tabs and how much of it is glass: the state behind the
 * glass tab bar's selection.
 *
 * Positions are in slots: 0 is the first tab, 1 the second, fractions lie between. A tap moves the
 * lens with [select] and presses nothing; the motion alone lifts it into glass ([travel]), which
 * only the lens's own layer and draw read, so the bar is never re-rendered for it. A drag
 * [press]es it: the lens swells, trails the finger on the track spring, and on [release] settles on
 * the settle spring at the tab its velocity points to. Glass, swell and stretch need [refracts];
 * without it the same motion carries a plain indicator.
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
    private var slots by mutableIntStateOf(requireSlots(slotCount))

    /** How many tabs the lens moves across. [rememberLiquidLensState] keeps it current. */
    var slotCount: Int
        get() = slots
        set(value) {
            slots = requireSlots(value)
        }

    /**
     * Whether a finger or a selection's motion turns the lens to glass: at [GlassQuality.Live]
     * with animations on, since Reduce Motion turns elastic glass off. [rememberLiquidLensState]
     * keeps it current.
     */
    var refracts by mutableStateOf(false)

    private var trackSpec = trackSpring.toFloatSpec(SlotThreshold)
    private var settleSpec = settleSpring.toFloatSpec(SlotThreshold)
    private var swellSpec = pressSpring.toFloatSpec(PressureThreshold)

    private var finger by mutableFloatStateOf(initialSlot.toFloat())
    private var restSlot by mutableIntStateOf(initialSlot)
    private var followedSlot = initialSlot
    private var pressureVelocity = 0f

    // Plain fields: they change only on a frame that writes the lens's position, or while the
    // lens is still, when its energy, and so travel, is 0 whatever they hold.
    private var departureOrigin = initialSlot.toFloat()
    private var departure = 0f

    /** True from [press] to [release] or [cancel]. */
    var pressed by mutableStateOf(false)
        private set

    /** The lens's centre, in slots. Read it in layout or draw. */
    var position by mutableFloatStateOf(initialSlot.toFloat())
        private set

    /** Slots per second. Read it in draw. */
    var velocity by mutableFloatStateOf(0f)
        private set

    /**
     * 0 at rest, 1 fully pressed: how far a finger has swollen the lens into glass. Always 0
     * unless [refracts]. Read it in draw or in a layer block.
     */
    var pressure by mutableFloatStateOf(0f)
        private set

    /**
     * 0 at rest, up to 1 while the lens moves: how far its own motion lifts it into glass. Always
     * 0 unless [refracts].
     *
     * It is read off the lens's spring, so it needs no animation of its own, and two things set
     * it. One is the spring's energy, as the top speed that energy could still give the lens:
     * √(v² + k·d²) for a speed v and a distance d left to go, full from [TravelFullSpeed] on. It
     * only drains while the spring carries the lens to one tab, so the glass calms as the lens
     * settles and holds where the speed alone passes through zero: at the turnaround of a
     * selection reversed in flight, or of an overshoot. The other is how far the lens has come
     * since its spring was last calm, as a running maximum: the glass materializes over the first
     * [LiftDistance] of the way instead of popping up in one frame, and never falls back on a
     * turnaround. A lens that has not moved yet, on the frame of a tap, is still plain. Read it
     * in draw or in a layer block.
     */
    val travel: Float
        get() = if (refracts) glassOf(energy) * eased(departure / LiftDistance) else 0f

    /**
     * How much of the lens is glass rather than the resting selection: the larger of [pressure]
     * and [travel]. Read it in draw or in a layer block.
     */
    val intensity: Float
        get() = max(pressure, travel)

    /**
     * Whether the lens shows the scene: while pressed where it [refracts], and until the glass
     * that a press or a selection's motion brought has calmed. While false the lens draws and
     * samples nothing.
     */
    val samplesBackdrop: Boolean by derivedStateOf {
        // Derived, so that the frames a lens still takes to settle after handing over to the
        // indicator invalidate nothing that reads only this.
        (pressed && refracts) || intensity >= ShownThreshold
    }

    private val lastSlot: Int
        get() = slotCount - 1

    private val target: Float
        get() = if (pressed) finger else restSlot.coerceIn(0, lastSlot).toFloat()

    private val spec: FloatSpringSpec
        get() = if (pressed) trackSpec else settleSpec

    /** The top speed, in slots per second, that the spring's energy could still give the lens. */
    private val energy: Float
        get() {
            val distance = target - position
            return sqrt(velocity * velocity + spec.stiffness * distance * distance)
        }

    // A tier that stops refracting under a finger lets the swell recede rather than vanish.
    private val pressureTarget: Float
        get() = if (pressed && refracts) 1f else 0f

    private val atRest: Boolean
        get() = position == target && velocity == 0f &&
            pressure == pressureTarget && pressureVelocity == 0f

    /**
     * Takes new springs, each in the role [rememberLiquidLensState] gives it, from the lens's next
     * frame on without interrupting it: a newly picked motion style's, for instance.
     * [rememberLiquidLensState] keeps them current.
     */
    fun setSprings(
        trackSpring: SpringSpec<Float>,
        settleSpring: SpringSpec<Float>,
        pressSpring: SpringSpec<Float>,
    ) {
        if (!trackSpec.matches(trackSpring)) trackSpec = trackSpring.toFloatSpec(SlotThreshold)
        if (!settleSpec.matches(settleSpring)) settleSpec = settleSpring.toFloatSpec(SlotThreshold)
        if (!swellSpec.matches(pressSpring)) swellSpec = pressSpring.toFloatSpec(PressureThreshold)
    }

    /** Moves the lens to [slot] without pressing it: a tap, or a selection made elsewhere. */
    fun select(slot: Int) {
        restSlot = slot.coerceIn(0, lastSlot)
    }

    /**
     * Moves the lens to [slot] when it differs from the selection this was last given.
     * [rememberLiquidLensState] passes the app's selection on every composition; an unchanged
     * one leaves the lens where a [release] sent it, so a composition that runs before the app
     * has taken up the released tab does not pull the lens back.
     */
    fun followSelection(slot: Int) {
        if (slot == followedSlot) return
        followedSlot = slot
        select(slot)
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
     * view picks its page, and that slot is returned for the bar to select. A bar whose app does
     * not take that tab up, such as a tab that opens a sheet, [select]s the app's selection again.
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

    /**
     * The lens's size against the resting selection: swollen by a press, or slightly by
     * [travel], and stretched along [orientation] as it moves, as a drop of liquid would. Never
     * below 1 on either axis, so the lens hides the resting indicator beneath it whenever it
     * shows.
     */
    fun scale(orientation: Orientation): ScaleFactor {
        val travel = travel
        val swell = 1f + max(PressSwell * pressure, TravelSwell * travel)
        val along = swell * (1f + StretchAlong * travel)
        val across = swell * (1f - StretchAcross * travel)
        return when (orientation) {
            Orientation.Horizontal -> ScaleFactor(along, across)
            Orientation.Vertical -> ScaleFactor(across, along)
        }
    }

    /**
     * Runs every motion of the lens until cancelled. Launch it once, for the bar's lifetime. It
     * follows the context's [MotionDurationScale] as Compose's animations do: with animations off
     * the lens jumps to where it is going.
     */
    suspend fun animate() {
        var lastFrameNanos = NoFrame
        var unspentNanos = 0L
        while (true) {
            if (atRest) {
                lastFrameNanos = NoFrame
                unspentNanos = 0L
                snapshotFlow { atRest }.first { !it }
                continue
            }
            val frameNanos = withFrameNanos { it }
            val durationScale = currentCoroutineContext()[MotionDurationScale]?.scaleFactor ?: 1f
            if (durationScale == 0f) {
                settle()
            } else {
                // The first frame after a rest shows where the lens starts; motion begins on the
                // next.
                if (lastFrameNanos != NoFrame) {
                    unspentNanos += ((frameNanos - lastFrameNanos) / durationScale).toLong()
                }
                // Springs step in whole milliseconds. Carrying the rest to the next frame keeps
                // a 120 Hz display, whose frames are 8.3 ms apart, from running the lens 4 % slow.
                val elapsedMillis = unspentNanos / NanosPerMilli
                unspentNanos -= elapsedMillis * NanosPerMilli
                step(elapsedMillis * NanosPerMilli)
            }
            lastFrameNanos = frameNanos
        }
    }

    private fun settle() {
        position = target
        velocity = 0f
        pressure = pressureTarget
        pressureVelocity = 0f
        departureOrigin = position
        departure = 0f
    }

    private fun step(elapsedNanos: Long) {
        val goal = target
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
        // Measured from where the spring last had too little energy to show any glass, so a tap
        // made while the lens still settles from the last one lifts it gradually again.
        if (glassOf(energy) < ShownThreshold) {
            departureOrigin = position
            departure = 0f
        } else if (departure < LiftDistance) {
            departure = max(departure, abs(position - departureOrigin))
        }

        val pressureGoal = pressureTarget
        val pressureFrom = pressure
        val nextPressureVelocity = swellSpec.getVelocityFromNanos(
            elapsedNanos, pressureFrom, pressureGoal, pressureVelocity,
        )
        val nextPressure = swellSpec.getValueFromNanos(
            elapsedNanos, pressureFrom, pressureGoal, pressureVelocity,
        )
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
 * The lens itself: the scene, plus whatever [backdrop] combines with it, magnified and bent at the
 * edge of [shape]. Only for [GlassQuality.Live].
 *
 * It shows only while [LiquidLensState.samplesBackdrop]: under a finger, and while a selection
 * carries it. The rest of the time it is skipped whole and samples nothing, and the selection is
 * the bar's drawn indicator, which the lens covers when it appears. As
 * [LiquidLensState.intensity] rises, [surface] (the resting selection's colour) fades out over
 * the sample and the sample is magnified about the lens's centre; the lens swells and stretches
 * along [orientation] through its own layer ([LiquidLensState.scale]). Nothing outside the lens's
 * layer and draw reads any of it, and the effect chain is built once: glass appears by
 * refraction, never by an alpha over a backdrop. While hidden, the layer and draw read nothing
 * but [LiquidLensState.samplesBackdrop], so a lens that has handed over is not redrawn while it
 * settles.
 *
 * Kyant's lens shader only knows rounded rectangles and throws on any other shape from Tiramisu
 * on, hence the shape's type. Build it inside `remember`, as [rememberGlassChrome] does.
 */
internal fun Modifier.liquidLens(
    backdrop: Backdrop,
    shape: RoundedRectangularShape,
    state: LiquidLensState,
    orientation: Orientation,
    surface: ColorProducer,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = LensEffects,
    highlight = null,
    shadow = null,
    layerBlock = {
        if (state.samplesBackdrop) {
            val factor = state.scale(orientation)
            scaleX = factor.scaleX
            scaleY = factor.scaleY
        } else {
            // The render thread skips a node with zero alpha outright, so a lens at rest costs no
            // offscreen pass. Only 0 or 1: anything between would composite the sample through
            // an alpha layer.
            alpha = 0f
        }
    },
    onDrawBackdrop = { drawSample ->
        if (state.samplesBackdrop) {
            // LensEffects pads nothing (the refraction band is wider than the blur), so the
            // recording's centre is the lens's.
            scale(1f + LensMagnification * state.intensity, pivot = center) { drawSample() }
        }
    },
    onDrawSurface = {
        val glass = if (state.samplesBackdrop) state.intensity else 0f
        drawRect(surface(), alpha = 1f - glass)
    },
)

/** The glass a spring's [energy] (in slots per second) shows, eased so that it calms with it. */
private fun glassOf(energy: Float): Float = eased(energy / TravelFullSpeed)

/**
 * Smoothstep of [amount] clamped to 0..1. Flat at both ends: the glass starts without a jump,
 * and the last slow stretch of a settle, where the lens has all but arrived, adds next to
 * nothing, so the glass calms with the motion instead of trailing it.
 */
private fun eased(amount: Float): Float {
    val clamped = amount.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

private fun requireSlots(count: Int): Int {
    require(count > 0) { "A lens needs at least one slot." }
    return count
}

private fun SpringSpec<Float>.toFloatSpec(threshold: Float) =
    FloatSpringSpec(dampingRatio, stiffness, threshold)

private fun FloatSpringSpec.matches(spring: SpringSpec<Float>) =
    dampingRatio == spring.dampingRatio && stiffness == spring.stiffness

private val LensEffects: BackdropEffectScope.() -> Unit = {
    vibrancy()
    blur(LensBlur.toPx())
    lens(LensRefractionHeight.toPx(), LensRefractionAmount.toPx())
}

/** Below half a pixel on a tab slot of the narrowest bar: nothing visible is left to animate. */
private const val SlotThreshold = 0.002f

/** One step of an 8-bit alpha. */
private const val PressureThreshold = 0.004f

/**
 * Below this the lens differs from the resting indicator by under 1 % of its colour and half a
 * pixel of its size, so handing over to the indicator is not seen.
 */
private const val ShownThreshold = 0.01f

/** A value is still once it would move less than its threshold in a 60 Hz frame. */
private const val StillRate = 60f

private const val NoFrame = Long.MIN_VALUE

private const val NanosPerMilli = 1_000_000L

/**
 * Where a paging scroll view projects a release: with a deceleration of 0.99 per millisecond the
 * distance left is (v / 1000) · 0.99 / 0.01, so v · 0.099 s.
 */
private const val ProjectionSeconds = 0.099f

/**
 * The spring energy, in slots per second, from which [LiquidLensState.travel] is full. A
 * selection gives the lens √k of it for each slot it has to go, k the settle spring's stiffness,
 * so on any settle spring stiffer than 36 a tap to the next tab is fully glass once lifted, and
 * the glass calms once the spring has spent all but this much.
 */
private const val TravelFullSpeed = 6f

/**
 * How far, in slots, the lens moves while its glass materializes: a fifth of a tab, the first
 * few frames of a tap.
 */
private const val LiftDistance = 0.2f

private const val PressSwell = 0.3f
private const val TravelSwell = 0.12f
private const val StretchAlong = 0.2f
private const val StretchAcross = 0.08f
private const val LensMagnification = 0.12f
private val LensBlur = 2.dp
private val LensRefractionHeight = 12.dp
private val LensRefractionAmount = 20.dp
