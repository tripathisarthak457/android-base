package com.example.standard.core.designsystem.component.sheet.gesture

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.example.standard.core.designsystem.TestSdk
import com.example.standard.core.designsystem.component.list.AppRow
import com.example.standard.core.designsystem.component.sheet.AppSheet
import com.example.standard.core.designsystem.component.sheet.AppSheetProperties
import com.example.standard.core.designsystem.component.sheet.AppSheetState
import com.example.standard.core.designsystem.component.sheet.SheetContentInteraction
import com.example.standard.core.designsystem.component.sheet.SheetDetent
import com.example.standard.core.designsystem.component.sheet.rememberAppSheetState
import com.example.standard.core.designsystem.theme.AppTheme
import com.example.standard.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** B-27 to B-30: what a finger scrolling the content does to the sheet around it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp-mdpi")
class SheetNestedScrollTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var state: AppSheetState
    private lateinit var list: LazyListState
    private lateinit var scope: CoroutineScope

    private fun showSheet(interaction: SheetContentInteraction) {
        rule.setContent {
            AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
                scope = rememberCoroutineScope()
                state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large))
                list = rememberLazyListState()
                AppSheet(
                    visible = true,
                    onDismissRequest = {},
                    state = state,
                    properties = AppSheetProperties(contentInteraction = interaction),
                ) {
                    LazyColumn(Modifier.testTag("list"), state = list, contentPadding = contentPadding) {
                        items(60, key = { it }) { AppRow(title = "Row $it") }
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

    private fun swipeList(dy: Float, durationMillis: Long) {
        rule.onNodeWithTag("list").performTouchInput {
            swipe(Offset(centerX, 100f), Offset(centerX, 100f + dy), durationMillis)
        }
        settle()
    }

    private fun listAtTop() = list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0

    @Test
    fun `B-28 with Resizes a scroll up grows the sheet before the list moves`() {
        showSheet(SheetContentInteraction.Resizes)
        swipeList(dy = -250f, durationMillis = 600)
        assertEquals(SheetDetent.Large, state.currentDetent)
        assertTrue("the list scrolled while the sheet grew", listAtTop())
    }

    @Test
    fun `B-29 with Scrolls a scroll up moves the list and leaves the sheet where it is`() {
        showSheet(SheetContentInteraction.Scrolls)
        swipeList(dy = -250f, durationMillis = 600)
        assertEquals(SheetDetent.Medium, state.currentDetent)
        assertTrue("the list did not scroll", !listAtTop())
    }

    @Test
    fun `B-28 a pull down at the list's top moves the sheet down`() {
        showSheet(SheetContentInteraction.Resizes)
        rule.runOnIdle { scope.launch { state.snapTo(SheetDetent.Large) } }
        settle()
        swipeList(dy = 320f, durationMillis = 900)
        assertEquals(SheetDetent.Medium, state.currentDetent)
        assertTrue(listAtTop())
    }
}
