package com.example.standard.core.navigation.back

import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.example.standard.core.common.navigation.AppNavKey
import com.example.standard.core.designsystem.theme.AppTheme
import com.example.standard.core.navigation.AppBackStack
import com.example.standard.core.navigation.FeedTab
import com.example.standard.core.navigation.Post
import com.example.standard.core.navigation.ROBOLECTRIC_SDK
import com.example.standard.core.navigation.onNodeWithTagExists
import com.example.standard.core.navigation.stack.StackHost
import com.example.standard.core.navigation.testRegistry
import com.example.standard.core.navigation.transition.ScreenMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Three Back presses in one burst while a pop runs: one more transition, to the root, and no gap. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class RapidBackTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val stack = AppBackStack(mutableStateListOf(FeedTab, Post(1), Post(2), Post(3)))
    private val sizes = mutableListOf<Int>()

    @Test
    fun `a burst of presses mid-pop becomes one pop to the root`() {
        rule.setContent {
            AppTheme { StackHost(stack = stack, registry = testRegistry(), motion = ScreenMotion()) }
            LaunchedEffect(Unit) { snapshotFlow { stack.size }.collect { sizes += it } }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.mainClock.advanceTimeBy(FRAME_MS * 3)
        rule.runOnUiThread {
            repeat(3) { rule.activity.onBackPressedDispatcher.onBackPressed() }
        }

        var framesToRoot = -1
        var rootShown = false
        repeat(STEPS) {
            rule.mainClock.advanceTimeBy(FRAME_MS)
            if (stack.size == 1 && framesToRoot < 0) framesToRoot = 0
            if (framesToRoot >= 0 && !rootShown) {
                rootShown = rule.onAllNodesWithTag(FeedTab.toString(), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
                if (!rootShown) framesToRoot++
            } else if (rootShown) {
                // Once the root arrives it stays: no blank frame, no frame of a screen popped past.
                rule.onNodeWithTagExists(FeedTab)
            }
        }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertEquals("one pop, then one pop to the root", listOf(4, 3, 1), sizes)
        assertTrue("the root is composed within a frame or two of the pop: $framesToRoot", framesToRoot in 0..2)
        assertEquals(listOf<AppNavKey>(FeedTab), stack.keys)
        assertFalse("the burst did not leave the app", rule.activity.isFinishing)
    }

    private companion object {
        const val FRAME_MS = 4L
        const val STEPS = 200
    }
}
