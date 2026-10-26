package com.example.finalafter.crui.core.designsystem.component.sheet

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipe
import com.example.finalafter.crui.core.designsystem.TestSdk
import com.example.finalafter.crui.core.designsystem.component.sheet.surface.ScrimTag
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** B-33 to B-35, B-43: every way out of an unguarded modal sheet calls the caller once, and no more. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class SheetDismissTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val screen = SheetTestScreen()

    private fun showSheet() {
        rule.setContent { screen.Content() }
        settle()
    }

    private fun settle() {
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
    }

    private fun assertClosedOnce() {
        settle()
        assertEquals(1, screen.dismissRequests)
        assertEquals(1, screen.hiddenCalls)
        assertFalse(screen.state.isVisible)
        assertEquals(0, screen.poppedBeneath)
    }

    @Test
    fun `B-43 a Back burst while the sheet slides away closes it once and pops nothing beneath`() {
        showSheet()
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread {
            rule.activity.onBackPressedDispatcher.onBackPressed()
            rule.activity.onBackPressedDispatcher.onBackPressed()
        }
        rule.mainClock.advanceTimeBy(80)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertClosedOnce()
    }

    @Test
    fun `B-3 hiding twice runs one exit and onHidden once, and asks the caller once`() {
        showSheet()
        rule.runOnIdle {
            screen.scope.launch { screen.state.hide() }
            screen.scope.launch { screen.state.hide() }
        }
        assertClosedOnce()
    }

    @Test
    fun `B-8 a caller that clears visible is not asked to dismiss`() {
        showSheet()
        rule.runOnIdle { screen.visible = false }
        settle()
        assertEquals(0, screen.dismissRequests)
        assertEquals(1, screen.hiddenCalls)
    }

    @Test
    fun `B-33 a tap on the dim closes the sheet once`() {
        showSheet()
        rule.onNodeWithTag(ScrimTag).performTouchInput { click(Offset(centerX, 40f)) }
        assertClosedOnce()
    }

    @Test
    fun `B-33 the header's close button closes the sheet once`() {
        showSheet()
        rule.onNodeWithContentDescription("Close").performClick()
        assertClosedOnce()
    }

    @Test
    fun `B-35 Done after a save closes the sheet once`() {
        showSheet()
        rule.runOnIdle { screen.sheetScope.dismiss() }
        assertClosedOnce()
    }

    @Test
    fun `B-42 Escape closes the sheet once`() {
        showSheet()
        rule.onNodeWithTag("sheet_surface").performKeyInput { pressKey(Key.Escape) }
        assertClosedOnce()
    }

    @Test
    fun `B-22 a flick down from the medium detent closes the sheet once`() {
        showSheet()
        rule.onNodeWithTag("sheet_grabber").performTouchInput {
            swipe(center, Offset(centerX, centerY + 600f), durationMillis = 80)
        }
        assertClosedOnce()
    }
}
