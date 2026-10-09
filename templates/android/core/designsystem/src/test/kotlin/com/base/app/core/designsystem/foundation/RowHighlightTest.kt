package com.base.app.core.designsystem.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.TEST_SDK
import com.base.app.core.designsystem.setThemed
import com.base.app.core.designsystem.theme.AppColors
import com.base.app.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RowHighlightTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a held row shows the highlight, and it is gone after the finger lifts`() {
        lateinit var colors: AppColors
        rule.setThemed {
            colors = AppTheme.colors
            Box(
                Modifier
                    .testTag("row")
                    .background(colors.surface)
                    .pressable(onClick = {}, feedback = PressFeedback.Highlight),
            ) {
                Box(Modifier.size(width = 200.dp, height = 60.dp))
            }
        }
        fun corner() = rule.onNodeWithTag("row").captureToImage().toPixelMap()[4, 4].toArgb()
        val resting = corner()
        assertEquals(colors.surface.toArgb(), resting)

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("row").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(300)
        assertNotEquals("the highlight shows while the finger is down", resting, corner())

        rule.onNodeWithTag("row").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals("the highlight fades once the finger lifts", resting, corner())
    }
}
