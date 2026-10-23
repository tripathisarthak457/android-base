package com.example.lean.core.navigation.back

import androidx.compose.animation.core.spring
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigationevent.NavigationEvent
import com.example.lean.core.navigation.ROBOLECTRIC_SDK
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

/**
 * The card's state on its own, on a frame clock the test steps: what it tells its readers and when,
 * how the commit is timed from the release, and how a cancel ends.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class BackCardStateTest {

    private val card = BackCardState()
    private val frames = BroadcastFrameClock()
    private val spring = spring<Float>()

    private fun cardTest(body: suspend TestScope.() -> Unit) = runTest(frames) { body() }

    private fun TestScope.frame(nanos: Long) {
        frames.sendFrame(nanos)
        runCurrent()
    }

    /** Every coroutine of the card goes in [TestScope.backgroundScope]: cancelled, not awaited, once the test ends. */
    private fun startCard(scope: CoroutineScope, vararg progress: Float) {
        card.start(event(0f), subject = Top, beneath = setOf(Under), midPush = false, scope = scope)
        progress.forEach { card.track(event(it)) }
    }

    /** How many applied changes include the state [read] reads, counted until [block] returns. */
    private suspend fun appliesOf(read: () -> Unit, block: suspend () -> Unit): Int {
        val states = HashSet<Any>()
        Snapshot.observe(readObserver = { states += it }) { read() }
        var applies = 0
        val observer = Snapshot.registerApplyObserver { changed, _ -> if (changed.any { it in states }) applies++ }
        try {
            block()
            Snapshot.sendApplyNotifications()
        } finally {
            observer.dispose()
        }
        return applies
    }

    @Test
    fun `whether the card is up changes twice a gesture, so its readers run only as it comes and goes`() = cardTest {
        val applies = appliesOf({ card.active }) {
            startCard(backgroundScope, 0.3f, 0.6f)
            Snapshot.sendApplyNotifications()
            assertTrue(card.active)
            val committing = backgroundScope.launch {
                card.commitPop(BackGate(), spring, pop = { 1 }, releasedNanos = RELEASE, scope = backgroundScope)
            }
            runCurrent()
            Snapshot.sendApplyNotifications()
            var time = RELEASE
            while (!committing.isCompleted && time < RELEASE + ONE_SECOND) {
                time += FRAME
                frame(time)
                Snapshot.sendApplyNotifications()
            }
            assertFalse(card.active)
        }
        assertEquals("written at the start and at the end only", 2, applies)
    }

    @Test
    fun `the screen is drawn from its texture from the start until the card is gone, and no other`() = cardTest {
        assertFalse(card.textures(Top))
        startCard(backgroundScope, 0.5f)
        assertTrue(card.textures(Top))
        assertFalse(card.textures(Under))

        val committing = backgroundScope.launch {
            card.commitPop(BackGate(), spring, pop = { 1 }, releasedNanos = RELEASE, scope = backgroundScope)
        }
        runCurrent()
        frame(RELEASE + FRAME)
        assertTrue("through the fade", card.textures(Top))
        var time = RELEASE + FRAME
        while (!committing.isCompleted && time < RELEASE + ONE_SECOND) {
            time += FRAME
            frame(time)
        }
        assertFalse(card.textures(Top))
    }

    @Test
    fun `the first frame after the release is already as far into the fade as the time since it`() = cardTest {
        startCard(backgroundScope, 0.5f)
        backgroundScope.launch { card.commit(spring, RELEASE, scope = backgroundScope) }
        runCurrent()
        assertEquals(1f, card.contentAlpha.value)

        frame(RELEASE + 4 * MILLI)

        assertEquals(1f - 4f / BackCard.COMMIT_FADE_MILLIS, card.contentAlpha.value, 0.002f)
        assertTrue("the card has started back to full size", card.unwind.value > 0f)
    }

    @Test
    fun `a first frame that does not follow the release on its clock starts the fade there instead`() = cardTest {
        startCard(backgroundScope, 0.5f)
        val late = RELEASE + 2 * ONE_SECOND
        backgroundScope.launch { card.commit(spring, RELEASE, scope = backgroundScope) }
        runCurrent()

        frame(late)
        assertEquals(1f, card.contentAlpha.value)
        frame(late + 8 * MILLI)
        assertEquals(1f - 8f / BackCard.COMMIT_FADE_MILLIS, card.contentAlpha.value, 0.002f)
    }

    @Test
    fun `a cancel back at full size over a settled stack ends at once`() = cardTest {
        startCard(backgroundScope, 0.4f, 0.1f, 0f)

        val cancelling = backgroundScope.launch { card.cancel(midPush = false, fullWidth = true) }
        runCurrent()

        assertTrue("it waited for no frame", cancelling.isCompleted)
        assertFalse(card.active)
        assertFalse(card.textures(Top))
    }

    @Test
    fun `a cancel that still has something in view fades its scrim out`() = cardTest {
        startCard(backgroundScope, 0.4f, 0.1f, 0f)

        val cancelling = backgroundScope.launch { card.cancel(midPush = false, fullWidth = false) }
        runCurrent()

        assertFalse("a pane beside the card is still under the scrim", cancelling.isCompleted)
        var time = 0L
        while (!cancelling.isCompleted && time < ONE_SECOND) {
            time += FRAME
            frame(time)
        }
        assertFalse(card.active)
    }

    @Test
    fun `with no frame time from the platform the release velocity is still measured`() = cardTest {
        card.start(event(0f, frameTimeMillis = 0L), subject = Top, beneath = setOf(Under), midPush = false, scope = backgroundScope)
        for (step in 1..6) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(FRAME / MILLI))
            card.track(event(step * 0.08f, frameTimeMillis = 0L))
        }

        assertTrue(
            "a swipe of 0.48 in about 50 ms, was ${card.releaseVelocity()} per second",
            card.releaseVelocity() > BackCard.MIN_COMMIT_VELOCITY,
        )
    }

    private fun event(progress: Float, frameTimeMillis: Long = RELEASE / MILLI) = NavigationEvent(
        swipeEdge = NavigationEvent.EDGE_LEFT,
        progress = progress,
        touchX = progress * 400f,
        touchY = 600f,
        frameTimeMillis = frameTimeMillis,
    )

    private companion object {
        const val Top = "top"
        const val Under = "under"
        const val MILLI = 1_000_000L
        const val FRAME = 8 * MILLI
        const val ONE_SECOND = 1_000 * MILLI
        const val RELEASE = 5_000 * MILLI
    }
}
