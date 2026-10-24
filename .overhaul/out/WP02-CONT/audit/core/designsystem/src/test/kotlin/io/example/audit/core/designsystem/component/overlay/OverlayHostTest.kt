package io.example.audit.core.designsystem.component.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.example.audit.core.designsystem.TestSdk
import io.example.audit.core.designsystem.component.button.AppButton
import io.example.audit.core.designsystem.component.feedback.AppLoadingOverlay
import io.example.audit.core.designsystem.foundation.layout.ChromeInset
import io.example.audit.core.designsystem.foundation.layout.LocalBottomChrome
import io.example.audit.core.designsystem.setThemed
import io.example.audit.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class OverlayHostTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `an alert called from a small box covers the whole window, not the box`() {
        rule.setThemed {
            Box(Modifier.fillMaxSize().testTag("window")) {
                Box(Modifier.size(40.dp)) {
                    AppAlert(visible = true, title = "Delete?", confirmLabel = "Delete", onConfirm = {}, onDismissRequest = {})
                }
            }
        }
        rule.waitForIdle()
        val window = rule.onNodeWithTag("window", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val title = rule.onNodeWithText("Delete?").getUnclippedBoundsInRoot()
        assertTrue("the alert is centred in the window", title.left > 40.dp && title.top > 40.dp)
        assertTrue(window.width > 200.dp)
    }

    @Test
    fun `confirm runs only the confirm action and the caller closes the alert`() {
        var confirmed = 0
        var dismissed = 0
        rule.setThemed {
            AppAlert(visible = true, title = "Delete?", confirmLabel = "Delete", onConfirm = { confirmed++ }, onDismissRequest = { dismissed++ }, dismissLabel = "Keep")
        }
        rule.onNodeWithText("Delete").performClick()
        assertEquals(1, confirmed)
        assertEquals(0, dismissed)
    }

    @Test
    fun `the loading overlay really blocks the screen beneath`() {
        var clicks = 0
        var busy by mutableStateOf(false)
        rule.setThemed {
            Box(Modifier.fillMaxSize()) {
                AppButton(text = "Pay", onClick = { clicks++ })
                AppLoadingOverlay(visible = busy)
            }
        }
        val button = rule.onNodeWithText("Pay").getUnclippedBoundsInRoot()
        rule.onNodeWithText("Pay").performClick()
        assertEquals(1, clicks)
        busy = true
        rule.waitForIdle()
        // The button is hidden from the semantics tree while the overlay is up, so tap where it was.
        rule.onRoot().performTouchInput {
            click(Offset(((button.left + button.right) / 2).toPx(), ((button.top + button.bottom) / 2).toPx()))
        }
        assertEquals(1, clicks)
    }

    @Test
    fun `the same toast twice restarts the one showing instead of queueing a copy`() {
        val queue = ToastQueue()
        val first = ToastMessage(text = "Saved")
        queue.show(first)
        queue.show(ToastMessage(text = "Saved"))
        assertSame(first, queue.current)
        assertEquals(1, queue.restarts)
        queue.dismiss()
        assertNull(queue.current)
    }

    @Test
    fun `a toast rests just above whatever covers the bottom edge`() {
        lateinit var host: OverlayHostState
        lateinit var bottomChrome: ChromeInset
        rule.setThemed {
            host = LocalOverlayHost.current!!
            bottomChrome = LocalBottomChrome.current
            Box(Modifier.fillMaxSize().testTag("window"))
        }
        rule.runOnIdle {
            bottomChrome.value = 80.dp
            // Shown for longer than the test runs, so it is measured where it rests.
            host.toasts.show(ToastMessage(text = "Saved", durationMillis = 600_000))
        }
        rule.waitForIdle()
        val window = rule.onNodeWithTag("window", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val toast = rule.onNodeWithText("Saved", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("the toast clears the bottom chrome: $toast in $window", toast.bottom < window.bottom - 80.dp)
        assertTrue("the toast sits near the bottom, not at the top: $toast in $window", toast.top > window.bottom - 80.dp - 120.dp)
    }

    @Test
    fun `a toast's time stands still while the app is not on screen`() {
        val lifecycle = TestLifecycle()
        var host: OverlayHostState? = null
        rule.mainClock.autoAdvance = false
        // Provided above the theme, so the toast layer the theme installs counts against it.
        rule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycle) {
                AppTheme { host = LocalOverlayHost.current }
            }
        }
        rule.runOnIdle {
            lifecycle.registry.currentState = Lifecycle.State.CREATED
            host!!.toasts.show(ToastMessage(text = "Sent", durationMillis = 1_000))
        }
        rule.mainClock.advanceTimeBy(5_000)
        assertEquals("Sent", host!!.toasts.current?.text)
        rule.runOnIdle { lifecycle.registry.currentState = Lifecycle.State.RESUMED }
        rule.mainClock.advanceTimeBy(1_500)
        assertNull(host!!.toasts.current)
    }

    @Test
    fun `suspended overlays are neither drawn, announced nor touched, then come back`() {
        var suspended by mutableStateOf(false)
        var confirmed = 0
        rule.setThemed {
            SuspendOverlays { suspended }
            AppAlert(
                visible = true,
                title = "Sign out?",
                confirmLabel = "Sign out",
                onConfirm = { confirmed++ },
                onDismissRequest = {},
                dismissLabel = "Cancel",
                destructive = true,
            )
        }
        val button = rule.onNodeWithText("Sign out").getUnclippedBoundsInRoot()

        rule.runOnIdle { suspended = true }
        rule.onNodeWithText("Sign out?").assertDoesNotExist()
        rule.onRoot().performTouchInput {
            val x = (button.left + button.right).toPx() / 2
            click(Offset(x, (button.top + button.bottom).toPx() / 2))
        }
        assertEquals("A tap reached a suspended alert.", 0, confirmed)

        rule.runOnIdle { suspended = false }
        rule.onNodeWithText("Sign out?").assertIsDisplayed()
        rule.onNodeWithText("Sign out").performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun `a toast waits out a suspension with its time held`() {
        var suspended by mutableStateOf(true)
        var host: OverlayHostState? = null
        rule.mainClock.autoAdvance = false
        rule.setThemed {
            host = LocalOverlayHost.current
            SuspendOverlays { suspended }
        }
        rule.runOnIdle { host!!.toasts.show(ToastMessage(text = "Saved", durationMillis = 1_000)) }
        rule.mainClock.advanceTimeBy(5_000)
        assertEquals("Saved", host!!.toasts.current?.text)
        rule.onNodeWithText("Saved").assertDoesNotExist()

        rule.runOnIdle { suspended = false }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Saved").assertExists()
        rule.mainClock.advanceTimeBy(1_500)
        assertNull(host!!.toasts.current)
    }

    private class TestLifecycle : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }
}
