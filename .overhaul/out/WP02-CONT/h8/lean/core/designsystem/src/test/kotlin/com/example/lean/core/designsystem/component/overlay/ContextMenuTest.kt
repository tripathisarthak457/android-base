package com.example.lean.core.designsystem.component.overlay

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import com.example.lean.core.designsystem.TestSdk
import com.example.lean.core.designsystem.component.list.AppRow
import com.example.lean.core.designsystem.component.text.AppText
import com.example.lean.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class ContextMenuTest {

    @get:Rule
    val rule = createComposeRule()

    private var opened = 0
    private var shared = 0

    private fun row() {
        rule.setThemed {
            Column {
                AppContextMenu(menu = { AppMenuItem(text = "Share", onClick = { shared++ }) }) {
                    AppRow(title = "Report", onClick = { opened++ }, modifier = Modifier.testTag("row"))
                }
            }
        }
    }

    @Test
    fun `a long press opens the menu and never clicks the row`() {
        row()
        rule.onNodeWithTag("row").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(800)
        // The open menu is modal: the page beneath has left the merged tree.
        rule.onNodeWithTag("row", useUnmergedTree = true).performTouchInput { up() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Share").assertExists()
        assertEquals(0, opened)
    }

    @Test
    fun `a short tap is still the row's`() {
        row()
        rule.onNodeWithTag("row").performClick()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Share").assertDoesNotExist()
        assertEquals(1, opened)
    }

    @Test
    fun `a screen reader opens it as the row's own long-click action`() {
        row()
        rule.onNodeWithTag("row")
            .assert(SemanticsMatcher("has a long-click labelled Show menu") { it.config[SemanticsActions.OnLongClick].label == "Show menu" })
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .performSemanticsAction(SemanticsActions.OnLongClick)
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Share").assertExists()
    }

    @Test
    fun `content that is not a control is merged into one node that offers the menu`() {
        rule.setThemed {
            AppContextMenu(menu = { AppMenuItem(text = "Share", onClick = {}) }, modifier = Modifier.testTag("bubble")) {
                AppText("See you at eight")
            }
        }
        rule.onNodeWithTag("bubble")
            .assert(hasText("See you at eight"))
            .performSemanticsAction(SemanticsActions.OnLongClick)
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Share").assertExists()
    }

    @Test
    fun `a right click opens it at the pointer without clicking`() {
        row()
        rule.onNodeWithTag("row").performMouseInput {
            moveTo(Offset(40f, 20f))
            press(MouseButton.Secondary)
            release(MouseButton.Secondary)
        }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Share").assertExists()
        assertEquals(0, opened)
    }

    @Test
    fun `choosing an item runs it and closes the menu`() {
        row()
        rule.onNodeWithTag("row").performSemanticsAction(SemanticsActions.OnLongClick)
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Share").performClick()
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(1, shared)
        rule.onNodeWithText("Share").assertDoesNotExist()
    }
}
