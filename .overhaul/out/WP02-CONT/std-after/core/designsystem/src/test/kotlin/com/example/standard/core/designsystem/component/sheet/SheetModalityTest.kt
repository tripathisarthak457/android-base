package com.example.standard.core.designsystem.component.sheet

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.example.standard.core.designsystem.TestSdk
import com.example.standard.core.designsystem.component.list.AppRow
import com.example.standard.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.example.standard.core.designsystem.theme.AppTheme
import com.example.standard.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** B-51: a non-modal sheet leaves the screen usable while it rests low, and blocks above upThrough. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class SheetModalityTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var taps = 0
    private lateinit var state: AppSheetState
    private lateinit var scope: CoroutineScope

    private fun showSheet() {
        rule.setContent {
            AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
                scope = rememberCoroutineScope()
                state = rememberAppSheetState(listOf(SheetDetent.Peek, SheetDetent.Medium, SheetDetent.Large))
                Box(Modifier.fillMaxSize().testTag("screen").clickable { taps++ })
                AppSheet(
                    visible = true,
                    onDismissRequest = {},
                    state = state,
                    modality = SheetModality.NonModal(upThrough = SheetDetent.Medium),
                    dismissible = false,
                    header = { AppSheetHeader(title = "Places", trailing = null) },
                ) {
                    LazyColumn(contentPadding = contentPadding) {
                        items(30) { AppRow(title = "Place $it") }
                    }
                }
            }
        }
        settle()
    }

    private fun settle() {
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
    }

    private fun tapNearTop() {
        // A modal sheet hides the screen from the merged tree; the touch still lands where it is.
        rule.onNodeWithTag("screen", useUnmergedTree = true).performTouchInput { click(Offset(centerX, 4f)) }
        rule.waitForIdle()
    }

    @Test
    fun `B-51 a tap beside a sheet at its peek reaches the screen`() {
        showSheet()
        assertEquals(SheetDetent.Peek, state.currentDetent)
        tapNearTop()
        assertEquals(1, taps)
    }

    @Test
    fun `B-51 at medium the screen still takes taps`() {
        showSheet()
        rule.runOnIdle { scope.launch { state.animateTo(SheetDetent.Medium) } }
        settle()
        tapNearTop()
        assertEquals(1, taps)
    }

    @Test
    fun `B-51 above medium the sheet blocks the screen`() {
        showSheet()
        rule.runOnIdle { scope.launch { state.animateTo(SheetDetent.Large) } }
        settle()
        tapNearTop()
        assertEquals(0, taps)
    }
}
