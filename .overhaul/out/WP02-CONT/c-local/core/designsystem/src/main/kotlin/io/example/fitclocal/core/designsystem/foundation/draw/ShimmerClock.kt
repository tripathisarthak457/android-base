package io.example.fitclocal.core.designsystem.foundation.draw

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.DpSize
import io.example.fitclocal.core.designsystem.theme.tokens.DesignTokens.Motion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The one sweep every skeleton bone in a window draws: a sweep, eased, then a pause with no frames.
 *
 * It runs only while some bone that is on screen and allowed to move has joined it, and stops when
 * the last one leaves, so an idle screen draws nothing. Each sweep frame writes [progress] as a
 * plain field and invalidates the draw of the bones that joined: nothing is snapshot state, so a
 * sweep can never recompose anything, and only the bones' own layers are redrawn.
 *
 * @param window the window's size in dp, read once per sweep: a large window sweeps longer, so the
 *   band crosses it at the speed it crosses a phone.
 */
internal class ShimmerClock(
    private val scope: CoroutineScope,
    private val window: () -> DpSize,
) {
    /** Where the band is in the sweep in flight, eased, from 0 to 1; NaN between sweeps. */
    var progress = Float.NaN
        private set

    /** How many bones each frame of a sweep redraws. */
    val boneCount: Int get() = bones.size

    private val bones = ArrayList<Bone>()
    private var job: Job? = null
    private var sweepStart = 0L
    private var sweepDuration = 0f

    // One callback for every frame of every sweep: a lambda that captured the sweep's start and
    // length would be a new object on each frame.
    private val sweepFrame: (Long) -> Boolean = { now ->
        val elapsed = (now - sweepStart) / NanosPerMilli
        progress = if (elapsed < sweepDuration) {
            Motion.skeletonEasing.transform(elapsed / sweepDuration)
        } else {
            Float.NaN
        }
        for (index in bones.indices) bones[index].onSweepFrame()
        !progress.isNaN()
    }

    fun add(bone: Bone) {
        bones += bone
        if (job == null) job = scope.launch { sweep() }
    }

    fun remove(bone: Bone) {
        bones -= bone
        if (bones.isEmpty()) {
            job?.cancel()
            job = null
            progress = Float.NaN
        }
    }

    private suspend fun sweep() {
        while (true) {
            sweepDuration = sweepMillis(window())
            // The band starts with the first frame after a bone joins, not at whatever phase a
            // previous sweep stopped at, so a bone never shows a band left over from before.
            sweepStart = withFrameNanos { it }
            var sweeping = true
            while (sweeping) sweeping = withFrameNanos(sweepFrame)
            delay(Motion.skeletonPauseMs.toLong())
        }
    }

    /** A bone the clock redraws on each frame of a sweep, and once as the band leaves. */
    fun interface Bone {
        fun onSweepFrame()
    }

    companion object {
        private const val NanosPerMilli = 1_000_000f

        /** How long one sweep takes across a window of [size]: longer on large windows, up to a limit. */
        fun sweepMillis(size: DpSize): Float {
            val travel = size.width.value * TiltCos + size.height.value * TiltSin +
                Motion.skeletonBandWidth.value
            val slowdown = (travel / Motion.skeletonReferenceTravel.value)
                .coerceIn(1f, Motion.skeletonMaxSlowdown)
            return Motion.skeletonSweepMs * slowdown
        }
    }
}

/** The window's shimmer, provided by the outermost `AppTheme`; null outside one, where bones hold still. */
internal val LocalShimmerClock = staticCompositionLocalOf<ShimmerClock?> { null }
