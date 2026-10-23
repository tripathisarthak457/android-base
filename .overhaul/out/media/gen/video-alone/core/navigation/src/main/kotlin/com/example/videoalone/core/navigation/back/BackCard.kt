package com.example.videoalone.core.navigation.back

import android.view.animation.AnimationUtils
import androidx.collection.MutableIntObjectMap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEvent
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The in-app predictive back of Google's own apps (Gmail): during the back gesture the screen on top
 * shrinks about its centre into a rounded card that drifts toward the far edge and follows the
 * finger's vertical movement, over the screen it returns to, which waits under a black scrim. On
 * commit the card returns to full size while it fades out, and the stack pops with no slide; on
 * cancel the system springs the progress back to 0 and the scrim fades.
 *
 * The constants are Material Components' main-container back helper and AOSP's back gesture curve,
 * checked against recordings of Gmail on a 120 Hz phone.
 */
internal object BackCard {
    const val MIN_SCALE = 0.9f
    val EdgeGap = 8.dp
    val MaxFollowY = 24.dp
    val Corner = 28.dp
    const val SCRIM_ALPHA = 0.32f
    const val SCRIM_IN_MILLIS = 50
    const val COMMIT_FADE_MILLIS = 100
    const val SCRIM_OUT_MILLIS = 120
    const val CANCEL_MILLIS = 100

    /** Progress per second; below the floor a slow release would barely move the card. */
    const val MIN_COMMIT_VELOCITY = 1f
    const val MAX_COMMIT_VELOCITY = 4f

    /** A pose this close to 0 leaves less than a pixel of the page beneath in view, on any screen. */
    const val FULL_SIZE_POSE = 0.001f

    val ProgressEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
}

/**
 * The card's state for one stack host. Every value the card draws with is snapshot state read only
 * in layer and draw blocks, so a gesture recomposes nothing.
 */
@Stable
internal class BackCardState {

    enum class Phase { Idle, Tracking, Settling }

    /**
     * Read only by the Back handler as its events arrive, never in composition, layout or drawing,
     * so it is a plain field: a change of phase invalidates nothing.
     */
    var phase = Phase.Idle
        private set

    /**
     * True from the first progress event until the card is gone, commit and cancel included. Its own
     * state, written only as the card comes and goes, so the commit and the cancel compose nothing.
     */
    var active by mutableStateOf(false)
        private set

    /** The content key of the entry that shrinks. */
    var subject: Any? by mutableStateOf(null)
        private set

    /** What the card reveals: the shown entries of the page the pop returns to. */
    var beneath: Set<Any> by mutableStateOf(emptySet())
        private set

    /** Eased gesture progress. */
    var progress by mutableFloatStateOf(0f)
        private set

    var deltaY by mutableFloatStateOf(0f)
        private set

    var fromRightEdge by mutableStateOf(false)
        private set

    /** 0 → 1 as the card returns to full size on commit. */
    val unwind = Animatable(0f)

    val contentAlpha = Animatable(1f)

    val scrim = Animatable(0f)

    /**
     * 0 → 1 as the page beneath moves from wherever a running push left it to rest under the card.
     * Over a settled stack it starts at 1: the page beneath is already at rest.
     */
    var reveal by mutableFloatStateOf(0f)
        private set

    private var startY = 0f
    private val tracker = VelocityTracker1D(isDataDifferential = false)
    private var scrimJob: Job? = null

    /** [midPush]: the page on top is still sliding in, so the page beneath is not at rest yet. */
    fun start(event: NavigationEvent, subject: Any, beneath: Set<Any>, midPush: Boolean, scope: CoroutineScope) {
        this.subject = subject
        this.beneath = beneath
        fromRightEdge = event.swipeEdge == NavigationEvent.EDGE_RIGHT
        startY = event.touchY
        tracker.resetTracking()
        phase = Phase.Tracking
        active = true
        reveal = if (midPush) 0f else 1f
        track(event)
        scrimJob = scope.launch {
            if (midPush) launch { animate(0f, 1f, animationSpec = tween(BackCard.SCRIM_IN_MILLIS, easing = LinearEasing)) { value, _ -> reveal = value } }
            scrim.animateTo(BackCard.SCRIM_ALPHA, tween(BackCard.SCRIM_IN_MILLIS, easing = LinearEasing))
        }
    }

    fun track(event: NavigationEvent) {
        progress = BackCard.ProgressEasing.transform(event.progress)
        deltaY = event.touchY - startY
        // Before API 36 the platform passes no frame time. The animation clock then reads the frame
        // the platform's progress spring is ticking in, so the release velocity is still measured.
        val frameMillis = event.frameTimeMillis.takeIf { it > 0L } ?: AnimationUtils.currentAnimationTimeMillis()
        tracker.addDataPoint(frameMillis, event.progress)
    }

    /** Progress per second at the release, within what the commit's spring may be seeded with. */
    fun releaseVelocity(): Float =
        tracker.calculateVelocity().coerceIn(BackCard.MIN_COMMIT_VELOCITY, BackCard.MAX_COMMIT_VELOCITY)

    /**
     * Brings the card back to full size, seeded with the release velocity, and fades it out. Returns
     * once the card is invisible; the scrim keeps fading over the screen beneath, which the caller
     * pops to.
     *
     * Timed from [releasedNanos], when the finger lifted ([System.nanoTime]): the first frame drawn
     * after the release is already that far into the commit, rather than the release pose again.
     */
    suspend fun commit(spring: SpringSpec<Float>, releasedNanos: Long, scope: CoroutineScope) = coroutineScope {
        phase = Phase.Settling
        val velocity = releaseVelocity()
        val frames = coroutineContext[MonotonicFrameClock]
        val fromRelease = { frames?.let { FromRelease(releasedNanos, it) } ?: EmptyCoroutineContext }
        // Undispatched, so all three are running before the next frame starts.
        val unwinding = launch(fromRelease(), CoroutineStart.UNDISPATCHED) {
            unwind.animateTo(1f, spring, initialVelocity = velocity)
        }
        scrimJob?.cancel()
        scrimJob = scope.launch(fromRelease(), CoroutineStart.UNDISPATCHED) {
            scrim.animateTo(0f, tween(BackCard.SCRIM_OUT_MILLIS, easing = LinearEasing))
        }
        withContext(fromRelease()) {
            contentAlpha.animateTo(0f, tween(BackCard.COMMIT_FADE_MILLIS, easing = LinearEasing))
        }
        unwinding.cancel()
    }

    /**
     * The system has already sprung the progress back to 0; the scrim follows, and the page beneath
     * returns to where a push still running puts it, then parks. [fullWidth]: the card is as wide as
     * its host.
     */
    suspend fun cancel(midPush: Boolean, fullWidth: Boolean) = coroutineScope {
        phase = Phase.Settling
        scrimJob?.cancel()
        // Back at full size over a settled stack, the card hides everything its scrim dims: fading
        // the scrim out would draw frames that show nothing new, and hold the card's texture and the
        // high frame rate through them.
        if (!midPush && fullWidth && pose < BackCard.FULL_SIZE_POSE) {
            reset()
            return@coroutineScope
        }
        if (midPush) launch { animate(reveal, 0f, animationSpec = tween(BackCard.CANCEL_MILLIS, easing = LinearEasing)) { value, _ -> reveal = value } }
        scrim.animateTo(0f, tween(BackCard.CANCEL_MILLIS, easing = LinearEasing))
        reset()
    }

    suspend fun finish() {
        scrimJob?.join()
        reset()
    }

    private suspend fun reset() {
        phase = Phase.Idle
        active = false
        subject = null
        beneath = emptySet()
        progress = 0f
        deltaY = 0f
        reveal = 0f
        unwind.snapTo(0f)
        contentAlpha.snapTo(1f)
        scrim.snapTo(0f)
    }

    /**
     * Whether [key]'s screen is drawn from a texture of its own: the card's subject, from the
     * gesture's first frame until the card is gone. It is rendered once, on that first frame, and
     * the card's pose above it moves, scales and fades it.
     *
     * The one exception to A11.3 rule 1 (no whole screen offscreen), REQUIREMENTS 42: while the card
     * is up, the screen under it is drawn from its own innermost layer as a hardware layer
     * ([CompositingStrategy.Offscreen], chosen by the stack and cover hosts from this), from the
     * gesture's first frame until the card is gone, and released then (Google's "hardware layer for
     * the animation's duration"). It is rendered once per gesture while the screen's own content is
     * still. The unit tests guard the two conditions for that: `SlideRecordingTest` checks that the
     * texture is the screen's innermost layer for exactly the card's span (`PresentationCardTest`
     * checks a cover only partly: no texture before the gesture, one after two progress events, none
     * after a cancel), and that the screen is not recorded again between the start and the pop, so
     * that layer never changes while the card is up. That HWUI then renders it once is shown only by
     * a trace: one `drawLayer` per gesture.
     */
    fun textures(key: Any): Boolean = active && key == subject

    /** How far the card is shrunk, 0 to 1: the eased progress, unwound on commit. */
    private val pose: Float get() = progress * (1f - unwind.value)

    /** The card's scale about its centre. */
    val scale: Float get() = 1f - (1f - BackCard.MIN_SCALE) * pose

    /** Toward the far edge until [BackCard.EdgeGap] remains, for a card [width] px wide. */
    fun Density.shiftX(width: Float): Float {
        val towards = if (fromRightEdge) -1f else 1f
        return towards * max(0f, width * (1f - BackCard.MIN_SCALE) / 2f - BackCard.EdgeGap.toPx()) * pose
    }

    /** After the finger's vertical drift, up to [BackCard.MaxFollowY] and never off the screen. */
    fun Density.shiftY(height: Float): Float {
        val room = max(0f, height * (1f - scale) / 2f - BackCard.EdgeGap.toPx())
        return sign(deltaY) * min(BackCard.MaxFollowY.toPx(), room) * min(1f, abs(deltaY) / height) * (1f - unwind.value)
    }

    /** The corner radius in the card's own (unscaled) px. */
    fun Density.corner(): Float = BackCard.Corner.toPx() * pose

    /** The card's pose on the subject's layer, on top of the slide the layer already has. */
    fun GraphicsLayerScope.applyCard() {
        val scale = scale
        scaleX = scale
        scaleY = scale
        translationX += shiftX(size.width)
        translationY = shiftY(size.height)
        val alpha = contentAlpha.value
        if (alpha < 1f) {
            this.alpha = alpha
            // What this layer draws is the screen's texture ([textures]), one draw that overlaps
            // nothing, so fading each of its draws fades the screen as one, with no offscreen pass.
            compositingStrategy = CompositingStrategy.ModulateAlpha
        }
        val corner = corner().roundToInt()
        if (corner > 0) {
            shape = CardShapes.of(corner)
            clip = true
        }
    }
}

/**
 * The commit of [BackCardState.commit] on a host's stack: the card fades while [gate] holds Back,
 * then the stack pops with no slide, and the presses counted meanwhile are applied after it.
 */
internal suspend fun BackCardState.commitPop(
    gate: BackGate,
    spring: SpringSpec<Float>,
    pop: (Int) -> Int,
    releasedNanos: Long,
    scope: CoroutineScope,
) {
    gate.hold {
        commit(spring, releasedNanos, scope)
        if (pop(1) > 0) gate.changed(StackChange.GesturePop)
    }
    finish()
}

/**
 * The composition's frame clock, seen from [releasedNanos], a moment already past: the first frame
 * an animation asks for is that moment itself, and every later one is the composition's. Started
 * on it, an animation is as far along on its first drawn frame as the time since then.
 *
 * Choreographer's frame times and [System.nanoTime] are one clock. A first frame from before the
 * moment, or further from it than [MAX_LEAD_NANOS], is not on that clock (a test's virtual one) or
 * not a frame that follows it: the animation then starts at that frame, as it would without this
 * clock.
 */
private class FromRelease(private val releasedNanos: Long, private val frames: MonotonicFrameClock) : MonotonicFrameClock {
    private var started = false
    private var aligned = false
    private var shift = 0L

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        if (!started) {
            started = true
            return onFrame(releasedNanos)
        }
        return frames.withFrameNanos { frameNanos ->
            if (!aligned) {
                aligned = true
                val lead = frameNanos - releasedNanos
                if (lead !in 0L..MAX_LEAD_NANOS) shift = lead
            }
            onFrame(frameNanos - shift)
        }
    }

    private companion object {
        const val MAX_LEAD_NANOS = 50_000_000L
    }
}

/** One shape per whole-pixel radius, so a gesture allocates none per frame. */
private object CardShapes {
    private val shapes = MutableIntObjectMap<Shape>()

    fun of(px: Int): Shape = shapes.getOrPut(px) { RoundedCornerShape(px.toFloat()) }
}
