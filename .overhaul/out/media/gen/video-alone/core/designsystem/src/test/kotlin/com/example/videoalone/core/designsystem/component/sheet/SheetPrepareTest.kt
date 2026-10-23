package com.example.videoalone.core.designsystem.component.sheet

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.example.videoalone.core.designsystem.RecompositionCounter
import com.example.videoalone.core.designsystem.TestSdk
import com.example.videoalone.core.designsystem.component.button.AppButton
import com.example.videoalone.core.designsystem.component.sheet.choice.AppChoiceSheet
import com.example.videoalone.core.designsystem.component.sheet.choice.AppChoiceSheetDefaults
import com.example.videoalone.core.designsystem.component.text.AppText
import com.example.videoalone.core.designsystem.foundation.layout.LocalTabVisible
import com.example.videoalone.core.designsystem.foundation.layout.TabVisibility
import com.example.videoalone.core.designsystem.theme.AppTheme
import com.example.videoalone.core.designsystem.theme.ThemeMode
import kotlin.math.abs
import kotlin.math.ceil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * District P3: a list sheet's first frame is paid while the finger is down on the control that opens
 * it, a slice per frame, and a hidden sheet that stays composed is inert.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class SheetPrepareTest {

    @get:Rule
    val counter = RecompositionCounter()

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val screen = PrepareTestScreen()

    private fun show(content: @Composable () -> Unit = { screen.Content() }) {
        rule.mainClock.autoAdvance = false
        rule.setContent(content)
        frames(120)
        counter.reset()
    }

    /**
     * One frame, after handing the recomposer what the test and the input wrote: with the clock held,
     * the harness does that only when told, where an app's main thread does it before every frame.
     */
    private fun frame() {
        rule.runOnUiThread { Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeByFrame()
    }

    private fun frames(count: Int) = repeat(count) { frame() }

    /** The rows composed in each of the next [count] frames. */
    private fun rowsPerFrame(count: Int): List<Int> = List(count) {
        val before = counter.count(ROW)
        frame()
        counter.count(ROW) - before
    }

    private fun press() = rule.onNodeWithTag(TRIGGER).performTouchInput { down(center) }

    private fun lift() = rule.onNodeWithTag(TRIGGER).performTouchInput { up() }

    @Test
    fun `a press composes the sheet's rows before the tap ends`() {
        show()

        press()
        frames(8)

        assertTrue(counter.count(ROW) > 0)
        assertFalse(screen.state.isVisible)
    }

    @Test
    fun `the press spreads the rows over frames`() {
        show()

        press()
        val perFrame = rowsPerFrame(30).dropWhile { it == 0 }

        assertTrue("never composed a row", perFrame.isNotEmpty())
        assertTrue("first frame $perFrame", perFrame.first() <= ceil(160.0 / ROW_DP).toInt() + 1)
        assertTrue("later frames $perFrame", perFrame.drop(1).all { it <= ceil(320.0 / ROW_DP).toInt() + 1 })
    }

    @Test
    fun `a prepared sheet opens with no row left to compose`() {
        show()
        val clicked = firstMovingFrame { rule.onNodeWithTag(TRIGGER).performSemanticsAction(SemanticsActions.OnClick) }
        screen.visible = false
        frames(120)

        press()
        frames(30)
        counter.reset()
        val pressed = firstMovingFrame { lift() }

        assertEquals(0, counter.count(ROW))
        // An unprepared open composes and lays out its rows in the frame after the tap and moves a frame
        // later; on a phone that composing frame is the one a list sheet drops.
        assertTrue("first moving frame: pressed $pressed, clicked $clicked", pressed in 0 until clicked)
    }

    /** The index of the first frame after [open] in which the sheet has left its hidden offset. */
    private fun firstMovingFrame(open: () -> Unit): Int {
        open()
        val offsets = List(12) {
            frame()
            screen.state.offset
        }
        val hidden = offsets.firstOrNull { !it.isNaN() } ?: return -1
        return offsets.indexOfFirst { !it.isNaN() && it < hidden - 0.5f }.also {
            assertTrue("never moved: $offsets", it >= 0)
        }
    }

    @Test
    fun `a press that becomes a scroll lets go`() {
        show { screen.Content(inScroll = true) }

        rule.onNodeWithTag(TRIGGER).performTouchInput {
            down(center)
            repeat(3) { moveBy(Offset(0f, -viewConfiguration.touchSlop)) }
        }
        frames(3)
        assertFalse(screen.state.preparing)
        frames(2)
        assertNull(screen.state.host)
    }

    @Test
    fun `a press that ends without opening lets go after the grace`() {
        screen.opens = false
        show()

        press()
        frames(2)
        lift()
        rule.mainClock.advanceTimeBy(399)
        assertNotNull(screen.state.host)
        rule.mainClock.advanceTimeBy(1)
        frames(2)
        assertNull(screen.state.host)
    }

    @Test
    fun `a click without a press opens as today`() {
        show()

        assertEquals(0, counter.count(ROW))
        rule.onNodeWithTag(TRIGGER).performSemanticsAction(SemanticsActions.OnClick)
        frames(120)

        assertTrue(screen.state.isVisible)
        assertTrue(counter.count(ROW) > 0)
    }

    @Test
    fun `the caller does not recompose while the sheet prepares`() {
        show()

        press()
        frames(1)
        val atDown = counter.count(CALLER)
        frames(20)

        assertTrue("the caller composed $atDown times at the down", atDown <= 1)
        assertEquals(atDown, counter.count(CALLER))
    }

    @Test
    fun `a kept sheet composes nothing until it first opens`() {
        screen.visible = false
        screen.keepContent = true
        show()

        assertEquals(0, counter.count(ROW))
        assertNull(screen.state.host)
    }

    @Test
    fun `a hidden sheet is inert`() {
        screen.keepContent = true
        show()
        screen.visible = true
        frames(120)
        screen.visible = false
        frames(120)
        assertNotNull("a kept sheet stays composed", screen.state.host)

        rule.onAllNodesWithTag(ROW_TAG).assertCountEquals(0)
        assertEquals(false, screen.contentVisibility?.isVisible)
        rule.onNodeWithTag(AFTER).requestFocus()
        frames(1)
        rule.runOnUiThread { screen.focusManager.moveFocus(FocusDirection.Next) }
        frames(2)
        // Past the last control, focus goes round to the first; the hidden sheet's surface is no stop.
        val focused = rule.onAllNodes(isFocused()).fetchSemanticsNodes().map { it.config.getOrNull(SemanticsProperties.TestTag) }
        assertTrue("focus went to $focused", focused.all { it == TRIGGER || it == AFTER })
    }

    @Test
    fun `a fitted sheet rests where it would without a press`() {
        val choice = ChoiceScreen()
        show { choice.Content() }

        rule.onNodeWithTag(TRIGGER).performSemanticsAction(SemanticsActions.OnClick)
        frames(120)
        val clicked = choice.state.offset
        choice.visible = false
        frames(120)

        rule.onNodeWithTag(TRIGGER).performTouchInput { down(center) }
        frames(30)
        rule.onNodeWithTag(TRIGGER).performTouchInput { up() }
        frames(120)

        assertTrue("clicked $clicked, pressed ${choice.state.offset}", abs(choice.state.offset - clicked) <= 1f)
    }

    @Test
    fun `a choice sheet takes its trigger's state`() {
        val choice = ChoiceScreen()
        show { choice.Content() }

        rule.onNodeWithTag(TRIGGER).performTouchInput { down(center) }
        frames(8)

        assertTrue(choice.state.host != null)
        assertFalse(choice.state.isVisible)
    }

    private fun SemanticsNodeInteractionCollection.assertCountEquals(expected: Int) {
        assertEquals(expected, fetchSemanticsNodes().size)
    }

    private class PrepareTestScreen {
        var visible by mutableStateOf(false)
        var keepContent by mutableStateOf(false)
        var opens = true
        var contentVisibility: TabVisibility? = null
        lateinit var state: AppSheetState
        lateinit var focusManager: FocusManager

        @Composable
        fun Content(inScroll: Boolean = false) {
            AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
                PrepareCaller(inScroll)
            }
        }

        @Composable
        private fun PrepareCaller(inScroll: Boolean) {
            state = rememberAppSheetState()
            focusManager = LocalFocusManager.current
            Column(if (inScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier) {
                AppButton(
                    text = "Open",
                    onClick = { if (opens) visible = true },
                    modifier = Modifier.testTag(TRIGGER).sheetTrigger(state),
                )
                AppButton(text = "After", onClick = {}, modifier = Modifier.testTag(AFTER))
                if (inScroll) Spacer(Modifier.height(2000.dp))
            }
            AppSheet(
                visible = visible,
                onDismissRequest = { visible = false },
                state = state,
                properties = AppSheetProperties(keepContent = keepContent),
            ) {
                contentVisibility = LocalTabVisible.current
                LazyColumn(contentPadding = contentPadding) {
                    items(60, key = { it }, contentType = { "row" }) { index -> PrepRow(index) }
                }
            }
        }
    }

    private class ChoiceScreen {
        var visible by mutableStateOf(false)
        lateinit var state: AppSheetState

        @Composable
        fun Content() {
            AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
                state = AppChoiceSheetDefaults.rememberState()
                AppButton(
                    text = "Open",
                    onClick = { visible = true },
                    modifier = Modifier.testTag(TRIGGER).sheetTrigger(state),
                )
                AppChoiceSheet(
                    visible = visible,
                    choices = List(8) { "Choice $it" },
                    selected = null,
                    onSelect = {},
                    onDismissRequest = { visible = false },
                    label = { it },
                    state = state,
                )
            }
        }
    }

    private companion object {
        const val TRIGGER = "prepare_trigger"
        const val AFTER = "prepare_after"
        const val ROW = "PrepRow"
        const val ROW_TAG = "prep_row"
        const val CALLER = "PrepareCaller"
        const val ROW_DP = 48.0
    }
}

@Composable
private fun PrepRow(index: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("prep_row"),
    ) {
        AppText(text = "Row $index")
    }
}
