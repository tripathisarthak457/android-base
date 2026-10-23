package com.example.standard.core.designsystem.foundation.scroll

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.standard.core.designsystem.TestSdk
import com.example.standard.core.designsystem.setThemed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w400dp-h800dp")
class ScrollToTopTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a list far down reaches the top quickly, composing only the last two screens of rows`() {
        val state = LazyListState(firstVisibleItemIndex = 400)
        val composed = mutableSetOf<Int>()
        lateinit var scope: CoroutineScope
        rule.setThemed {
            scope = rememberCoroutineScope()
            LazyColumn(Modifier.fillMaxSize(), state = state, flingBehavior = AppScrollDefaults.flingBehavior()) {
                items(500, key = { it }) { index ->
                    SideEffect { composed += index }
                    BasicText("Row $index", Modifier.fillMaxWidth().height(RowHeight))
                }
            }
        }
        val screen = state.layoutInfo.visibleItemsInfo.size
        rule.mainClock.autoAdvance = false
        composed.clear()
        val job = scope.launch { state.animateScrollToTop() }
        var frames = 0
        while (job.isActive && frames++ < MaxFrames) rule.mainClock.advanceTimeByFrame()

        assertTrue("still scrolling after ${frames * 16} ms", !job.isActive)
        assertEquals(0, state.firstVisibleItemIndex)
        assertEquals(0, state.firstVisibleItemScrollOffset)
        assertTrue("took $frames frames", frames * 16 <= MaxTripMillis)
        // The rows of the two screens it glides over, the screen it lands on and the one it jumped to.
        assertTrue("composed ${composed.size} rows for a $screen-row screen", composed.size <= screen * 4 + 2)
    }

    private companion object {
        val RowHeight = 56.dp
        const val MaxFrames = 300
        const val MaxTripMillis = 1_000
    }
}
