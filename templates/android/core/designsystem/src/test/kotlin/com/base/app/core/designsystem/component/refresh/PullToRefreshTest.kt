package com.base.app.core.designsystem.component.refresh

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.TEST_SDK
import com.base.app.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class PullToRefreshTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a pull past the threshold refreshes, and another pull while it runs does not`() {
        var refreshing by mutableStateOf(false)
        var refreshes = 0
        rule.setThemed {
            AppPullToRefresh(isRefreshing = refreshing, onRefresh = { refreshes++; refreshing = true }, modifier = Modifier.fillMaxSize()) {
                LazyColumn(Modifier.fillMaxSize().testTag("list")) {
                    items((1..30).toList(), key = { it }) { Box(Modifier.height(60.dp)) }
                }
            }
        }
        rule.onNodeWithTag("list").performTouchInput { swipeDown(startY = top + 10f, endY = bottom, durationMillis = 600) }
        rule.waitForIdle()
        assertEquals(1, refreshes)

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
}
