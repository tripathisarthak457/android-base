package com.base.app.core.designsystem.foundation.glass

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.MotionDurationScale
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
    fun `tapping the tab the lens is on neither moves nor lifts it`() = runTest {
        val (state, clock) = startLens(slot = 1)

        state.select(1)

        assertNoMoreFrames(clock)
        assertEquals(1f, state.position)
        assertFalse(state.samplesBackdrop)
    }

    @Test
    fun `a tap's motion lifts the lens into glass, which calms as the lens settles`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.select(2)
        val travel = framesUntilIdle(clock) { state.travel }
        val peak = travel.indexOf(travel.max())

        assertTrue("the lens turns fully to glass on its way: $travel", travel[peak] > 0.95f)
        assertTrue(
            "and calms without a second lift: $travel",
            travel.drop(peak).zipWithNext().all { (before, after) -> after <= before },
        )
        assertEquals(0f, state.travel)
        assertEquals(0f, state.intensity)
        assertEquals(2f, state.position)
    }

    @Test
    fun `a tap samples the scene while it travels and stops before it settles`() = runTest {
        val (state, clock) = startLens(slot = 0)
        val atRest = state.samplesBackdrop

        state.select(2)
        val moving = framesUntilIdle(clock) { state.samplesBackdrop to state.position }
            .dropWhile { (_, position) -> position == 0f }
        val sampling = moving.map { it.first }
        val handOver = sampling.indexOf(false)

        assertFalse(atRest)
        assertTrue("the lens is glass from its first step: $sampling", sampling.first())
        assertTrue("in one unbroken run: $sampling", sampling.drop(handOver).none { it })
        assertTrue("which hands over before the last of the motion", moving[handOver].second < 2f)
        assertFalse(state.samplesBackdrop)
    }

    @Test
    fun `a selection reversed in flight stays glass until a single hand-over`() = runTest {
        for ((first, second) in listOf(2 to 0, 1 to 0, 2 to 1)) {
            for (reversal in 2..20) {
                val (state, clock) = startLens(slot = 0)

                state.select(first)
                val outward = List(reversal) {
                    frame(clock)
                    state.samplesBackdrop to state.position
                }
                state.select(second)
                val back = framesUntilIdle(clock) { state.samplesBackdrop to state.position }

                val moving = (outward + back).dropWhile { (_, position) -> position == 0f }
                val sampling = moving.map { it.first }
                val handOver = sampling.indexOf(false)
                val case = "to $first, back to $second after $reversal frames: $sampling"
                assertTrue("glass from the first step, $case", sampling.first())
                assertTrue("in one unbroken run, $case", sampling.drop(handOver).none { it })
                assertTrue(
                    "before the lens arrives, $case",
                    moving[handOver].second != second.toFloat(),
                )
                assertEquals(second.toFloat(), state.position)
            }
        }
    }

    @Test
    fun `a press samples the scene from the touch while the finger is down`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.press(0f)
        val onTouch = state.samplesBackdrop
        val held = framesUntilIdle(clock) { state.samplesBackdrop }

        assertTrue(onTouch)
        assertTrue("a still, held lens is glass: $held", held.all { it } && state.samplesBackdrop)
    }

    @Test
    fun `a released press samples the scene until its swell has receded`() = runTest {
        val (state, clock) = startLens(slot = 0)
        state.press(0f)
        framesUntilIdle(clock) { state.pressure }

        state.release()
        val receding = framesUntilIdle(clock) { state.pressure to state.samplesBackdrop }

        assertTrue(
            "for as long as the swell shows: $receding",
            receding.filter { (pressure, _) -> pressure >= 0.01f }.all { (_, samples) -> samples },
        )
        assertTrue(receding.first().second)
        assertFalse(state.samplesBackdrop)
        assertEquals(0f, state.intensity)
    }

    @Test
    fun `without refraction a tap moves a plain indicator`() = runTest {
        val (state, clock) = startLens(slot = 0, refracts = false)

        state.select(2)
        val tap = framesUntilIdle(clock) { state.isPlain() }

        assertTrue("$tap", tap.isNotEmpty() && tap.all { it })
        assertEquals(2f, state.position)
    }

    @Test
    fun `without refraction a drag moves a plain indicator that follows the finger`() = runTest {
        val (state, clock) = startLens(slot = 2, refracts = false)

        state.press(2f)
        state.dragBy(-1f)
        val drag = framesUntilIdle(clock) { state.isPlain() }
        val held = state.position
        state.release()
        val release = framesUntilIdle(clock) { state.isPlain() }

        assertTrue("a drag: $drag", drag.isNotEmpty() && drag.all { it })
        assertTrue("a release: $release", release.all { it })
        assertEquals("the indicator still follows the finger", 1f, held)
    }

    @Test
    fun `a lens that stops refracting under the finger lets its swell recede`() = runTest {
        val (state, clock) = startLens(slot = 0)
        state.press(0f)
        framesUntilIdle(clock) { state.pressure }

        state.refracts = false
        val receding = framesUntilIdle(clock) { state.pressure to state.samplesBackdrop }

        assertTrue(
            "over several frames, as glass: $receding",
            receding.count { (pressure, samples) -> pressure in 0.01f..0.99f && samples } >= 3,
        )
        assertTrue(state.pressed)
        assertEquals(0f, state.pressure)
        assertFalse(state.samplesBackdrop)
    }

    @Test
    fun `the lens stretches along its travel and never drops below its resting size`() = runTest {
        val (state, clock) = startLens(slot = 0)
        val resting = state.scale(Orientation.Horizontal)

        state.select(2)
        val frames = framesUntilIdle(clock) {
            Triple(
                state.travel,
                state.scale(Orientation.Horizontal),
                state.scale(Orientation.Vertical),
            )
        }
        val (_, horizontal, vertical) = frames.maxBy { it.first }

        assertEquals(1f, resting.scaleX)
        assertEquals(1f, resting.scaleY)
        assertTrue(
            "a bar's lens stretches along the bar: $horizontal",
            horizontal.scaleX > horizontal.scaleY,
        )
        assertTrue(
            "a rail's lens stretches along the rail: $vertical",
            vertical.scaleY > vertical.scaleX,
        )
        assertTrue(frames.all { (_, scale, _) -> scale.scaleX >= 1f && scale.scaleY >= 1f })
        assertEquals(1f, state.scale(Orientation.Horizontal).scaleX)
    }

    @Test
    fun `new springs take over a moving lens from where it is`() = runTest {
        val (steady, steadyClock) = startLens(slot = 0)
        steady.select(2)
        val steadyFrames = framesUntilIdle(steadyClock) { steady.position }.size

        val (state, clock) = startLens(slot = 0)
        state.select(2)
        repeat(3) { frame(clock) }
        val before = state.position
        state.setSprings(TrackSpring, spring(dampingRatio = 1f, stiffness = 2000f), PressSpring)
        frame(clock)
        val after = state.position
        val frames = 4 + framesUntilIdle(clock) { state.position }.size

        assertTrue("it carries on from where it was: $before, $after", after > before && after < 2f)
        assertTrue("on the new spring: $frames against $steadyFrames frames", frames < steadyFrames)
        assertEquals(2f, state.position)
    }

    @Test
    fun `an overshooting settle spring keeps the lens glass through its turnarounds`() = runTest {
        val bouncy = spring<Float>(dampingRatio = 0.5f, stiffness = 504f)
        val (state, clock) = startLens(slot = 0, settleSpring = bouncy)

        state.select(2)
        val moving = framesUntilIdle(clock) { state.samplesBackdrop to state.position }
            .dropWhile { (_, position) -> position == 0f }
        val sampling = moving.map { it.first }

        assertTrue("it overshoots: $moving", moving.maxOf { it.second } > 2.1f)
        assertTrue("glass from the first step: $sampling", sampling.first())
        assertTrue("in one unbroken run: $sampling", sampling.dropWhile { it }.none { it })
    }

    @Test
    fun `with animations off the lens jumps straight to the tab`() = runTest {
        val (state, clock) = startLens(slot = 0, durationScale = 0f)

        state.select(2)
        val frames = framesUntilIdle(clock) { state.position to state.samplesBackdrop }

        assertEquals(listOf(2f to false), frames)
    }

    @Test
    fun `a slower animator scale slows the lens by its factor`() = runTest {
        val (normal, normalClock) = startLens(slot = 0)
        normal.select(2)
        repeat(1 + 5) { frame(normalClock) }
        val atNormalSpeed = normal.position

        val (slow, slowClock) = startLens(slot = 0, durationScale = 2f)
        slow.select(2)
        repeat(1 + 10) { frame(slowClock) }

        assertEquals(atNormalSpeed, slow.position, Tolerance)
    }

    @Test
    fun `on a 120 Hz display the lens keeps the display's time`() = runTest {
        val (state, clock) = startLens(slot = 0, framePeriodNanos = Period120HzNanos)

        state.select(2)
        repeat(1 + 12) { frame(clock) }

        val spentMillis = 12 * Period120HzNanos / 1_000_000
        val settle = FloatSpringSpec(dampingRatio = 1f, stiffness = 504f)
        val earliest = settle.getValueFromNanos((spentMillis - 1) * 1_000_000, 0f, 2f, 0f)
        val latest = settle.getValueFromNanos(spentMillis * 1_000_000, 0f, 2f, 0f)
        assertTrue(
            "${state.position} lies within a millisecond of $spentMillis ms of motion: $latest",
            state.position in (earliest - Tolerance)..(latest + Tolerance),
        )
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
        assertFalse(state.samplesBackdrop)
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

    private fun LiquidLensState.isPlain(): Boolean {
        val scale = scale(Orientation.Horizontal)
        return intensity == 0f && !samplesBackdrop && scale.scaleX == 1f && scale.scaleY == 1f
    }

    private fun TestScope.startLens(
        slot: Int,
        refracts: Boolean = true,
        settleSpring: SpringSpec<Float> = SettleSpring,
        durationScale: Float = 1f,
        framePeriodNanos: Long = FrameMillis * 1_000_000,
    ): Pair<LiquidLensState, FrameClock> {
        val clock = FrameClock(testScheduler, framePeriodNanos)
        val state = LiquidLensState(
            initialSlot = slot,
            slotCount = 3,
            trackSpring = TrackSpring,
            settleSpring = settleSpring,
            pressSpring = PressSpring,
        )
        state.refracts = refracts
        backgroundScope.launch(clock + MotionScale(durationScale)) { state.animate() }
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
     * A display refreshing every [FrameMillis] of the test's virtual time, whose vsync timestamps
     * lie [periodNanos] apart, so it can stand in for a display of any rate. Everything waiting
     * for a frame is woken at the same vsync with the same timestamp, as Choreographer does.
     */
    private class FrameClock(
        private val scheduler: TestCoroutineScheduler,
        private val periodNanos: Long,
    ) : MonotonicFrameClock {
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
            return onFrame(time / FrameMillis * periodNanos)
        }
    }

    private class MotionScale(override val scaleFactor: Float) : MotionDurationScale

    private companion object {
        const val FrameMillis = 8L
        const val Period120HzNanos = 8_333_333L
        const val MaxFrames = 600
        const val Tolerance = 1e-4f
        val TrackSpring: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 1755f)
        val SettleSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 504f)
        val PressSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 1542f)
    }
}
