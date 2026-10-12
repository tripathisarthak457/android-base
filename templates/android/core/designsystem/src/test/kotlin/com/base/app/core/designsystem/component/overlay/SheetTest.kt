package com.base.app.core.designsystem.component.overlay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.base.app.core.designsystem.TestSdk
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// A phone held upright: tall enough for both detents, too narrow for a form sheet.
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class SheetTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var measures = 0
    private var gone = 0
    private var underneathPopped = 0
    private val state = AppSheetState()
    private lateinit var scope: CoroutineScope

    private fun showSheet() {
        rule.setContent {
            AppTheme(reduceMotion = false) {
                scope = rememberCoroutineScope()
                // The screen beneath, at default priority: it only hears Back the sheet lets through.
                NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None)) { underneathPopped++ }
                AppSheet(state = state, onDismiss = { gone++ }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .layout { measurable, constraints ->
                                measures++
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            },
                    )
                }
            }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
    }

    @Test
    fun `a second Back while the sheet slides away is ignored, so it goes once and nothing beneath pops`() {
        showSheet()
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread {
            rule.activity.onBackPressedDispatcher.onBackPressed()
            rule.activity.onBackPressedDispatcher.onBackPressed()
        }
        rule.mainClock.advanceTimeBy(80)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        assertEquals(1, gone)
        assertEquals(0, underneathPopped)
    }

    @Test
    fun `dismissing twice runs the callback once`() {
        showSheet()
        var callbacks = 0
        rule.runOnIdle {
            scope.launch { state.dismiss { callbacks++ } }
            scope.launch { state.dismiss { callbacks++ } }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        assertEquals(1, callbacks)
    }

    @Test
    fun `dragging the sheet moves its content without measuring it again`() {
        showSheet()
        val before = measures
        repeat(10) {
            rule.runOnIdle { state.dragBy(-15f) }
            rule.waitForIdle()
        }
        repeat(10) {
            rule.runOnIdle { state.dragBy(25f) }
            rule.waitForIdle()
        }
        assertEquals(before, measures)
    }

    @Test
    fun `pulling up past the large detent gives less the further it goes, and lets go where it was`() {
        showSheet()
        val large = state.anchors!!.large
        val moved = mutableListOf<Float>()
        rule.runOnIdle {
            while (state.offset > large) state.dragBy(-100f)
            repeat(10) { moved += -state.dragBy(-100f) }
        }
        assertTrue(state.offset < large)
        assertTrue("each pull past the top moves the sheet less: $moved", moved.zipWithNext().all { (a, b) -> b < a })
        assertTrue(moved.last() < 25f)
        rule.runOnIdle { state.dragBy(10_000f) }
        assertEquals(state.anchors!!.hidden, state.offset, 0.5f)
    }

    @Test
    fun `a flick down from the medium detent closes the sheet, a slow release settles back`() {
        showSheet()
        val medium = state.anchors!!.medium
        rule.runOnIdle { state.dragBy(40f) }
        var closed = false
        rule.runOnIdle { scope.launch { closed = state.settle(velocity = 300f) } }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        assertEquals(false, closed)
        assertEquals(medium, state.offset, 1f)
        rule.runOnIdle { state.dragBy(40f) }
        rule.runOnIdle { scope.launch { closed = state.settle(velocity = 9_000f) } }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        assertEquals(true, closed)
    }

    @Test
    fun `the large fraction is zero at the medium detent and one at large`() {
        showSheet()
        rule.runOnIdle { assertEquals(0f, state.largeFraction, 0.001f) }
        rule.runOnIdle { scope.launch { state.animateTo(SheetDetent.Large) } }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(1f, state.largeFraction, 0.001f) }
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `with room on every side the sheet is a card in the middle that nothing recedes behind`() {
        showSheet()
        val anchors = state.anchors!!
        assertTrue(anchors.form)
        assertEquals(anchors.large, state.offset, 1f)
        assertTrue(anchors.large > 0f && anchors.large < anchors.hidden / 2f)
        rule.runOnIdle { assertEquals(0f, state.largeFraction, 0f) }
    }
}
