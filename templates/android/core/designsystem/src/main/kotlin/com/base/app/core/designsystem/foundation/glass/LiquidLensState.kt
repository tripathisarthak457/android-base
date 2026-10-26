package com.base.app.core.designsystem.foundation.glass

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.layout.ScaleFactor
import com.base.app.core.designsystem.theme.AppTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first

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
    val refracts = LocalGlassQuality.current.refracts && !AppTheme.reduceMotion
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

    // The glass the last frame showed, which each frame may raise only so far (see travel).
    private var lift by mutableFloatStateOf(0f)

    // Whether the last frame left glass to see. Only the frame loop writes it, and an unchanged
    // value notifies no one, so the frames a lens still takes to settle after handing over to the
    // indicator invalidate nothing that reads only samplesBackdrop. Between frames nothing raises
    // the glass (only a frame raises lift), so a value a frame old errs only towards showing.
    private var glassShown by mutableStateOf(false)

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
     * It is read off the lens's spring, so it needs no animation of its own. The spring's energy
     * caps it, as the top speed that energy could still give the lens: √(v² + k·d²) for a speed v
     * and a distance d left to go, full from [TravelFullSpeed] on. That energy only drains while
     * the spring carries the lens to one tab, so the glass calms as the lens settles and holds
     * where the speed alone passes through zero: at the turnaround of a selection reversed in
     * flight, or of an overshoot. A new selection raises the energy at once, so the glass does
     * not follow it up: each frame raises it from what it last showed by no more than the lens
     * moved, over [LiftDistance], and no more than the frame's time, over [LiftNanos]. A tap made
     * at rest therefore starts plain and one made while the glass calms starts from that glass,
     * and either lifts over a few frames. Read it in draw or in a layer block.
     */
    val travel: Float
        get() = if (refracts) min(glassOf(energy), lift) else 0f

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
    val samplesBackdrop: Boolean
        get() = (pressed && refracts) || glassShown

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
            glassShown = intensity >= ShownThreshold
            lastFrameNanos = frameNanos
        }
    }

    private fun settle() {
        position = target
        velocity = 0f
        pressure = pressureTarget
        pressureVelocity = 0f
        lift = 0f
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
        // A lens that does not refract keeps no lift, so glass that returns mid-flight (the tier
        // rising back to Live) also rises from plain.
        val rise = min(abs(position - from) / LiftDistance, elapsedNanos.toFloat() / LiftNanos)
        lift = if (refracts) min(glassOf(energy), lift + rise) else 0f

        val pressureGoal = pressureTarget
        val pressureFrom = pressure
        val nextPressureVelocity = swellSpec.getVelocityFromNanos(
            elapsedNanos,
            pressureFrom,
            pressureGoal,
            pressureVelocity,
        )
        val nextPressure = swellSpec.getValueFromNanos(
            elapsedNanos,
            pressureFrom,
            pressureGoal,
            pressureVelocity,
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

/** The glass a spring's [energy] (in slots per second) shows, eased so that it calms with it. */
private fun glassOf(energy: Float): Float = eased(energy / TravelFullSpeed)

/**
 * Smoothstep of [amount] clamped to 0..1. Flat at both ends: the glass leaves full without a
 * jump, and the last slow stretch of a settle, where the lens has all but arrived, adds next to
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
 * How far, in slots, the lens moves while its glass rises from plain to full: a fifth of a tab. A
 * lens leaving a tab covers it slowly at first, so the glass eases in with the motion.
 */
private const val LiftDistance = 0.2f

/**
 * The shortest time over which the glass rises from plain to full: four frames of a 120 Hz
 * display. A lens sent several tabs away covers [LiftDistance] within a frame or two, and its
 * glass would pop up as it left.
 */
private const val LiftNanos = 32 * NanosPerMilli

private const val PressSwell = 0.3f
private const val TravelSwell = 0.12f
private const val StretchAlong = 0.2f
private const val StretchAcross = 0.08f
