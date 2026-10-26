package com.example.finalafter.crui2.core.designsystem.component.formfields

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.finalafter.crui2.core.designsystem.TestSdk
import com.example.finalafter.crui2.core.designsystem.setThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class MoneyFieldTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = "de")
    fun `a language chosen without a region still shows a field, in that language's likeliest currency`() {
        rule.setThemed { AppMoneyField(minorUnits = 995, onMinorUnitsChange = {}, label = "Betrag") }

        rule.onNodeWithText("Betrag").assertExists()
        rule.onNodeWithText("€").assertExists()
    }

    @Test
    @Config(qualifiers = "en-rGB")
    fun `a locale with a region takes that region's currency`() {
        rule.setThemed { AppMoneyField(minorUnits = 995, onMinorUnitsChange = {}, label = "Amount") }

        rule.onNodeWithText("£").assertExists()
    }
}
