package com.example.standard.core.designsystem.component.text

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.standard.core.designsystem.TestSdk
import com.example.standard.core.designsystem.setThemed
import com.example.standard.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A glyph beside text, and a display title, grow with the person's font scale. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class TextScaleTest {

    @get:Rule
    val rule = createComposeRule()

    private fun measure(fontScale: Float): Pair<Dp, Float> {
        var glyph = Dp.Unspecified
        var title = TextUnit.Unspecified
        var density = 1f
        rule.setThemed {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                glyph = 16.dp.scaledLike(AppTheme.typography.body)
                title = AppTheme.typography.largeTitle.grownWithFontScale(1.3f).fontSize
                density = LocalDensity.current.run { title.toDp().value }
            }
        }
        rule.waitForIdle()
        return glyph to density
    }

    @Test
    fun `at the default scale nothing changes`() {
        val (glyph, title) = measure(1f)
        assertEquals(16f, glyph.value, 0.01f)
        assertEquals(34f, title, 0.01f)
    }

    @Test
    fun `at 1_5x the glyph grows as body text does and the title grows to its cap`() {
        val (glyph, title) = measure(1.5f)
        assertTrue("glyph $glyph", glyph.value > 16f * 1.2f)
        assertEquals(34f * 1.3f, title, 0.1f)
    }
}
