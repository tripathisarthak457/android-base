package com.base.app.core.designsystem.component.pickers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.base.app.core.designsystem.RecompositionCounter
import com.base.app.core.designsystem.TestSdk
import com.base.app.core.designsystem.setThemed
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The wheel's journey, counted in composables that ran as `RecompositionJourneysTest` counts the
 * rest of the design system's: a turn moves the items' layers and recomposes the wheel only for
 * the value it lands on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class WheelPickerRecompositionTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val counter = RecompositionCounter()

    @Test
    fun `turning a wheel recomposes only for the value it lands on`() {
        var selected by mutableIntStateOf(3)
        rule.setThemed {
            AppWheelPicker(
                items = (0..40).toList(),
                selectedIndex = selected,
                onSelectedChange = {
                    selected =
                        it
                },
                modifier = Modifier.testTag("wheel"),
                label = { "$it" },
            )
        }
        rule.waitForIdle()
        counter.reset()
        rule.onNodeWithTag("wheel").performTouchInput {
            swipeUp(
                startY = bottom - 10f,
                endY =
                    top + 10f,
                durationMillis = 600,
            )
        }
        rule.mainClock.advanceTimeBy(3_000)
        assertTrue("the wheel did not move", selected > 3)
        assertTrue("landing recomposed ${counter.executed()}", counter.count("AppWheelPicker") <= 1)
    }
}
