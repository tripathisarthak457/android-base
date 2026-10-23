package io.example.fitcnet.core.designsystem.component.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import io.example.fitcnet.core.designsystem.TestSdk
import io.example.fitcnet.core.designsystem.component.button.AppButton
import io.example.fitcnet.core.designsystem.component.container.AppCard
import io.example.fitcnet.core.designsystem.setThemed
import io.example.fitcnet.core.designsystem.theme.AppTheme
import io.example.fitcnet.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A placeholder row is the real row, `onClick` and all, so redaction is what keeps it from being
 * used: a redacted control takes no tap, no focus and no key, lets go of a press it was in, and
 * works again as soon as the content is shown.
 */
@OptIn(ExperimentalTestApi::class)
// Focus, not pixels: under the native graphics the module's screenshot tests switch on for every test,
// a requested focus never lands in Robolectric, so this class keeps the legacy mode it was written for.
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class RedactedInputTest {

    @get:Rule
    val rule = createComposeRule()

    private var redacted by mutableStateOf(true)
    private var cardClicks = 0
    private var buttonClicks = 0

    @Test
    fun `a redacted card and button take no tap, and take one again once shown`() {
        setStill {
            Column {
                Box(Modifier.testTag("card")) {
                    AppRedacted(active = redacted) {
                        AppCard(onClick = { cardClicks++ }, modifier = Modifier.size(CardWidth, CardHeight)) {}
                    }
                }
                Box(Modifier.testTag("button")) {
                    AppRedacted(active = redacted) { AppButton(text = "Save", onClick = { buttonClicks++ }) }
                }
            }
        }
        tapBoth()
        assertEquals("the redacted card was tapped", 0, cardClicks)
        assertEquals("the redacted button was tapped", 0, buttonClicks)

        redacted = false
        tapBoth()
        assertEquals(1, cardClicks)
        assertEquals(1, buttonClicks)
    }

    @Test
    fun `a redacted button takes no focus and no Enter, and loses focus it had`() {
        val requester = FocusRequester()
        var focused = false
        setStill {
            Box(
                Modifier
                    .testTag("button")
                    .focusRequester(requester)
                    .onFocusChanged { focused = it.hasFocus },
            ) {
                AppRedacted(active = redacted) { AppButton(text = "Save", onClick = { buttonClicks++ }) }
            }
        }
        rule.runOnIdle { requester.requestFocus() }
        assertFalse("the redacted button took focus", focused)
        pressEnter()
        assertEquals("Enter reached the redacted button", 0, buttonClicks)

        redacted = false
        rule.runOnIdle { requester.requestFocus() }
        assertTrue("the shown button took no focus", focused)
        pressEnter()
        assertEquals(1, buttonClicks)

        redacted = true
        rule.waitForIdle()
        assertFalse("the button kept focus once redacted", focused)
        pressEnter()
        assertEquals("Enter reached the button once redacted", 1, buttonClicks)
    }

    @Test
    fun `redacting a card held down lets go of the press and brings it back to full size`() {
        var probe: LayoutCoordinates? = null
        redacted = false
        rule.setThemed {
            Box(Modifier.testTag("card")) {
                AppRedacted(active = redacted) {
                    AppCard(onClick = { cardClicks++ }, modifier = Modifier.size(CardWidth, CardHeight)) {
                        Box(Modifier.fillMaxSize().onPlaced { probe = it })
                    }
                }
            }
        }
        val rest = contentWidth { probe }
        rule.onNodeWithTag("card").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(HoldMillis)
        val pressed = contentWidth { probe }
        assertTrue("the card did not shrink under the finger: $pressed of $rest", pressed < rest - 1f)

        redacted = true
        rule.mainClock.advanceTimeBy(SettleMillis)
        assertEquals("the card stayed pressed", rest, contentWidth { probe }, 0.5f)

        rule.onNodeWithTag("card").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(SettleMillis)
        assertEquals("lifting the finger clicked the redacted card", 0, cardClicks)
    }

    /** Motion off, so a redacted button's bone holds still and no idle check waits out a sweep. */
    private fun setStill(content: @Composable () -> Unit) {
        rule.setContent { AppTheme(mode = ThemeMode.Light, reduceMotion = true, content = content) }
    }

    private fun tapBoth() {
        rule.onNodeWithTag("card").performTouchInput { click() }
        rule.onNodeWithTag("button").performTouchInput { click() }
        rule.waitForIdle()
    }

    private fun pressEnter() {
        rule.onNodeWithTag("button").performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
    }

    /** The width the card's content is drawn at, in pixels, its press scale included. */
    private fun contentWidth(probe: () -> LayoutCoordinates?): Float =
        rule.runOnIdle { checkNotNull(probe()).boundsInRoot().width }

    private companion object {
        val CardWidth = 240.dp
        val CardHeight = 120.dp

        /** Past the tap timeout, after which a held press shows. */
        const val HoldMillis = 400L
        const val SettleMillis = 1_000L
    }
}
