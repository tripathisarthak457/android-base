package com.example.finalafter.crui3.core.designsystem.foundation.glass

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.layout.ScaleFactor
import kotlin.math.roundToInt
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

class LiquidLensStateTest {

    @Test
    fun `a tap glides the lens to the tab without pressing it`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.select(2)
        val pressures = framesUntilIdle(clock) { state.pressure }

        assertEquals(2f, state.position)
        assertTrue(pressures.all { it == 0f })
    }

    @Test
    fun `a lens that has arrived asks for no more frames`() = runTest {
        val (state, clock) = startLens(slot = 0)
        state.select(2)

        framesUntilIdle(clock) { state.position }

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
    fun `a tap's motion lifts the lens to full glass`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.select(2)
        val travel = framesUntilIdle(clock) { state.travel }

        assertTrue("$travel", travel.max() > FullGlass)
    }

    @Test
    fun `a tap's glass calms once and is gone when the lens has settled`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.select(2)
        val travel = framesUntilIdle(clock) { state.travel }

        assertTrue("without a second lift: $travel", liftsThenCalmsOnce(travel))
        assertEquals(0f, state.intensity)
    }

    @Test
    fun `a tap's glass materializes over the lens's first frames`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.select(1)
        val onTap = state.travel
        val travel = framesUntilIdle(clock) { state.travel }
        val lift = travel.takeWhile { it < FullGlass }

        assertEquals("the frame of the tap is still plain", 0f, onTap)
        assertTrue("it rises through several frames: $travel", lift.count { it > 0f } >= 3)
        assertTrue("without falling back: $lift", lift.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun `a tap's glass eases in as the lens leaves its tab`() = runTest {
        val travel = travelOfTap(to = 1)
        val rises = travel.zipWithNext { before, after -> after - before }.filter { it > 0f }

        assertTrue("the first rise is the gentlest by far: $rises", rises.first() < rises.max() / 2)
    }

    @Test
    fun `a tap two tabs away lifts its glass no faster than a tap to the next tab`() = runTest {
        val nextTab = largestRise(travelOfTap(to = 1))

        val twoTabs = largestRise(travelOfTap(to = 2))

        assertTrue("$twoTabs against $nextTab", twoTabs <= nextTab + Tolerance)
    }

    @Test
    fun `a new selection never drops the glass the lens shows`() = runTest {
        for ((first, second) in NewSelections) {
            for (frames in 1..framesToHandOver(first)) {
                val (state, clock) = startLens(slot = 0)
                state.select(first)
                repeat(frames) { frame(clock) }
                val before = state.travel

                state.select(second)
                val after = state.travel
                val rest = framesUntilIdle(clock) { state.travel }

                val case = "to $first, then $second after $frames frames: $before, $after, $rest"
                assertTrue("at the selection, $case", after >= before)
                assertTrue("after it, $case", liftsThenCalmsOnce(listOf(after) + rest))
            }
        }
    }

    @Test
    fun `a new selection lifts the glass no faster than a tap from rest`() = runTest {
        val fromRest = largestRise(travelOfTap(to = 1))
        for ((first, second) in NewSelections) {
            for (frames in 1..framesToHandOver(first)) {
                val (state, clock) = startLens(slot = 0)
                state.select(first)
                repeat(frames) { frame(clock) }
                val before = state.travel

                state.select(second)
                val travel = listOf(before, state.travel) + framesUntilIdle(clock) { state.travel }

                val rise = largestRise(travel)
                val case = "to $first, then $second after $frames frames: $travel"
                assertTrue("$rise against $fromRest, $case", rise <= fromRest + Tolerance)
            }
        }
    }

    @Test
    fun `a tap made while the lens still settles lifts it gradually again`() = runTest {
        val (state, clock) = startLens(slot = 0)
        state.select(1)
        framesUntil(clock) { state.samplesBackdrop }
        framesUntil(clock) { !state.samplesBackdrop }
        val settling = state.velocity

        state.select(2)
        val travel = List(3) {
            frame(clock)
            state.travel
        }

        assertTrue("the lens had not come to rest: $settling", settling != 0f)
        assertTrue("$travel", travel.first() < 0.5f && travel.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun `a lens that jumped with animations off lifts gradually on its next tap`() = runTest {
        val motion = MotionScale(1f)
        val (state, clock) = startLens(slot = 0, motion = motion)
        state.select(2)
        framesUntil(clock) { state.travel > FullGlass }
        motion.scaleFactor = 0f
        frame(clock)
        motion.scaleFactor = 1f

        state.select(0)
        val travel = List(3) {
            frame(clock)
            state.travel
        }

        assertTrue("$travel", travel.first() < 0.5f && travel.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun `a tap samples the scene in one unbroken run`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.select(2)
        val sampling = framesUntilIdle(clock) { state.samplesBackdrop }

        assertTrue("$sampling", isOneRun(sampling))
    }

    @Test
    fun `a tap's lens hands over to the indicator before the lens arrives`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.select(2)
        val moving = framesUntilIdle(clock) { state.samplesBackdrop to state.position }

        assertTrue("$moving", moving[handOverIn(moving.map { it.first })].second < 2f)
    }

    @Test
    fun `a selection reversed in flight samples the scene until a single hand-over`() = runTest {
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

                val moving = outward + back
                val sampling = moving.map { it.first }
                val case = "to $first, back to $second after $reversal frames: $sampling"
                assertTrue("in one unbroken run, $case", isOneRun(sampling))
                assertTrue(
                    "before the lens arrives, $case",
                    moving[handOverIn(sampling)].second != second.toFloat(),
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
    fun `without refraction a drag moves a plain indicator`() = runTest {
        val (state, clock) = startLens(slot = 2, refracts = false)

        state.press(2f)
        state.dragBy(-1f)
        val drag = framesUntilIdle(clock) { state.isPlain() }
        state.release()
        val release = framesUntilIdle(clock) { state.isPlain() }

        assertTrue("$drag, then $release", drag.isNotEmpty() && (drag + release).all { it })
    }

    @Test
    fun `without refraction the indicator still follows the finger`() = runTest {
        val (state, clock) = startLens(slot = 2, refracts = false)

        state.press(2f)
        state.dragBy(-1f)
        framesUntilIdle(clock) { state.position }

        assertEquals(1f, state.position)
    }

    @Test
    fun `a lens that stops refracting in flight is plain at once`() = runTest {
        val (state, clock) = startLens(slot = 0)
        state.select(2)
        framesUntil(clock) { state.travel > FullGlass }

        state.refracts = false

        assertEquals(0f, state.intensity)
        assertEquals(Resting, state.scale(Orientation.Horizontal))
    }

    @Test
    fun `glass that returns while the lens travels rises from plain`() = runTest {
        val (state, clock) = startLens(slot = 0)
        state.select(2)
        framesUntil(clock) { state.travel > FullGlass }
        state.refracts = false
        frame(clock)

        state.refracts = true
        val travel = listOf(state.travel) + List(3) {
            frame(clock)
            state.travel
        }

        assertTrue("$travel", travel.first() == 0f && travel.zipWithNext().all { (a, b) -> b > a })
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
    fun `a travelling lens stretches along its orientation`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.select(2)
        val frames = framesUntilIdle(clock) {
            Triple(
                state.travel,
                state.scale(Orientation.Horizontal),
                state.scale(Orientation.Vertical),
            )
        }
        val (_, horizontal, vertical) = frames.maxBy { it.first }

        assertTrue(
            "a bar's lens stretches along the bar: $horizontal",
            horizontal.scaleX > horizontal.scaleY,
        )
        assertTrue(
            "a rail's lens stretches along the rail: $vertical",
            vertical.scaleY > vertical.scaleX,
        )
    }

    @Test
    fun `a travelling lens never shrinks below its resting size`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.select(2)
        val scales = framesUntilIdle(clock) {
            listOf(state.scale(Orientation.Horizontal), state.scale(Orientation.Vertical))
        }.flatten()

        assertTrue("$scales", scales.all { it.scaleX >= 1f && it.scaleY >= 1f })
    }

    @Test
    fun `a lens at rest has its resting size`() = runTest {
        val (state, clock) = startLens(slot = 0)
        val beforeTap = state.scale(Orientation.Horizontal)
        state.select(2)

        framesUntilIdle(clock) { state.position }

        assertEquals(Resting, beforeTap)
        assertEquals(Resting, state.scale(Orientation.Horizontal))
        assertEquals(Resting, state.scale(Orientation.Vertical))
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
        val sampling = moving.map { it.first }

        assertTrue("it overshoots: $moving", moving.maxOf { it.second } > 2.1f)
        assertTrue("in one unbroken run: $sampling", isOneRun(sampling))
    }

    @Test
    fun `with animations off the lens jumps straight to the tab`() = runTest {
        val (state, clock) = startLens(slot = 0, motion = MotionScale(0f))

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

        val (slow, slowClock) = startLens(slot = 0, motion = MotionScale(2f))
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
    fun `a lens held under a still finger swells evenly by the full press`() = runTest {
        val (state, clock) = startLens(slot = 0)
        state.press(0f)
        state.dragBy(1f)

        framesUntilIdle(clock) { state.position }

        assertEquals("the lens has stopped, so it swells by the press alone", 0f, state.travel)
        assertEquals(FullPress, state.scale(Orientation.Horizontal))
        assertEquals(FullPress, state.scale(Orientation.Vertical))
    }

    @Test
    fun `a pressed lens trails the finger across the bar`() = runTest {
        val (state, clock) = startLens(slot = 0)

        state.press(0f)
        state.dragBy(1f)
        framesUntilIdle(clock) { state.position }

        assertTrue(state.pressed)
        assertEquals(1f, state.position)
    }

    @Test
    fun `a lens held still under a finger asks for no frames`() = runTest {
        val (state, clock) = startLens(slot = 0)
        state.press(0f)
        state.dragBy(1f)

        framesUntilIdle(clock) { state.position }

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
    fun `a release settles on the tab the lens's velocity points to`() = runTest {
        val (state, clock) = startLens(slot = 0)
        state.press(0f)
        state.dragBy(2f)
        repeat(4) { frame(clock) }
        val position = state.position
        val velocity = state.velocity

        val target = state.release()
        framesUntilIdle(clock) { state.position }

        assertTrue(velocity > 0f)
        assertEquals((position + velocity * 0.099f).roundToInt().coerceIn(0, 2), target)
        assertEquals(target.toFloat(), state.position)
        assertEquals(0f, state.pressure)
        assertFalse(state.pressed)
    }

    @Test
    fun `a released lens keeps the speed the finger gave it`() = runTest {
        val (state, clock) = startLens(slot = 0)
        state.press(0f)
        state.dragBy(2f)
        repeat(4) { frame(clock) }
        val thrown = state.velocity

        state.release()
        frame(clock)

        // Restarted from rest, the settle spring would reach under a fifth of the finger's speed
        // on this frame.
        assertTrue("$thrown, then ${state.velocity}", state.velocity > thrown / 2)
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

    /** Whether [travel] rises, if at all, to its peak, then only falls. */
    private fun liftsThenCalmsOnce(travel: List<Float>): Boolean {
        val peak = travel.indexOf(travel.max())
        return travel.take(peak + 1).zipWithNext().all { (before, after) -> after >= before } &&
            travel.drop(peak).zipWithNext().all { (before, after) -> after <= before }
    }

    private fun largestRise(travel: List<Float>): Float =
        travel.zipWithNext().maxOf { (before, after) -> after - before }

    /** The travel on the frame of a tap from the first tab to [to], then on every frame after. */
    private fun TestScope.travelOfTap(to: Int): List<Float> {
        val (state, clock) = startLens(slot = 0)
        state.select(to)
        return listOf(state.travel) + framesUntilIdle(clock) { state.travel }
    }

    /** How many frames a tap from the first tab to [to] takes to hand over to the indicator. */
    private fun TestScope.framesToHandOver(to: Int): Int {
        val (state, clock) = startLens(slot = 0)
        state.select(to)
        return handOverIn(framesUntilIdle(clock) { state.samplesBackdrop }) + 1
    }

    /** Whether [sampling] holds one unbroken run of frames that sample, and never one again. */
    private fun isOneRun(sampling: List<Boolean>): Boolean =
        sampling.any { it } && sampling.dropWhile { !it }.dropWhile { it }.none { it }

    /** The first frame that no longer samples after some did: where the lens hands over. */
    private fun handOverIn(sampling: List<Boolean>): Int {
        val start = sampling.indexOf(true)
        val end = sampling.drop(start).indexOf(false)
        check(start >= 0 && end >= 0) { "No hand-over in $sampling." }
        return start + end
    }

    private fun LiquidLensState.isPlain(): Boolean {
        val scale = scale(Orientation.Horizontal)
        return intensity == 0f && !samplesBackdrop && scale.scaleX == 1f && scale.scaleY == 1f
    }

    private fun TestScope.startLens(
        slot: Int,
        refracts: Boolean = true,
        settleSpring: SpringSpec<Float> = SettleSpring,
        motion: MotionScale = MotionScale(1f),
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
        backgroundScope.launch(clock + motion) { state.animate() }
        runCurrent()
        Snapshot.sendApplyNotifications()
        return state to clock
    }

    private fun TestScope.frame(clock: FrameClock) {
        val before = clock.frames
        advance()
        check(clock.frames > before) { "No frame was requested." }
    }

    private fun TestScope.framesUntil(clock: FrameClock, done: () -> Boolean) {
        repeat(MaxFrames) {
            frame(clock)
            if (done()) return
        }
        error("Not done after $MaxFrames frames.")
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

    /** The animator duration scale, which a person can change while the lens moves. */
    private class MotionScale(override var scaleFactor: Float) : MotionDurationScale

    private companion object {
        const val FrameMillis = 8L
        const val Period120HzNanos = 8_333_333L
        const val MaxFrames = 600
        const val Tolerance = 1e-4f
        const val FullGlass = 0.95f
        val Resting = ScaleFactor(1f, 1f)
        val FullPress = ScaleFactor(1.3f, 1.3f)

        /**
         * A first selection from the first tab, then a second: on along the bar, back to the
         * first tab, and two reversals of a longer tap.
         */
        val NewSelections = listOf(1 to 2, 1 to 0, 2 to 0, 2 to 1)
        val TrackSpring: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 1755f)
        val SettleSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 504f)
        val PressSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 1542f)
    }
}
