package com.base.app.core.navigation.back

import androidx.collection.MutableIntObjectMap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

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

    val ProgressEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
}

/**
 * The card's state for one stack host. Every value the card draws with is snapshot state read only
 * in layer and draw blocks, so a gesture recomposes nothing.
 */
@Stable
internal class BackCardState {

    enum class Phase { Idle, Tracking, Settling }

    var phase by mutableStateOf(Phase.Idle)
        private set

    /** True from the first progress event until the card is gone, commit and cancel included. */
    val active: Boolean get() = phase != Phase.Idle

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
        tracker.addDataPoint(event.frameTimeMillis, event.progress)
    }

    /**
     * Brings the card back to full size, seeded with the release velocity, and fades it out. Returns
     * once the card is invisible; the scrim keeps fading over the screen beneath, which the caller
     * pops to.
     */
    suspend fun commit(spring: SpringSpec<Float>, scope: CoroutineScope) = coroutineScope {
        phase = Phase.Settling
        val velocity = tracker.calculateVelocity()
            .coerceIn(BackCard.MIN_COMMIT_VELOCITY, BackCard.MAX_COMMIT_VELOCITY)
        val unwinding = launch { unwind.animateTo(1f, spring, initialVelocity = velocity) }
        scrimJob?.cancel()
        scrimJob = scope.launch {
            scrim.animateTo(0f, tween(BackCard.SCRIM_OUT_MILLIS, easing = LinearEasing))
        }
        contentAlpha.animateTo(0f, tween(BackCard.COMMIT_FADE_MILLIS, easing = LinearEasing))
        unwinding.cancel()
    }

    /**
     * The system has already sprung the progress back to 0; the scrim follows, and the page beneath
     * returns to where a push still running puts it, then parks.
     */
    suspend fun cancel(midPush: Boolean) = coroutineScope {
        phase = Phase.Settling
        scrimJob?.cancel()
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
        subject = null
        beneath = emptySet()
        progress = 0f
        deltaY = 0f
        reveal = 0f
        unwind.snapTo(0f)
        contentAlpha.snapTo(1f)
        scrim.snapTo(0f)
    }

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
            // A 100 ms fade of a whole screen without an offscreen buffer: each draw is faded on its
            // own, which overlapping draws inside an opaque screen cannot show.
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
    scope: CoroutineScope,
) {
    gate.hold {
        commit(spring, scope)
        if (pop(1) > 0) gate.changed(StackChange.GesturePop)
    }
    finish()
}

/** One shape per whole-pixel radius, so a gesture allocates none per frame. */
private object CardShapes {
    private val shapes = MutableIntObjectMap<Shape>()

    fun of(px: Int): Shape = shapes.getOrPut(px) { RoundedCornerShape(px.toFloat()) }
}
