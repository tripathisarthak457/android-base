package com.base.app.core.designsystem.foundation.glass

import androidx.compose.animation.core.spring
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class LiquidLensStateTest {

    @Test
    fun `a tap glides the lens to the tab without pressing it, then stops drawing`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.select(2)
        val pressures = framesUntilIdle(clock) { state.pressure }

        assertEquals(2f, state.position)
        assertTrue(pressures.all { it == 0f })
        assertNoMoreFrames(clock)
    }

    @Test
    fun `a press swells the lens and it trails the finger across the bar`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.press(0f)
        state.dragBy(1f)
        framesUntilIdle(clock) { state.position }

        assertTrue(state.pressed)
        assertEquals(1f, state.pressure)
        assertEquals(1f, state.position)
        assertNoMoreFrames(clock)
    }

    @Test
    fun `the finger cannot drag the lens past either end`() = runTest {
        val (state, clock) = startLens(slot = 1)

        state.press(1f)
        state.dragBy(-5f)
        framesUntilIdle(clock) { state.position }
        val start = state.position
        state.dragBy(9f)
        framesUntilIdle(clock) { state.position }

        assertEquals(0f, start)
        assertEquals(2f, state.position)
    }

    @Test
    fun `a release settles on the tab the velocity points to, carrying the velocity`() = runTest {
        val (state, clock) = startLens(slot = 0)
        state.press(0f)
        state.dragBy(2f)
        repeat(4) { frame(clock) }
        val position = state.position
        val velocity = state.velocity

        val target = state.release()
        frame(clock)
        val firstSettleFrame = state.position
        framesUntilIdle(clock) { state.position }

        assertTrue(velocity > 0f)
        assertEquals((position + velocity * 0.099f).roundToInt().coerceIn(0, 2), target)
        assertTrue("the lens keeps moving after the finger lifts", firstSettleFrame > position)
        assertEquals(target.toFloat(), state.position)
        assertEquals(0f, state.pressure)
        assertFalse(state.pressed)
        assertNoMoreFrames(clock)
    }

    @Test
    fun `a slow release lands on the nearest tab`() = runTest {
        val (state, clock) = startLens(slot = 0)
        state.press(0f)
        state.dragBy(1.4f)
        framesUntilIdle(clock) { state.position }

        assertEquals(1, state.release())
    }

    @Test
    fun `a cancelled drag returns to the selected tab`() = runTest {
        val (state, clock) = startLens(slot = 1)
        state.press(1f)
        state.dragBy(0.8f)
        repeat(6) { frame(clock) }

        state.cancel()
        framesUntilIdle(clock) { state.position }

        assertEquals(1f, state.position)
        assertEquals(0f, state.pressure)
    }

    private fun TestScope.startLens(slot: Int): Pair<LiquidLensState, FrameClock> {
        val clock = FrameClock(testScheduler)
        val state = LiquidLensState(
            initialSlot = slot,
            slotCount = 3,
            trackSpring = spring(dampingRatio = 0.86f, stiffness = 1755f),
            settleSpring = spring(dampingRatio = 1f, stiffness = 504f),
            pressSpring = spring(dampingRatio = 1f, stiffness = 1542f),
        )
        backgroundScope.launch(clock) { state.animate() }
        runCurrent()
        Snapshot.sendApplyNotifications()
        return state to clock
    }

    private fun TestScope.frame(clock: FrameClock) {
        val before = clock.frames
        advance()
        check(clock.frames > before) { "No frame was requested." }
    }

    /** Runs frames until nothing asks for another, recording [sample] after each. */
    private fun <T> TestScope.framesUntilIdle(clock: FrameClock, sample: () -> T): List<T> {
        val samples = mutableListOf<T>()
        repeat(MaxFrames) {
            val before = clock.frames
            advance()
            if (clock.frames == before) return samples
            samples += sample()
        }
        error("Still animating after $MaxFrames frames: a visibility threshold is missing.")
    }

    private fun TestScope.assertNoMoreFrames(clock: FrameClock) {
        val before = clock.frames
        repeat(30) { advance() }
        assertEquals("frames requested at rest", before, clock.frames)
    }

    /**
     * One frame of virtual time. Outside a composition nothing sends snapshot apply
     * notifications, which the lens's snapshotFlow waits for, so the frame sends them first, as
     * the frame clock does.
     */
    private fun TestScope.advance() {
        Snapshot.sendApplyNotifications()
        advanceTimeBy(FrameMillis)
        runCurrent()
    }

    /**
     * A display refreshing every [FrameMillis] of the test's virtual time. Everything waiting for a
     * frame is woken at the same vsync with the same timestamp, as Choreographer does.
     */
    private class FrameClock(private val scheduler: TestCoroutineScheduler) : MonotonicFrameClock {
        var frames = 0
            private set
        private var lastFrame = -1L

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            delay(FrameMillis - scheduler.currentTime % FrameMillis)
            val time = scheduler.currentTime
            if (time != lastFrame) {
                lastFrame = time
                frames++
            }
            return onFrame(time * 1_000_000)
        }
    }

    private companion object {
        const val FrameMillis = 8L
        const val MaxFrames = 600
    }
}
