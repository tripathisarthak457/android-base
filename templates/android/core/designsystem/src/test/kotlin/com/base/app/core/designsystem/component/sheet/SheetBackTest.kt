package com.base.app.core.designsystem.component.sheet

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.base.app.core.designsystem.TestSdk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** B-38 to B-41: the predictive Back gesture shrinks the sheet, a cancel restores it, a commit closes it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class SheetBackTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val screen = SheetTestScreen()

    private fun settle() {
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
    }

    private fun startGesture(progress: Float) {
        val dispatcher = rule.activity.onBackPressedDispatcher
        rule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(
                BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT),
            )
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(100f, 400f, progress, BackEventCompat.EDGE_LEFT),
            )
        }
        rule.waitForIdle()
    }

    @Test
    fun `B-39 the gesture shrinks the sheet as it goes and B-40 a cancel puts it back`() {
        rule.setContent { screen.Content() }
        settle()
        startGesture(0.5f)
        val mid = screen.state.backProgress
        assertTrue("eased progress at half the gesture: $mid", mid > 0f && mid < 1f)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        settle()
        assertEquals(0f, screen.state.backProgress, 0.001f)
        assertEquals(0, screen.dismissRequests)
        assertEquals(SheetDetent.Medium, screen.state.currentDetent)
    }

    @Test
    fun `B-40 a committed gesture closes the sheet once and pops nothing beneath`() {
        rule.setContent { screen.Content() }
        settle()
        startGesture(0.8f)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
        assertEquals(1, screen.dismissRequests)
        assertEquals(1, screen.hiddenCalls)
        assertEquals(0, screen.poppedBeneath)
        assertEquals(0f, screen.state.backProgress, 0.001f)
    }
}
