package com.example.everything.core.navigation.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import com.example.everything.core.common.navigation.AppNavKey
import com.example.everything.core.designsystem.theme.AppTheme
import com.example.everything.core.navigation.AppNavigator
import com.example.everything.core.navigation.AppShell
import com.example.everything.core.navigation.Compose
import com.example.everything.core.navigation.FeedTab
import com.example.everything.core.navigation.NavCommand
import com.example.everything.core.navigation.ROBOLECTRIC_SDK
import com.example.everything.core.navigation.ShellState
import com.example.everything.core.navigation.back.submit
import com.example.everything.core.navigation.rememberShellState
import com.example.everything.core.navigation.screenNode
import com.example.everything.core.navigation.testRegistry
import com.example.everything.core.navigation.testSerialization
import com.example.everything.core.navigation.testTabs
import com.example.everything.core.navigation.texturesAround
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A back gesture over a cover shrinks it into the back card over the shell. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class PresentationCardTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val input = DirectNavigationEventInput()
    private lateinit var state: ShellState

    private fun showCover() {
        val registry = testRegistry()
        rule.setContent {
            AppTheme {
                val shell = rememberShellState(testTabs, registry, testSerialization, generation = 1)
                state = shell
                AppShell(tabs = testTabs, state = shell, registry = registry, navigator = AppNavigator())
            }
        }
        rule.runOnIdle {
            rule.activity.navigationEventDispatcher.addInput(input)
            state.submit(NavCommand.Navigate(Compose))
        }
        rule.waitForIdle()
    }

    private fun swipe(vararg progress: Float) {
        rule.runOnIdle {
            input.backStarted(event(0f))
            progress.forEach { input.backProgressed(event(it)) }
        }
        rule.waitForIdle()
    }

    private fun event(progress: Float) = NavigationEvent(
        swipeEdge = NavigationEvent.EDGE_LEFT,
        progress = progress,
        touchX = progress * 400f,
        touchY = 600f,
        frameTimeMillis = rule.mainClock.currentTime,
    )

    private fun bounds(key: AppNavKey): Rect =
        rule.onNodeWithTag(key.toString(), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    @Test
    fun `a back swipe shrinks the cover about its centre`() {
        showCover()
        val width = rule.onRoot().fetchSemanticsNode().boundsInRoot.width

        swipe(0.448f)

        assertEquals(0.9135f, bounds(Compose).width / width, 0.002f)
        rule.onNodeWithTag(FeedTab.toString(), useUnmergedTree = true).assertExists()
    }

    @Test
    fun `releasing dismisses the cover once, and the tab is left as it was`() {
        showCover()

        swipe(0.3f, 0.6f)
        rule.runOnIdle { input.backCompleted() }
        rule.waitForIdle()

        assertEquals(emptyList<AppNavKey>(), state.presentedKeys)
        assertEquals(listOf<AppNavKey>(FeedTab), state.current.keys)
        rule.onNodeWithTag(Compose.toString(), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `cancelling keeps the cover, at full size`() {
        showCover()
        val width = rule.onRoot().fetchSemanticsNode().boundsInRoot.width

        swipe(0.5f, 0.2f, 0f)
        rule.runOnIdle { input.backCancelled() }
        rule.waitForIdle()

        assertEquals(listOf<AppNavKey>(Compose), state.presentedKeys)
        assertEquals(width, bounds(Compose).width, 0.5f)
    }

    @Test
    fun `the cover under the back card is a texture in its innermost layer while the card is up`() {
        showCover()
        val cover = rule.screenNode(Compose)
        assertEquals("before the gesture", emptyList<Int>(), rule.texturesAround(cover))

        swipe(0.3f, 0.6f)
        assertEquals("while the card is up, 0 the innermost layer", listOf(0), rule.texturesAround(cover))

        rule.runOnIdle {
            input.backProgressed(event(0.2f))
            input.backProgressed(event(0f))
            input.backCancelled()
        }
        rule.waitForIdle()
        assertEquals("after the cancel", emptyList<Int>(), rule.texturesAround(cover))
    }
}
