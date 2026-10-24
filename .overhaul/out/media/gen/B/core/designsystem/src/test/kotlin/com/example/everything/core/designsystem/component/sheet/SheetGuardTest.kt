package com.example.everything.core.designsystem.component.sheet

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.everything.core.designsystem.TestSdk
import com.example.everything.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.example.everything.core.designsystem.component.sheet.content.SheetColumn
import com.example.everything.core.designsystem.component.sheet.content.SheetDismissConfirmation
import com.example.everything.core.designsystem.component.sheet.content.SheetDismissGuard
import com.example.everything.core.designsystem.component.sheet.surface.ScrimTag
import com.example.everything.core.designsystem.theme.AppTheme
import com.example.everything.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** B-33, B-34: a guard turns every way the person closes a sheet into one attempt; code still closes it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class SheetGuardTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var visible by mutableStateOf(true)
    private var guarded by mutableStateOf(true)
    private val attempts = mutableListOf<SheetDismissSource>()
    private var requests = 0
    private var poppedBeneath = 0
    private var discarded = 0
    private lateinit var scope: AppSheetScope

    private fun showSheet(dismissible: Boolean = true, confirmation: Boolean = false) {
        rule.setContent {
            AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
                NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None)) { poppedBeneath++ }
                AppSheet(
                    visible = visible,
                    onDismissRequest = {
                        requests++
                        visible = false
                    },
                    state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
                    dismissible = dismissible,
                    header = { AppSheetHeader(title = "Form") },
                ) {
                    scope = this
                    if (confirmation) {
                        SheetDismissConfirmation(enabled = guarded, onDiscard = { discarded++ })
                    } else {
                        SheetDismissGuard(enabled = guarded) { attempts += it }
                    }
                    SheetColumn { Box(Modifier.fillMaxWidth().height(300.dp)) }
                }
            }
        }
        settle()
    }

    private fun settle() {
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
    }

    private fun assertStayed(vararg expected: SheetDismissSource) {
        settle()
        assertEquals(expected.toList(), attempts)
        assertEquals(0, requests)
        assertEquals(0, poppedBeneath)
        assertTrue(visible)
    }

    @Test
    fun `B-34 Back is one attempt and pops nothing beneath`() {
        showSheet()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertStayed(SheetDismissSource.Back)
    }

    @Test
    fun `B-34 a tap on the dim is one attempt`() {
        showSheet()
        rule.onNodeWithTag(ScrimTag).performTouchInput { click(Offset(centerX, 40f)) }
        assertStayed(SheetDismissSource.Scrim)
    }

    @Test
    fun `B-34 the close button is one attempt`() {
        showSheet()
        rule.onNodeWithContentDescription("Close").performClick()
        assertStayed(SheetDismissSource.Request)
    }

    @Test
    fun `B-42 Escape is one attempt`() {
        showSheet()
        rule.onNodeWithTag("sheet_surface").performKeyInput { pressKey(Key.Escape) }
        assertStayed(SheetDismissSource.Escape)
    }

    @Test
    fun `B-22 a flick that would close the sheet is one attempt, and it stays on its detent`() {
        showSheet()
        rule.onNodeWithTag("sheet_grabber").performTouchInput {
            swipe(center, Offset(centerX, centerY + 600f), durationMillis = 80)
        }
        assertStayed(SheetDismissSource.Drag)
    }

    @Test
    fun `B-33 Done after a save closes a guarded sheet`() {
        showSheet()
        rule.runOnIdle { scope.dismiss() }
        settle()
        assertEquals(emptyList<SheetDismissSource>(), attempts)
        assertEquals(1, requests)
    }

    @Test
    fun `B-34 a disabled guard registers nothing, so Back closes the sheet`() {
        guarded = false
        showSheet()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
        assertEquals(1, requests)
        assertEquals(0, poppedBeneath)
    }

    @Test
    fun `B-37 a modal sheet the person cannot close consumes Back`() {
        guarded = false
        showSheet(dismissible = false)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
        assertEquals(0, requests)
        assertEquals(0, poppedBeneath)
        assertTrue(visible)
    }

    @Test
    fun `B-34 the confirmation asks over the sheet and Discard discards`() {
        showSheet(confirmation = true)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
        rule.onNodeWithText("Discard").performClick()
        settle()
        assertEquals(1, discarded)
        assertEquals(0, poppedBeneath)
    }
}
