package com.base.app.core.designsystem.component.formfields

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.width
import com.base.app.core.designsystem.TestSdk
import com.base.app.core.designsystem.setThemed
import com.base.app.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class PhoneFieldTest {

    @get:Rule
    val rule = createComposeRule()

    private val dialCode = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList)

    @Test
    fun `a screen reader names the number and the code each by its own label`() {
        rule.setThemed { AppPhoneField(dialCode = "+44", onDialCodeChange = {}, number = TextFieldState()) }
        rule.onNode(hasSetTextAction() and hasText("Phone number")).assertExists()
        rule.onNode(dialCode and hasText("Code") and hasText("+44")).assertExists()
    }

    @Test
    fun `the two boxes stay level when only one part has a label`() {
        rule.setThemed {
            AppPhoneField(dialCode = "+44", onDialCodeChange = {}, number = TextFieldState(), dialCodeLabel = null)
        }
        // Each part's box is the bottom of its node: neither shows anything beneath its box here.
        val code = rule.onNode(dialCode).getUnclippedBoundsInRoot()
        val number = rule.onNode(hasSetTextAction()).getUnclippedBoundsInRoot()
        assertEquals(number.bottom.value, code.bottom.value, 0.5f)
    }

    @Test
    fun `an error under the number leaves the code's box level with the number's`() {
        rule.setThemed {
            AppPhoneField(dialCode = "+44", onDialCodeChange = {}, number = TextFieldState(), error = "Enter the whole number")
        }
        rule.waitForIdle()
        val code = rule.onNode(dialCode).getUnclippedBoundsInRoot()
        val number = rule.onNode(hasSetTextAction()).getUnclippedBoundsInRoot()
        assertEquals(number.top.value, code.top.value, 0.5f)
    }

    @Test
    fun `at 2x the code's box grows with the text`() {
        var bodyGrowth = 1f
        rule.setThemed {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                val body = AppTheme.typography.body.fontSize
                bodyGrowth = with(LocalDensity.current) { body.toDp().value } / body.value
                AppPhoneField(dialCode = "+971", onDialCodeChange = {}, number = TextFieldState())
            }
        }
        val code = rule.onNode(dialCode).getUnclippedBoundsInRoot()
        assertTrue("body text grew only $bodyGrowth", bodyGrowth > 1.5f)
        assertEquals(108f * bodyGrowth, code.width.value, 0.5f)
        rule.onNodeWithText("+971", useUnmergedTree = true).assertExists()
    }
}
