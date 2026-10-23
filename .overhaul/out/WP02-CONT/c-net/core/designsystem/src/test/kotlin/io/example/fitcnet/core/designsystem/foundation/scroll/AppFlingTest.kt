package io.example.fitcnet.core.designsystem.foundation.scroll

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.dp
import io.example.fitcnet.core.designsystem.TestSdk
import io.example.fitcnet.core.designsystem.foundation.interaction.PressFeedback
import io.example.fitcnet.core.designsystem.foundation.interaction.pressable
import io.example.fitcnet.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w400dp-h800dp")
class AppFlingTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a gentle fling ends once it no longer visibly moves, and the next tap reaches a row`() {
        val list = LazyListState()
        val clicked = mutableListOf<Int>()
        rule.setThemed {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("list"),
                state = list,
                flingBehavior = AppScrollDefaults.flingBehavior(),
            ) {
                items(200, key = { it }, contentType = { "row" }) { index ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(RowHeight)
                            .testTag("row $index")
                            .pressable(onClick = { clicked += index }, feedback = PressFeedback.Highlight),
                    )
                }
            }
        }
        // Time moves only when the test moves it: waiting for idle would otherwise run the fling out.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("list").performTouchInput {
            swipeWithVelocity(start = Offset(centerX, bottom - 20f), end = Offset(centerX, centerY), endVelocity = GentleRelease)
        }
        val rowPx = with(rule.density) { RowHeight.roundToPx() }
        val released = list.firstVisibleItemIndex * rowPx + list.firstVisibleItemScrollOffset
        rule.mainClock.advanceTimeBy(SettledMillis)
        val settled = list.firstVisibleItemIndex * rowPx + list.firstVisibleItemScrollOffset
        assertTrue("the release carried the list on", settled > released)
        assertFalse("still flinging $SettledMillis ms after a gentle release", list.isScrollInProgress)

        val target = list.firstVisibleItemIndex + 2
        rule.onNodeWithTag("row $target").performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(listOf(target), clicked)
    }

    private companion object {
        val RowHeight = 56.dp

        /** Pixels a second: a list let go of slowly, as a person does before reading. */
        const val GentleRelease = 1_000f

        /** After the fling has come to rest, and well inside the decay's tail at Compose's default threshold. */
        const val SettledMillis = 2_500L
    }
}
