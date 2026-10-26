package com.example.cmp.core.designsystem.foundation.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.cmp.core.designsystem.TestSdk
import com.example.cmp.core.designsystem.setThemed
import kotlin.math.ln
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The fling against UIScrollView's normal deceleration, which keeps 0.998 of the speed every
 * millisecond: a fling at v covers v / k, k = 1000 · ln(1 / 0.998) a second, its speed falling as
 * e^(-k·t). The design system's fling follows that curve until the speed is too low to see (the rest
 * velocity), which costs the rest velocity's own share of the distance and nothing else.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class AppFlingCurveTest {

    @get:Rule
    val rule = createComposeRule()

    private class Fling(val distance: Float, val millis: Long)

    private fun fling(velocity: Float): Fling {
        val scroll = ScrollState(0)
        lateinit var behavior: FlingBehavior
        lateinit var scope: CoroutineScope
        rule.setThemed {
            behavior = AppScrollDefaults.flingBehavior()
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize().verticalScroll(scroll)) { Spacer(Modifier.fillMaxWidth().height(20_000.dp)) }
        }
        rule.mainClock.autoAdvance = false
        val start = rule.mainClock.currentTime
        val job = scope.launch { scroll.scroll { with(behavior) { performFling(velocity) } } }
        var frames = 0
        while (job.isActive && frames++ < MaxFrames) rule.mainClock.advanceTimeByFrame()
        assertTrue("the fling never ended", !job.isActive)
        return Fling(scroll.value.toFloat(), rule.mainClock.currentTime - start)
    }

    private fun assertOnTheIosCurve(velocity: Float) {
        val run = fling(velocity)
        assertEquals("distance of a $velocity px/s fling", (velocity - RestVelocity) / IosRate, run.distance, 2f)
        // Within one frame either side: the fling starts on the next frame and ends on a frame.
        val expectedMillis = ln(velocity / RestVelocity) / IosRate * 1000f
        assertEquals("duration of a $velocity px/s fling", expectedMillis, run.millis.toFloat(), 2 * FrameMillis)
        assertTrue("the rest velocity costs at most its own share", velocity / IosRate - run.distance <= RestVelocity / IosRate + 2f)
    }

    @Test
    fun `a gentle fling follows the iOS curve`() = assertOnTheIosCurve(800f)

    @Test
    fun `an ordinary flick follows the iOS curve`() = assertOnTheIosCurve(3_000f)

    @Test
    fun `a hard throw follows the iOS curve`() = assertOnTheIosCurve(9_000f)

    private companion object {
        /** UIScrollView.DecelerationRate.normal as a continuous rate: 1000 · ln(1 / 0.998) a second. */
        val IosRate = (1000.0 * ln(1.0 / 0.998)).toFloat()

        /** The design system's rest velocity, in pixels a second (`AppMotion.kt`). */
        const val RestVelocity = 30f

        const val FrameMillis = 16f
        const val MaxFrames = 1_000
    }
}
