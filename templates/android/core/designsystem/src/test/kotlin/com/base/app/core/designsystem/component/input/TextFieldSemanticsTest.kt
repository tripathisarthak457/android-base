package com.base.app.core.designsystem.component.input

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTextInput
import com.base.app.core.designsystem.TestSdk
import com.base.app.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class TextFieldSemanticsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `the label and placeholder belong to the field a screen reader announces`() {
        rule.setThemed { AppTextField(state = TextFieldState(), label = "Email", placeholder = "you@example.com") }
        rule.onNode(hasSetTextAction() and hasText("Email")).assert(hasText("you@example.com"))
    }

    @Test
    fun `an error is reported as the field's error`() {
        rule.setThemed { AppTextField(state = TextFieldState(), label = "Email", error = "Enter a valid address") }
        rule.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Enter a valid address"))
    }

    @Test
    fun `a limit refuses input past it, a paste included`() {
        val state = TextFieldState()
        rule.setThemed { AppTextField(state = state, maxLength = 5) }
        rule.onNode(hasSetTextAction()).performTextInput("abcdefgh")
        assertEquals("", state.text.toString())
        rule.onNode(hasSetTextAction()).performTextInput("abc")
        assertEquals("abc", state.text.toString())
    }
}
