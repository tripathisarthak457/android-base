package com.example.finalafter.crui2.core.designsystem.component.sheet

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui2.core.designsystem.TestSdk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Where a modal sheet sits as the window grows. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class SheetAdaptiveTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val screen = SheetTestScreen()

    private fun showSheet() {
        rule.setContent { screen.Content() }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `R-10 with room on every side the sheet is a card in the middle that nothing recedes behind`() {
        showSheet()
        val stops = screen.state.stops!!
        assertTrue(stops.single)
        assertEquals(stops.top, screen.state.offset, 1f)
        rule.runOnIdle { assertEquals(0f, screen.state.expansionFraction, 0f) }
        val bounds = rule.onNodeWithTag("sheet_surface").getUnclippedBoundsInRoot()
        assertEquals(600.dp, bounds.right - bounds.left)
        assertEquals(640.dp, bounds.left + (bounds.right - bounds.left) / 2)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `on a phone held upright the sheet rises from the bottom edge`() {
        showSheet()
        val stops = screen.state.stops!!
        assertTrue(!stops.single)
        assertEquals(SheetDetent.Medium, screen.state.currentDetent)
    }
}
