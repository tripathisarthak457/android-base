package com.base.app.core.designsystem.component.overlay

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.TEST_SDK
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
@Config(sdk = [TEST_SDK])
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
                // Registered first, so it only hears Back the sheet lets through: the screen beneath.
                BackHandler { underneathPopped++ }
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
    fun `pulling up past the large detent resists and stops`() {
        showSheet()
        val large = state.anchors!!.large
        rule.runOnIdle { repeat(50) { state.dragBy(-100f) } }
        assertTrue(state.offset >= large - 120f)
        assertTrue(state.offset < large)
    }
}
