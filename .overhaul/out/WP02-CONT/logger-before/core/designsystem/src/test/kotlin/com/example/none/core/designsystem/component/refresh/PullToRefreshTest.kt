package com.example.none.core.designsystem.component.refresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.example.none.core.designsystem.TestSdk
import com.example.none.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class PullToRefreshTest {

    @get:Rule
    val rule = createComposeRule()

    private var refreshing by mutableStateOf(false)
    private var refreshes = 0
    private var rowCompositions = 0
    private lateinit var list: LazyListState
    private var firstRow: LayoutCoordinates? = null

    private fun setList(refreshOnPull: Boolean = true) {
        rule.setThemed {
            list = rememberLazyListState()
            AppPullToRefresh(
                isRefreshing = refreshing,
                onRefresh = {
                    refreshes++
                    if (refreshOnPull) refreshing = true
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(Modifier.fillMaxSize().testTag("list"), state = list) {
                    items((1..40).toList(), key = { it }) { row ->
                        SideEffect { rowCompositions++ }
                        Box(
                            Modifier
                                .height(RowHeight)
                                .then(if (row == 1) Modifier.onGloballyPositioned { firstRow = it } else Modifier),
                        )
                    }
                }
            }
        }
    }

    /**
     * Where the first row is drawn now. Read from its coordinates rather than its semantics bounds,
     * which follow layout and placement but not a layer moving on its own.
     */
    private fun firstRowTop(): Float = rule.runOnUiThread { checkNotNull(firstRow).positionInRoot().y }

    /** Finger travel well past the trigger on any viewport the tests run in. */
    private fun pullFarWithoutLifting() {
        rule.onNodeWithTag("list").performTouchInput {
            down(Offset(centerX, top + 10f))
            repeat(PullSteps) { moveBy(Offset(0f, PullStep)) }
        }
    }

    @Test
    fun `the refresh starts as the pull crosses the trigger, before the finger lifts`() {
        setList()
        pullFarWithoutLifting()
        rule.waitForIdle()
        assertEquals(1, refreshes)

        rule.onNodeWithTag("list").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(1, refreshes)
    }

    @Test
    fun `a pull while a refresh runs does not start another, and one after it ends does`() {
        setList()
        rule.onNodeWithTag("list").performTouchInput { swipeDown(startY = top + 10f, endY = bottom, durationMillis = 600) }
        rule.waitForIdle()
        rule.onNodeWithTag("list").performTouchInput { swipeDown(startY = top + 10f, endY = bottom, durationMillis = 600) }
        rule.waitForIdle()
        assertEquals(1, refreshes)

        refreshing = false
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.onNodeWithTag("list").performTouchInput { swipeDown(startY = top + 10f, endY = bottom, durationMillis = 600) }
        rule.waitForIdle()
        assertEquals(2, refreshes)
    }

    @Test
    fun `the content rests below the spinner while refreshing, and returns when it ends`() {
        setList()
        val top = firstRowTop()
        rule.onNodeWithTag("list").performTouchInput { swipeDown(startY = top + 10f, endY = bottom, durationMillis = 600) }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        assertEquals(top + RestPx, firstRowTop(), 1f)

        refreshing = false
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        assertEquals(top, firstRowTop(), 0.5f)
    }

    @Test
    fun `pushing up during a refresh closes the gap before the list scrolls`() {
        setList()
        val top = firstRowTop()
        rule.onNodeWithTag("list").performTouchInput { swipeDown(startY = top + 10f, endY = bottom, durationMillis = 600) }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()

        rule.onNodeWithTag("list").performTouchInput {
            down(center)
            moveBy(Offset(0f, -viewConfiguration.touchSlop - 1f))
            moveBy(Offset(0f, -RestPx / 2f))
        }
        rule.waitForIdle()
        assertEquals(0, list.firstVisibleItemIndex)
        assertEquals(0, list.firstVisibleItemScrollOffset)
        assertTrue(firstRowTop() < top + RestPx)

        rule.onNodeWithTag("list").performTouchInput {
            moveBy(Offset(0f, -RestPx))
            up()
        }
        rule.waitForIdle()
        assertTrue(list.firstVisibleItemScrollOffset > 0 || list.firstVisibleItemIndex > 0)
    }

    @Test
    fun `a flick up during a refresh closes the gap and carries the list on without a pause`() {
        setList()
        val top = firstRowTop()
        rule.onNodeWithTag("list").performTouchInput { swipeDown(startY = top + 10f, endY = bottom, durationMillis = 600) }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        val rowPx = with(rule.density) { RowHeight.toPx() }
        // How far the content has gone up: through the gap first (negative while it shows), then the list.
        fun travelled(): Float =
            if (list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0) {
                top - firstRowTop()
            } else {
                list.firstVisibleItemIndex * rowPx + list.firstVisibleItemScrollOffset
            }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("list").performTouchInput { swipeUp(startY = top + RestPx + 40f, endY = top + RestPx, durationMillis = 40) }
        rule.mainClock.advanceTimeByFrame()
        val positions = List(HandOverFrames) {
            rule.mainClock.advanceTimeByFrame()
            travelled()
        }
        rule.mainClock.autoAdvance = true
        val steps = positions.zipWithNext { a, b -> b - a }
        assertTrue("the content paused in its fling: $steps", steps.all { it > 0.5f })
        assertTrue("the fling never reached the list: $positions", positions.last() > 0f)
    }

    @Test
    fun `a pull released short of the trigger springs back without refreshing`() {
        setList()
        val top = firstRowTop()
        rule.onNodeWithTag("list").performTouchInput {
            down(Offset(centerX, this.top + 10f))
            moveBy(Offset(0f, viewConfiguration.touchSlop + 1f))
            moveBy(Offset(0f, 60f))
        }
        rule.waitForIdle()
        assertTrue(firstRowTop() > top)
        rule.onNodeWithTag("list").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        assertEquals(0, refreshes)
        assertEquals(top, firstRowTop(), 0.5f)
    }

    @Test
    fun `a fling that reaches the top never refreshes`() {
        setList()
        // Far enough down that the drag alone cannot reach the top: only its fling does.
        rule.runOnIdle { list.requestScrollToItem(FlingFromRow) }
        rule.waitForIdle()
        rule.onNodeWithTag("list").performTouchInput { swipeDown(startY = top + 10f, endY = bottom, durationMillis = 80) }
        rule.mainClock.advanceTimeBy(3_000)
        rule.waitForIdle()
        assertEquals(0, refreshes)
    }

    @Test
    fun `the release carries the finger's velocity into the settle`() {
        setList(refreshOnPull = false)
        val top = firstRowTop()

        fun pullAndRelease(pauseBeforeLift: Boolean): Float {
            rule.mainClock.autoAdvance = false
            rule.onNodeWithTag("list").performTouchInput {
                down(Offset(centerX, this.top + 10f))
                moveBy(Offset(0f, viewConfiguration.touchSlop + 1f))
                repeat(ShortPullSteps) { moveBy(Offset(0f, PullStep), delayMillis = 8) }
                if (pauseBeforeLift) {
                    repeat(SettlePauseSteps) { moveBy(Offset.Zero, delayMillis = 40) }
                }
                up()
            }
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()
            val afterTwoFrames = firstRowTop()
            rule.mainClock.autoAdvance = true
            rule.mainClock.advanceTimeBy(2_000)
            rule.waitForIdle()
            assertEquals(top, firstRowTop(), 0.5f)
            return afterTwoFrames
        }

        val withVelocity = pullAndRelease(pauseBeforeLift = false)
        val fromRest = pullAndRelease(pauseBeforeLift = true)
        // Both start from the same gap. A release still moving down keeps going a little before the
        // spring turns it, so it is lower two frames in than one let go from rest.
        assertTrue("$withVelocity vs $fromRest", withVelocity > fromRest)
    }

    @Test
    fun `pulling and settling recompose none of the content`() {
        setList(refreshOnPull = false)
        rule.waitForIdle()
        val before = rowCompositions
        pullFarWithoutLifting()
        rule.onNodeWithTag("list").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(3_000)
        rule.waitForIdle()
        assertEquals(1, refreshes)
        assertEquals(before, rowCompositions)
    }

    private companion object {
        val RowHeight = 60.dp
        const val PullSteps = 40
        const val ShortPullSteps = 8
        const val FlingFromRow = 15
        const val SettlePauseSteps = 6
        const val HandOverFrames = 10
        const val PullStep = 20f

        /** The resting gap, 60dp, at Robolectric's default density of 1. */
        const val RestPx = 60f
    }
}
