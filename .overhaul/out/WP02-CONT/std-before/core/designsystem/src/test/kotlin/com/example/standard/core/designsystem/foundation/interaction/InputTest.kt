package com.example.standard.core.designsystem.foundation.interaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import com.example.standard.core.designsystem.TestSdk
import com.example.standard.core.designsystem.component.input.AppTextField
import com.example.standard.core.designsystem.foundation.adaptive.InputTraits
import com.example.standard.core.designsystem.setThemed
import com.example.standard.core.designsystem.theme.AppTheme
import com.example.standard.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Mouse and keyboard on a control: ADAPTIVE-PLAN 9.2. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class InputTest {

    @get:Rule
    val rule = createComposeRule()

    private var clicks = 0
    private var secondary: Offset? = null
    private var pressStarts = 0

    private fun row() {
        rule.setThemed {
            Box(
                Modifier
                    .testTag("row")
                    .pressable(
                        onClick = { clicks++ },
                        feedback = PressFeedback.Highlight,
                        onSecondaryClick = { secondary = it },
                        onPressStart = { pressStarts++ },
                    ),
            ) {
                Box(Modifier.size(200.dp, 56.dp))
            }
        }
    }

    @Test
    fun `a right click opens the menu at the pointer and never clicks`() {
        row()
        rule.onNodeWithTag("row").performMouseInput {
            moveTo(Offset(30f, 20f))
            press(MouseButton.Secondary)
            release(MouseButton.Secondary)
        }
        rule.waitForIdle()
        assertEquals(0, clicks)
        assertEquals(Offset(30f, 20f), secondary)
    }

    @Test
    fun `a left click is still a click`() {
        row()
        rule.onNodeWithTag("row").performMouseInput { click(Offset(30f, 20f)) }
        rule.waitForIdle()
        assertEquals(1, clicks)
        assertEquals(null, secondary)
    }

    @Test
    fun `the press-start hook runs as the finger lands, before anything is known about the tap`() {
        row()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("row").performTouchInput { down(center) }
        rule.mainClock.advanceTimeByFrame()
        assertEquals(1, pressStarts)
        assertEquals(0, clicks)
        rule.onNodeWithTag("row").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(500)
        assertEquals(1, clicks)
    }

    @Test
    fun `Enter on a hardware keyboard submits, and Shift+Enter starts a new line`() {
        val field = TextFieldState("Hello")
        var submitted = 0
        rule.setContent {
            AppTheme(mode = ThemeMode.Light, reduceMotion = true, input = InputTraits(hardwareKeyboard = true)) {
                AppTextField(
                    state = field,
                    lineLimits = TextFieldLineLimits.MultiLine(),
                    onSubmit = { submitted++ },
                    modifier = Modifier.testTag("field"),
                )
            }
        }
        rule.onNodeWithTag("field").requestFocus()
        rule.onNodeWithTag("field").performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, submitted)
        assertEquals("Hello", field.text.toString())
        rule.onNodeWithTag("field").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        assertEquals(1, submitted)
        assertEquals(true, field.text.contains('\n'))
    }
}
