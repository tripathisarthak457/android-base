package io.example.fitcnet.core.designsystem.component.input

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.width
import io.example.fitcnet.core.designsystem.TestSdk
import io.example.fitcnet.core.designsystem.setThemed
import io.example.fitcnet.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A control at the end of a field grows with the field's text, as the glyph at its start does. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class FieldAccessoryTest {

    @get:Rule
    val rule = createComposeRule()

    /** How much body text grows at the scale under test; Android 14 and later scale large sizes less. */
    private var bodyGrowth = 1f

    /** The glyph itself, not the 48dp touch target around it. */
    private fun glyphWidth(contentDescription: String, fontScale: Float, field: @Composable () -> Unit): Dp {
        rule.setThemed {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                val body = AppTheme.typography.body.fontSize
                bodyGrowth = with(LocalDensity.current) { body.toDp().value } / body.value
                field()
            }
        }
        return rule.onNodeWithContentDescription(contentDescription, useUnmergedTree = true).getUnclippedBoundsInRoot().width
    }

    @Test
    fun `at the default scale the reveal button keeps the small icon size`() {
        val width = glyphWidth("Show password", fontScale = 1f) { AppSecureField(TextFieldState()) }
        assertEquals(16f, width.value, 0.5f)
    }

    @Test
    fun `at 2x the reveal button grows with the secure field's text`() {
        val width = glyphWidth("Show password", fontScale = 2f) { AppSecureField(TextFieldState()) }
        assertTrue("body text grew only $bodyGrowth", bodyGrowth > 1.5f)
        assertEquals(16f * bodyGrowth, width.value, 0.5f)
    }

    @Test
    fun `at 2x the clear button grows with the search field's text`() {
        val width = glyphWidth("Clear search", fontScale = 2f) { AppSearchField(TextFieldState("tea")) }
        assertEquals(16f * bodyGrowth, width.value, 0.5f)
    }
}
