package com.example.deskstandard.core.designsystem.component.selection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.deskstandard.core.designsystem.TestSdk
import com.example.deskstandard.core.designsystem.setThemed
import com.example.deskstandard.core.designsystem.theme.AppColors
import com.example.deskstandard.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SegmentedControlTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `the selected segment sits on a thumb, even in a page that scrolls`() {
        lateinit var colors: AppColors
        rule.setThemed {
            colors = AppTheme.colors
            // A scrolling page gives the control no height to fill, which is where the thumb went missing.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AppSegmentedControl(
                    options = listOf("Day", "Week", "Month"),
                    selected = 1,
                    onSelect = {},
                    modifier = Modifier.testTag("segmented"),
                )
            }
        }

        val image = rule.onNodeWithTag("segmented").captureToImage().toPixelMap()
        val clearOfLabels = with(rule.density) { 6.dp.roundToPx() }
        val middle = image.height / 2
        assertEquals(colors.surfaceElevated.toArgb(), image[image.width / 3 + clearOfLabels, middle].toArgb())
        assertEquals(colors.fill.toArgb(), image[clearOfLabels, middle].toArgb())
    }
}
