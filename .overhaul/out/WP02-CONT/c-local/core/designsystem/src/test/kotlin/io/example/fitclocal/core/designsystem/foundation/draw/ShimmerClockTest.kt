package io.example.fitclocal.core.designsystem.foundation.draw

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.example.fitclocal.core.designsystem.theme.tokens.DesignTokens.Motion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sweep's timing on a frame clock that counts the frames it is asked for: a sweep, then a pause
 * that asks for none, and nothing at all once no bone is left.
 */
class ShimmerClockTest {

    private val phone = DpSize(393.dp, 873.dp)

    @Test
    fun `a bone that joins sees no band until the sweep has started`() = runTest {
        val (clock, frames) = shimmer(phone)
        val bone = CountingBone()
        clock.add(bone)
        runCurrent()
        advanceTimeBy(FrameMillis)
        runCurrent()

        assertEquals(1, frames.count)
        assertTrue(clock.progress.isNaN())
        assertEquals(0, bone.frames)
        clock.remove(bone)
    }

    @Test
    fun `a sweep eases from 0 to 1 over its duration, then rests without asking for frames`() = runTest {
        val (clock, frames) = shimmer(phone)
        val bone = CountingBone()
        clock.add(bone)
        val duration = ShimmerClock.sweepMillis(phone)
        val seen = mutableListOf<Float>()
        repeat((duration / FrameMillis).toInt() + 3) {
            advanceTimeBy(FrameMillis)
            runCurrent()
            if (!clock.progress.isNaN()) seen += clock.progress
        }

        assertTrue("the band never moved", seen.size > 10)
        assertTrue("progress went backwards: $seen", seen.zipWithNext().all { (a, b) -> b >= a })
        assertTrue("it started at ${seen.first()}", seen.first() < 0.01f)
        assertTrue("it ended at ${seen.last()}", seen.last() > 0.99f)
        // Eased: slow at both ends, so the first tenth of the time covers well under a tenth of the way.
        val early = seen[(seen.size * 0.1f).toInt()]
        assertTrue("not eased in: $early", early < 0.05f)
        assertTrue("the sweep did not end", clock.progress.isNaN())

        val atRest = frames.count
        val redrawn = bone.frames
        advanceTimeBy(Motion.skeletonPauseMs.toLong() - FrameMillis)
        runCurrent()
        assertEquals("frames asked for during the pause", atRest, frames.count)
        assertEquals("bones redrawn during the pause", redrawn, bone.frames)

        advanceTimeBy(FrameMillis * 4)
        runCurrent()
        assertTrue("the next sweep did not start", frames.count > atRest)
        clock.remove(bone)
    }

    @Test
    fun `removing the last bone stops the clock`() = runTest {
        val (clock, frames) = shimmer(phone)
        val first = CountingBone()
        val second = CountingBone()
        clock.add(first)
        clock.add(second)
        repeat(20) {
            advanceTimeBy(FrameMillis)
            runCurrent()
        }
        clock.remove(first)
        val withOne = frames.count
        advanceTimeBy(FrameMillis * 5)
        runCurrent()
        assertTrue("one bone left still sweeps", frames.count > withOne)

        clock.remove(second)
        val stopped = frames.count
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(stopped, frames.count)
        assertTrue(clock.progress.isNaN())
    }

    @Test
    fun `a large window sweeps longer, so the band keeps its speed, up to a limit`() {
        val phoneSweep = ShimmerClock.sweepMillis(phone)
        assertEquals(Motion.skeletonSweepMs.toFloat(), phoneSweep, Motion.skeletonSweepMs * 0.02f)

        val tablet = ShimmerClock.sweepMillis(DpSize(1280.dp, 800.dp))
        val travel = 1280 * TiltCos + 800 * TiltSin + Motion.skeletonBandWidth.value
        assertEquals(Motion.skeletonSweepMs * travel / Motion.skeletonReferenceTravel.value, tablet, 1f)

        val huge = ShimmerClock.sweepMillis(DpSize(4000.dp, 3000.dp))
        assertEquals(Motion.skeletonSweepMs * Motion.skeletonMaxSlowdown, huge, 0.01f)
    }

    private fun TestScope.shimmer(window: DpSize): Pair<ShimmerClock, CountingFrameClock> {
        val frames = CountingFrameClock(testScheduler)
        return ShimmerClock(CoroutineScope(backgroundScope.coroutineContext + frames)) { window } to frames
    }

    /** A vsync every [FrameMillis] of virtual time, counting how often a frame was asked for. */
    private class CountingFrameClock(private val scheduler: TestCoroutineScheduler) : MonotonicFrameClock {
        var count = 0
            private set

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            delay(FrameMillis)
            count++
            return onFrame(scheduler.currentTime * 1_000_000)
        }
    }

    private class CountingBone : ShimmerClock.Bone {
        var frames = 0
            private set

        override fun onSweepFrame() {
            frames++
        }
    }

    private companion object {
        const val FrameMillis = 8L
    }
}
