package com.example.finalafter.crui2.core.navigation.shell.back

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import com.example.finalafter.crui2.core.common.navigation.AppNavKey
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import com.example.finalafter.crui2.core.navigation.AppBackStack
import com.example.finalafter.crui2.core.navigation.NavCommand
import com.example.finalafter.crui2.core.navigation.back.StackChange
import com.example.finalafter.crui2.core.navigation.shell.FeedTab
import com.example.finalafter.crui2.core.navigation.shell.Post
import com.example.finalafter.crui2.core.navigation.shell.ROBOLECTRIC_SDK
import com.example.finalafter.crui2.core.navigation.shell.Screen
import com.example.finalafter.crui2.core.navigation.shell.stack.StackHost
import com.example.finalafter.crui2.core.navigation.shell.testRegistry
import com.example.finalafter.crui2.core.navigation.shell.transition.ScreenMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Gmail-style back card: what a back gesture shows, and what its release and cancel do to the stack. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class BackCardTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val input = DirectNavigationEventInput()
    private var rootCompositions = 0

    private val registry = testRegistry { key ->
        if (key == FeedTab) remember { rootCompositions++ }
        Screen(key)
    }

    private fun show(stack: AppBackStack, direction: LayoutDirection = LayoutDirection.Ltr) {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                AppTheme { StackHost(stack = stack, registry = registry, motion = ScreenMotion()) }
            }
        }
        rule.runOnIdle { rule.activity.navigationEventDispatcher.addInput(input) }
        rule.waitForIdle()
    }

    private fun swipe(vararg progress: Float, edge: Int = NavigationEvent.EDGE_LEFT) {
        rule.runOnIdle {
            input.backStarted(event(0f, edge))
            progress.forEach { input.backProgressed(event(it, edge)) }
        }
        rule.waitForIdle()
    }

    private fun event(progress: Float, edge: Int) = NavigationEvent(
        swipeEdge = edge,
        progress = progress,
        touchX = progress * 400f,
        touchY = 600f,
        frameTimeMillis = rule.mainClock.currentTime,
    )

    private fun bounds(key: AppNavKey): Rect =
        rule.onNodeWithTag(key.toString(), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    @Test
    fun `a back swipe shrinks the top screen about its centre, as Gmail does`() {
        show(AppBackStack(mutableStateListOf(FeedTab, Post(1))))
        val width = rule.onRoot().fetchSemanticsNode().boundsInRoot.width

        swipe(0.448f)

        val card = bounds(Post(1))
        assertEquals("scale at a 44.8 % swipe", 0.9135f, card.width / width, 0.002f)
        assertTrue("it drifts toward the far edge", card.center.x > width / 2)
    }

    @Test
    fun `each progress step is applied as it arrives, so the frame it arrives in draws it`() {
        show(AppBackStack(mutableStateListOf(FeedTab, Post(1))))
        swipe(0.2f)

        val applied = rule.runOnUiThread {
            var applied = 0
            val observer = Snapshot.registerApplyObserver { changed, _ -> if (changed.isNotEmpty()) applied++ }
            try {
                input.backProgressed(event(0.5f, NavigationEvent.EDGE_LEFT))
                applied
            } finally {
                observer.dispose()
            }
        }

        assertTrue("the step was left for Compose to apply after the frame", applied > 0)
    }

    @Test
    fun `a swipe from the right edge moves the card the other way`() {
        show(AppBackStack(mutableStateListOf(FeedTab, Post(1))))
        val width = rule.onRoot().fetchSemanticsNode().boundsInRoot.width

        swipe(0.6f, edge = NavigationEvent.EDGE_RIGHT)

        assertTrue(bounds(Post(1)).center.x < width / 2)
    }

    @Test
    fun `releasing pops once, with the screen beneath already there`() {
        val stack = AppBackStack(mutableStateListOf(FeedTab, Post(1)))
        show(stack)

        swipe(0.3f, 0.6f)
        rule.runOnIdle { input.backCompleted() }
        rule.waitForIdle()

        assertEquals(listOf<AppNavKey>(FeedTab), stack.keys)
        assertEquals("the screen beneath was not composed again", 1, rootCompositions)
    }

    @Test
    fun `cancelling never pops, and the screen returns to full size`() {
        val stack = AppBackStack(mutableStateListOf(FeedTab, Post(1)))
        show(stack)
        val width = rule.onRoot().fetchSemanticsNode().boundsInRoot.width

        swipe(0.5f, 0.2f, 0f)
        rule.runOnIdle { input.backCancelled() }
        rule.waitForIdle()

        assertEquals(listOf(FeedTab, Post(1)), stack.keys)
        assertEquals(width, bounds(Post(1)).width, 0.5f)
    }

    @Test
    fun `progress with no swipe edge is a button press, so no card and the pop slides`() {
        val stack = AppBackStack(mutableStateListOf(FeedTab, Post(1)))
        show(stack)
        val width = rule.onRoot().fetchSemanticsNode().boundsInRoot.width

        swipe(0.5f, edge = NavigationEvent.EDGE_NONE)

        assertEquals(width, bounds(Post(1)).width, 0.5f)
        rule.runOnIdle { input.backCompleted() }
        rule.waitForIdle()
        assertEquals(listOf<AppNavKey>(FeedTab), stack.keys)
    }

    @Test
    fun `a Back press during the commit lands one screen further once it ends`() {
        val stack = AppBackStack(mutableStateListOf(FeedTab, Post(1), Post(2)))
        show(stack)
        swipe(0.6f)
        rule.mainClock.autoAdvance = false

        rule.runOnIdle { input.backCompleted() }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertEquals(listOf<AppNavKey>(FeedTab), stack.keys)
    }

    @Test
    fun `a swipe during a push runs the card from where the push is`() {
        val stack = AppBackStack(mutableStateListOf(FeedTab))
        show(stack)
        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            stack.apply(NavCommand.Navigate(Post(1)))
            stack.gate.changed(StackChange.Push)
        }
        repeat(6) { rule.mainClock.advanceTimeByFrame() }

        rule.runOnIdle {
            input.backStarted(event(0f, NavigationEvent.EDGE_LEFT))
            input.backProgressed(event(0.5f, NavigationEvent.EDGE_LEFT))
        }
        rule.mainClock.advanceTimeBy(500)
        rule.runOnIdle { input.backCompleted() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertEquals(listOf<AppNavKey>(FeedTab), stack.keys)
        rule.onNodeWithTag(FeedTab.toString(), useUnmergedTree = true).assertExists()
    }

    @Test
    fun `at the root nothing takes Back, so the system can go home`() {
        show(AppBackStack(mutableStateListOf(FeedTab)))

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

        assertTrue(rule.activity.isFinishing)
    }

    @Test
    fun `before the root, Back is the app's`() {
        show(AppBackStack(mutableStateListOf(FeedTab, Post(1))))

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

        assertFalse(rule.activity.isFinishing)
    }
}
