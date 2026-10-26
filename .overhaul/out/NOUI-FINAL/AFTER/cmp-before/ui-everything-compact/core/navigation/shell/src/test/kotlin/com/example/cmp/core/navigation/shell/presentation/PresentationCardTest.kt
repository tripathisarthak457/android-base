package com.example.cmp.core.navigation.shell.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import com.example.cmp.core.common.navigation.AppNavKey
import com.example.cmp.core.designsystem.theme.AppTheme
import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.navigation.NavCommand
import com.example.cmp.core.navigation.back.submit
import com.example.cmp.core.navigation.shell.AppShell
import com.example.cmp.core.navigation.shell.Compose
import com.example.cmp.core.navigation.shell.FeedTab
import com.example.cmp.core.navigation.shell.ROBOLECTRIC_SDK
import com.example.cmp.core.navigation.shell.ShellState
import com.example.cmp.core.navigation.shell.rememberShellState
import com.example.cmp.core.navigation.shell.screenNode
import com.example.cmp.core.navigation.shell.testRegistry
import com.example.cmp.core.navigation.shell.testSerialization
import com.example.cmp.core.navigation.shell.testTabs
import com.example.cmp.core.navigation.shell.texturesAround
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
