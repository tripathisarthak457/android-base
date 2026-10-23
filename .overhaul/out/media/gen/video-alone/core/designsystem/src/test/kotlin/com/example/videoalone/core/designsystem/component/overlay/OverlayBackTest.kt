package com.example.videoalone.core.designsystem.component.overlay

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.videoalone.core.designsystem.TestSdk
import com.example.videoalone.core.designsystem.component.feedback.AppLoadingOverlay
import com.example.videoalone.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class OverlayBackTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var dismissed = 0
    private var popped = 0

    private fun pressBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun `Back closes an alert before the screen that registered after it pops`() {
        rule.setContent {
            AppTheme(reduceMotion = true) {
                AppAlert(visible = true, title = "Delete?", confirmLabel = "Delete", onConfirm = {}, onDismissRequest = { dismissed++ })
                // A screen's own handler, registered later: without overlay priority it would hear Back first.
                NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None)) { popped++ }
            }
        }
        rule.waitForIdle()
        pressBack()
        assertEquals(1, dismissed)
        assertEquals(0, popped)
    }

    @Test
    fun `an alert left up in a tab that is not on screen never takes Back`() {
        rule.setContent {
            AppTheme(reduceMotion = true) {
                NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None)) { popped++ }
                // A kept tab out of sight: the shell disables its dispatcher, and with it everything in the tab.
                val hiddenTab = rememberNavigationEventDispatcherOwner(enabled = false)
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides hiddenTab) {
                    AppAlert(visible = true, title = "Delete?", confirmLabel = "Delete", onConfirm = {}, onDismissRequest = { dismissed++ })
                }
            }
        }
        rule.waitForIdle()
        pressBack()
        assertEquals(0, dismissed)
        assertEquals(1, popped)
    }

    @Test
    fun `the loading overlay takes Back and does nothing with it`() {
        rule.setContent {
            AppTheme(reduceMotion = true) {
                NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None)) { popped++ }
                AppLoadingOverlay(visible = true)
            }
        }
        rule.waitForIdle()
        pressBack()
        assertEquals(0, popped)
    }
}
