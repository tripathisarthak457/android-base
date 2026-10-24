package com.example.deskstandard.core.designsystem.component.sheet.choice

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.deskstandard.core.designsystem.TestSdk
import com.example.deskstandard.core.designsystem.theme.AppTheme
import com.example.deskstandard.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class AppChoiceSheetTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    private fun showSheet(choices: List<String>) {
        rule.setContent {
            AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
                AppChoiceSheet(
                    visible = true,
                    choices = choices,
                    selected = choices.first(),
                    onSelect = { calls += "select $it" },
                    onDismissRequest = { calls += "dismiss" },
                    label = { it },
                    onSelectNone = { calls += "none" },
                )
            }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
    }

    @Test
    fun `choosing selects, then asks to close`() {
        showSheet(listOf("Small", "Medium", "Large"))
        rule.onNodeWithText("Medium").performClick()
        assertEquals(listOf("select Medium", "dismiss"), calls)
    }

    @Test
    fun `the None row selects nothing`() {
        showSheet(listOf("Small", "Medium", "Large"))
        rule.onNodeWithText("None").performClick()
        assertEquals(listOf("none", "dismiss"), calls)
    }

    @Test
    fun `from six choices a search ignores case and accents`() {
        showSheet(listOf("Deutsch", "English", "Español", "Français", "Italiano", "Polski"))
        rule.onAllNodesWithText("Deutsch").assertCountEquals(1)
        rule.onNode(hasSetTextAction()).performTextInput("ESPANOL")
        rule.mainClock.advanceTimeBy(500)
        rule.onAllNodesWithText("Español").assertCountEquals(1)
        rule.onAllNodesWithText("Deutsch").assertCountEquals(0)
    }
}
