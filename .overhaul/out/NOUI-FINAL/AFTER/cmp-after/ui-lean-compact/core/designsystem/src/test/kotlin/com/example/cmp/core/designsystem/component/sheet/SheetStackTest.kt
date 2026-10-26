package com.example.cmp.core.designsystem.component.sheet

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import com.example.cmp.core.designsystem.TestSdk
import com.example.cmp.core.designsystem.component.sheet.content.SheetColumn
import com.example.cmp.core.designsystem.theme.AppTheme
import com.example.cmp.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** B-54, B-55: a sheet opened from a sheet stacks on it, and only the front one answers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class SheetStackTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var first by mutableStateOf(true)
    private var second by mutableStateOf(false)
    private lateinit var lower: AppSheetState
    private lateinit var upper: AppSheetState

    private fun showSheets() {
        rule.setContent {
            AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
                lower = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large))
                upper = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large))
                AppSheet(visible = first, onDismissRequest = { first = false }, state = lower) {
                    SheetColumn { Box(Modifier.fillMaxWidth().height(200.dp)) }
                    AppSheet(
                        visible = second,
                        onDismissRequest = { second = false },
                        state = upper,
                    ) {
                        SheetColumn { Box(Modifier.fillMaxWidth().height(200.dp)) }
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

    @Test
    fun `B-55 the covered sheet's grabber hides and Back closes the front sheet first`() {
        showSheets()
        rule.onAllNodesWithTag("sheet_grabber").assertCountEquals(1)
        rule.runOnIdle { second = true }
        settle()
        rule.onAllNodesWithTag("sheet_grabber").assertCountEquals(1)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
        assertEquals(false, second)
        assertEquals(true, first)
        assertTrue(lower.isVisible)
    }
}
