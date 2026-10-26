package com.example.finalafter.crui.core.designsystem.component.formfields

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import com.example.finalafter.crui.core.designsystem.TestSdk
import com.example.finalafter.crui.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A held button keeps repeating in a gesture that started before the caller changed the stepper's
 * step or range; each repeat must use the values the stepper has now.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class StepperTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `holding a button repeats with the step and range the stepper has now`() {
        var value by mutableIntStateOf(0)
        var step by mutableIntStateOf(1)
        var range by mutableStateOf(0..100)
        rule.setThemed {
            AppStepper(value = value, onValueChange = { value = it }, range = range, step = step)
        }
        step = 5
        range = 0..7
        rule.waitForIdle()

        val increase = rule.onNodeWithContentDescription("Increase")
        increase.performTouchInput { down(center) }
        // The first repeat comes 500 ms into the hold, the next 150 ms after it.
        rule.mainClock.advanceTimeBy(600)
        assertEquals(5, value)
        rule.mainClock.advanceTimeBy(1_000)
        increase.performTouchInput { up() }
        rule.waitForIdle()

        assertEquals(7, value)
    }
}
