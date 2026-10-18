package com.base.app.core.designsystem.foundation.scroll

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.TestSdk
import com.base.app.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * The edge every list gets from `AppTheme`: past its end the content follows the finger along iOS's
 * rubber band, and a fling that reaches the end carries on past it and springs back without a jump.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w400dp-h800dp")
class AppOverscrollTest {

    @get:Rule
    val rule = createComposeRule()

    private fun list(state: LazyListState = LazyListState()) {
        rule.setThemed {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(ListHeight).testTag("list"),
                state = state,
                flingBehavior = AppScrollDefaults.flingBehavior(),
            ) {
                items(Rows, key = { it }) { index ->
                    BasicText("Row $index", Modifier.fillMaxWidth().height(RowHeight).testTag("row $index"))
                }
            }
        }
    }

    private fun rowTop(index: Int): Float =
        rule.onNodeWithTag("row $index").getUnclippedBoundsInRoot().top.value -
            rule.onNodeWithTag("list").getUnclippedBoundsInRoot().top.value

    @Test
    fun `pulled past the top, the content follows the finger along the rubber band`() {
        list()
        val height = with(rule.density) { ListHeight.toPx() }
        val pull = height / 2
        rule.onNodeWithTag("list").performTouchInput {
            down(center)
            moveBy(Offset(0f, viewConfiguration.touchSlop + 1f))
            moveBy(Offset(0f, pull - 1f))
        }
        rule.waitForIdle()
        // iOS: shown = (1 − 1 / (pull · 0.55 / d + 1)) · d for a container d tall.
        val expected = (1f - 1f / (pull * Resistance / height + 1f)) * height
        val shown = with(rule.density) { rowTop(0).dp.toPx() }
        assertEquals(expected, shown, 2f)
        assertTrue("the band resists: $shown of $pull", shown < pull * Resistance)
    }

    @Test
    fun `pulled past the top and back without letting go, the list follows the finger back`() {
        val state = LazyListState()
        list(state)
        val pull = with(rule.density) { ListHeight.toPx() } / 2
        rule.onNodeWithTag("list").performTouchInput {
            down(center)
            moveBy(Offset(0f, viewConfiguration.touchSlop + 1f))
            moveBy(Offset(0f, pull - 1f))
        }
        rule.waitForIdle()
        val stretched = rowTop(0)
        rule.onNodeWithTag("list").performTouchInput { moveBy(Offset(0f, -pull / 2)) }
        rule.waitForIdle()
        val halfway = rowTop(0)
        assertTrue(
            "taken halfway back, the band went from $stretched to $halfway dp instead of easing back with the finger",
            halfway < stretched && halfway >= 0f,
        )
        rule.onNodeWithTag("list").performTouchInput { moveBy(Offset(0f, -pull / 2 - IntoTheListPx)) }
        rule.waitForIdle()
        val rowPx = with(rule.density) { RowHeight.roundToPx() }
        val scrolled = rule.runOnIdle { state.firstVisibleItemIndex * rowPx + state.firstVisibleItemScrollOffset }
        assertEquals(
            "past where the drag began, the list did not follow the finger",
            IntoTheListPx,
            scrolled.toFloat(),
            2f,
        )
        rule.onNodeWithTag("list").performTouchInput { up() }
    }

    @Test
    fun `a fling into the end carries past it and springs back without a jump`() {
        val state = LazyListState(firstVisibleItemIndex = Rows - 12)
        list(state)
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("list").performTouchInput {
            swipeWithVelocity(start = Offset(centerX, bottom - 10f), end = Offset(centerX, centerY), endVelocity = HardFling)
        }
        val listBottom = rule.onNodeWithTag("list").getUnclippedBoundsInRoot().bottom.value
        val gaps = mutableListOf<Float>()
        repeat(SettleFrames) {
            rule.mainClock.advanceTimeByFrame()
            // How far the last row has been carried above the list's bottom edge: past the end.
            gaps += runCatching { listBottom - rule.onNodeWithTag("row ${Rows - 1}").getUnclippedBoundsInRoot().bottom.value }
                .getOrDefault(0f)
                .coerceAtLeast(0f)
        }
        assertTrue("the fling never reached past the end: $gaps", gaps.max() > 4f)
        assertEquals("the edge did not settle back", 0f, gaps.last(), 0.5f)
        // Never faster than the fling itself was moving: anything more is the content jumping.
        val frameTravel = HardFling / rule.density.density * FrameMillis / 1000f
        val steps = gaps.zipWithNext { a, b -> abs(b - a) }
        assertTrue("the edge jumped: ${steps.max()} dp in one frame", steps.max() <= frameTravel + 1f)
    }

    private companion object {
        val ListHeight = 400.dp
        val RowHeight = 56.dp
        const val Rows = 40
        const val Resistance = 0.55f

        /** Pixels the finger goes on into the list once the band has eased. */
        const val IntoTheListPx = 60f

        /** Pixels a second, a firm flick; the list is a few rows from its end. */
        const val HardFling = 6_000f
        const val SettleFrames = 120
        const val FrameMillis = 16f
    }
}
