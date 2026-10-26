package com.example.finalafter.crui2.core.designsystem.component.sheet

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.example.finalafter.crui2.core.designsystem.TestSdk
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** B-16 to B-26: the finger, the release and the rubber band on a phone held upright. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class SheetGestureTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val screen = SheetTestScreen()
    private val stops get() = screen.state.stops!!

    private fun showSheet() {
        rule.setContent { screen.Content() }
        settle()
    }

    private fun settle() {
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
    }

    private fun drag(dy: Float, steps: Int = 10) {
        rule.onNodeWithTag("sheet_grabber").performTouchInput {
            down(center)
            repeat(steps) { moveBy(Offset(0f, dy / steps)) }
            up()
        }
    }

    @Test
    fun `B-1a dragging the sheet re-places its content without measuring it again`() {
        showSheet()
        val before = screen.contentMeasures
        rule.onNodeWithTag("sheet_grabber").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, -15f)) }
            repeat(10) { moveBy(Offset(0f, 25f)) }
            up()
        }
        settle()
        assertEquals(before, screen.contentMeasures)
    }

    @Test
    fun `B-24 pulling past the large detent gives less the further it goes, and B-26 lets go at large`() {
        showSheet()
        rule.runOnIdle { screen.scope.launch { screen.state.snapTo(SheetDetent.Large) } }
        settle()
        val large = stops.offsetOf(SheetDetent.Large)
        val shown = mutableListOf<Float>()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("sheet_grabber").performTouchInput {
            down(center)
            repeat(10) {
                moveBy(Offset(0f, -100f))
                shown += -screen.state.stretch
            }
        }
        val gains = shown.zipWithNext { a, b -> b - a }
        assertTrue("each pull past the top shows less: $shown", gains.zipWithNext().all { (a, b) -> b <= a + 0.01f })
        assertTrue("the band stays below its asymptote: $shown", shown.last() < stops.roomAbove)
        rule.onNodeWithTag("sheet_grabber").performTouchInput { up() }
        rule.mainClock.autoAdvance = true
        settle()
        assertEquals(large, screen.state.offset, 0.5f)
        assertEquals(0f, screen.state.stretch, 0.5f)
    }

    @Test
    fun `B-22 a slow release near medium settles back on it`() {
        showSheet()
        drag(dy = 120f, steps = 30)
        settle()
        assertEquals(SheetDetent.Medium, screen.state.currentDetent)
        assertEquals(stops.offsetOf(SheetDetent.Medium), screen.state.offset, 0.5f)
        assertEquals(0, screen.dismissRequests)
    }

    @Test
    fun `B-22 a flick up from medium goes one stop, to large`() {
        showSheet()
        rule.onNodeWithTag("sheet_grabber").performTouchInput {
            swipe(center, Offset(centerX, centerY - 150f), durationMillis = 60)
        }
        settle()
        assertEquals(SheetDetent.Large, screen.state.currentDetent)
    }

    @Test
    fun `the expansion is zero at the medium detent and one at large`() {
        showSheet()
        rule.runOnIdle { assertEquals(0f, screen.state.expansionFraction, 0.001f) }
        rule.runOnIdle { screen.scope.launch { screen.state.animateTo(SheetDetent.Large) } }
        settle()
        rule.runOnIdle { assertEquals(1f, screen.state.expansionFraction, 0.001f) }
    }

    @Test
    fun `B-19 tapping the grabber moves to the next taller detent and from the tallest to the lowest`() {
        showSheet()
        rule.onNodeWithTag("sheet_grabber").performClick()
        settle()
        assertEquals(SheetDetent.Large, screen.state.currentDetent)
        rule.onNodeWithTag("sheet_grabber").performClick()
        settle()
        assertEquals(SheetDetent.Medium, screen.state.currentDetent)
        assertEquals(0, screen.dismissRequests)
    }

    @Test
    fun `B-9 animateTo during a hide does nothing`() {
        showSheet()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            screen.scope.launch { screen.state.hide() }
            screen.scope.launch { screen.state.animateTo(SheetDetent.Large) }
        }
        rule.mainClock.autoAdvance = true
        settle()
        assertEquals(false, screen.state.isVisible)
    }
}
