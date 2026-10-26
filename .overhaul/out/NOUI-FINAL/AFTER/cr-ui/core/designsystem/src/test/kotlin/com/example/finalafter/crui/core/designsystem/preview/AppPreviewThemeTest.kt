package com.example.finalafter.crui.core.designsystem.preview

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.example.finalafter.crui.core.designsystem.TestSdk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class AppPreviewThemeTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = "ar")
    fun `a right-to-left locale lays the preview out right to left, as Android Studio does`() {
        assertEquals(LayoutDirection.Rtl, directionInPreview())
    }

    @Test
    @Config(qualifiers = "en")
    fun `a left-to-right locale keeps the preview left to right`() {
        assertEquals(LayoutDirection.Ltr, directionInPreview())
    }

    private fun directionInPreview(): LayoutDirection? {
        var direction: LayoutDirection? = null
        rule.setContent { AppPreviewTheme().Wrap { direction = LocalLayoutDirection.current } }
        rule.waitForIdle()
        return direction
    }
}
