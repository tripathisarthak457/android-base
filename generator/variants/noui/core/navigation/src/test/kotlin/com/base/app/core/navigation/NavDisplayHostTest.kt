package com.base.app.core.navigation

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.base.app.core.common.navigation.AppNavKey
import java.util.Optional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The one-stack host: navigation commands, no fade between screens, system Back, a deep link, and
 * process death.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class NavDisplayHostTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val registry = NavRegistry(testGraphs())
    private val navigator = AppNavigator()
    private val pending = PendingDeepLink(
        routes = setOf(
            DeepLinkRoute { link ->
                link.segments.getOrNull(1)?.toIntOrNull()?.let(::Post)
            },
        ),
        origin = Optional.of(LinkOrigin(scheme = "app", host = "app.example.com")),
    )
    private var stack: AppBackStack? = null

    private fun host(restoration: StateRestorationTester? = null) {
        val content = @Composable {
            val backStack =
                rememberAppBackStack(startKey = FeedTab, serialization = testSerialization)
            stack = backStack
            AppNavigationHost(backStack = backStack, registry = registry, navigator = navigator)
            OpenDeepLinks(stack = backStack, pending = pending)
        }
        if (restoration != null) restoration.setContent(content) else rule.setContent(content)
    }

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun `a push shows the new screen over the start screen, and Up returns`() {
        host()
        rule.runOnIdle { navigator.navigate(Post(1)) }
        rule.waitForIdle()
        assertEquals(listOf(FeedTab, Post(1)), stack!!.keys)
        rule.onNodeWithTagExists(Post(1))

        rule.runOnIdle { navigator.navigateUp() }
        rule.waitForIdle()
        assertEquals(listOf<AppNavKey>(FeedTab), stack!!.keys)
        rule.onNodeWithTagExists(FeedTab)
    }

    @Test
    fun `a push replaces the screen at once, never fading the two together`() {
        host()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        rule.runOnUiThread { stack!!.applyNow(NavCommand.Navigate(Post(1))) }
        // Six frames, 0.1 s: Navigation 3's default fade would still draw both screens, 0.7 s long.
        // The clock is paused, so each frame waits for the looper to hand the change to Compose.
        repeat(6) {
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
        }

        rule.onNodeWithTagExists(Post(1))
        rule.onNodeWithTag(FeedTab.toString(), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `a destination already on top is not pushed twice`() {
        host()
        rule.runOnIdle {
            navigator.navigate(Post(1))
            navigator.navigate(Post(1))
        }
        rule.waitForIdle()
        assertEquals(listOf(FeedTab, Post(1)), stack!!.keys)
    }

    @Test
    fun `system Back pops one screen at a time`() {
        host()
        rule.runOnIdle {
            navigator.navigate(Post(1))
            navigator.navigate(Post(2))
        }
        rule.waitForIdle()

        back()
        assertEquals(listOf(FeedTab, Post(1)), stack!!.keys)
        back()
        assertEquals(listOf<AppNavKey>(FeedTab), stack!!.keys)
        rule.onNodeWithTagExists(FeedTab)
    }

    @Test
    fun `a deep link opens on top of the start screen`() {
        host()
        rule.runOnIdle { pending.hold(Uri.parse("app://posts/7")) }
        rule.waitForIdle()
        assertEquals(listOf(FeedTab, Post(7)), stack!!.keys)
        rule.onNodeWithTagExists(Post(7))

        back()
        assertEquals(listOf<AppNavKey>(FeedTab), stack!!.keys)
    }

    @Test
    fun `the stack survives process death, and Back still works`() {
        val restoration = StateRestorationTester(rule)
        host(restoration)
        rule.runOnIdle { navigator.navigate(Post(1)) }
        rule.waitForIdle()
        val before = stack

        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        assertNotSame("the stack was rebuilt from what was saved", before, stack)
        assertEquals(listOf(FeedTab, Post(1)), stack!!.keys)
        rule.onNodeWithTagExists(Post(1))
        back()
        assertEquals(listOf<AppNavKey>(FeedTab), stack!!.keys)
    }
}

private fun ComposeTestRule.onNodeWithTagExists(key: AppNavKey) {
    onNodeWithTag(key.toString(), useUnmergedTree = true).assertExists()
}
