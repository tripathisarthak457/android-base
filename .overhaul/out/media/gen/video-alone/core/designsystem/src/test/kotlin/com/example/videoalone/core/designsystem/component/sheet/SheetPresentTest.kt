package com.example.videoalone.core.designsystem.component.sheet

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import com.example.videoalone.core.designsystem.TestSdk
import com.example.videoalone.core.designsystem.component.overlay.LocalOverlayHost
import com.example.videoalone.core.designsystem.component.overlay.OverlayHostState
import com.example.videoalone.core.designsystem.component.sheet.content.SheetColumn
import com.example.videoalone.core.designsystem.theme.AppTheme
import com.example.videoalone.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** B-1 to B-8: how a sheet arrives, turns around, and leaves with its caller. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class SheetPresentTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val screen = SheetTestScreen()

    private fun settle() {
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
    }

    @Test
    fun `B-1 B-2 the sheet appears hidden and its first moving frame shows little of the travel`() {
        rule.mainClock.autoAdvance = false
        rule.setContent { screen.Content() }
        val offsets = mutableListOf<Float>()
        repeat(12) {
            rule.mainClock.advanceTimeByFrame()
            offsets += screen.state.offset
        }
        val stops = screen.state.stops!!
        val travel = stops.hidden - stops.offsetOf(SheetDetent.Medium)
        val first = offsets.indexOfFirst { !it.isNaN() && it < stops.hidden }
        assertTrue("never moved: $offsets", first >= 0)
        assertTrue("hidden before it moves: $offsets", offsets.take(first).all { it.isNaN() || it == stops.hidden })
        assertTrue("first moving frame ${offsets[first]} of $travel", stops.hidden - offsets[first] <= travel * 0.04f)
    }

    @Test
    fun `B-3 showing it again while it leaves turns it around without asking the caller`() {
        rule.setContent { screen.Content() }
        settle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { screen.visible = false }
        rule.mainClock.advanceTimeBy(80)
        rule.runOnIdle { screen.visible = true }
        rule.mainClock.autoAdvance = true
        settle()
        assertTrue(screen.state.isVisible)
        assertEquals(SheetDetent.Medium, screen.state.currentDetent)
        assertEquals(0, screen.dismissRequests)
        assertEquals(0, screen.hiddenCalls)
    }

    @Test
    fun `B-3 onHidden runs once for each close`() {
        rule.setContent { screen.Content() }
        settle()
        repeat(2) {
            rule.runOnIdle { screen.visible = false }
            settle()
            rule.runOnIdle { screen.visible = true }
            settle()
        }
        assertEquals(2, screen.hiddenCalls)
    }

    @Test
    fun `B-4a a call site that leaves composition takes its sheet out of the stack at once`() {
        var composed by mutableStateOf(true)
        var host: OverlayHostState? = null
        rule.setContent {
            AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
                host = LocalOverlayHost.current
                if (composed) {
                    AppSheet(visible = true, onDismissRequest = {}) {
                        SheetColumn { Box(Modifier.fillMaxWidth().height(200.dp)) }
                    }
                }
            }
        }
        settle()
        assertTrue(host!!.sheetStack.front(1) != null)
        rule.mainClock.autoAdvance = false
        // With the clock held, the harness hands the write to the recomposer only when told; an app's
        // main thread does it before the next frame.
        rule.runOnIdle {
            composed = false
            Snapshot.sendApplyNotifications()
        }
        rule.mainClock.advanceTimeByFrame()
        assertEquals(null, host!!.sheetStack.front(1))
        rule.onAllNodesWithTag("sheet_surface").assertCountEquals(0)
    }

    @Test
    fun `B-6 a sheet whose caller does not show it after a restore stays away`() {
        screen.visible = false
        rule.setContent { screen.Content() }
        settle()
        assertFalse(screen.state.isVisible)
        rule.onAllNodesWithTag("sheet_surface").assertCountEquals(0)
    }
}
