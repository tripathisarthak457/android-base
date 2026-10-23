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
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.example.videoalone.core.designsystem.TestSdk
import com.example.videoalone.core.designsystem.component.sheet.content.SheetColumn
import com.example.videoalone.core.designsystem.theme.AppTheme
import com.example.videoalone.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** B-63: with motion removed a sheet arrives and leaves in one frame. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class SheetReduceMotionTest {

    /** "Remove animations": Compose follows the system's animator scale, which a test sets here. */
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>(RemovedAnimations)

    private var visible by mutableStateOf(true)
    private lateinit var state: AppSheetState

    @Test
    fun `B-63 the sheet reaches its detent and leaves within a few frames`() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme(mode = ThemeMode.Light, reduceMotion = true) {
                state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large))
                AppSheet(visible = visible, onDismissRequest = { visible = false }, state = state) {
                    SheetColumn { Box(Modifier.fillMaxWidth().height(200.dp)) }
                }
            }
        }
        repeat(FramesToArrive) { rule.mainClock.advanceTimeByFrame() }
        val stops = state.stops!!
        assertEquals(stops.offsetOf(SheetDetent.Medium), state.offset, 0.5f)
        // With the clock held, the harness hands the write to the recomposer only when told; an app's
        // main thread does it before the next frame.
        rule.runOnIdle {
            visible = false
            Snapshot.sendApplyNotifications()
        }
        repeat(FramesToArrive) { rule.mainClock.advanceTimeByFrame() }
        assertFalse(state.isVisible)
    }

    private object RemovedAnimations : MotionDurationScale {
        override val scaleFactor: Float = 0f
    }

    private companion object {
        /** The sheet composes a frame after its caller and starts moving on the frame after that. */
        const val FramesToArrive = 5
    }
}
