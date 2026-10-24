package com.example.everything.core.designsystem.component.sheet.action

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.everything.core.designsystem.TestSdk
import com.example.everything.core.designsystem.theme.AppTheme
import com.example.everything.core.designsystem.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** At Medium width an action sheet is a card; it is modal for keyboards and screen readers. */
// Focus, not pixels: under the native graphics the module's screenshot tests switch on for every test,
// a requested focus never lands in Robolectric, so this class keeps the legacy mode it was written for.
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w800dp-h1280dp")
class ActionCardTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun show(actions: List<SheetAction>) {
        rule.setContent {
            AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
                AppActionSheet(visible = true, actions = actions, onDismissRequest = {})
            }
        }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
    }

    @Test
    fun `focus moves to the first action that can run`() {
        show(
            listOf(
                SheetAction("Share", onClick = {}, enabled = false),
                SheetAction("Duplicate", onClick = {}),
                SheetAction("Delete", onClick = {}, destructive = true),
            ),
        )
        rule.onNodeWithText("Duplicate").assertIsFocused()
        rule.onNodeWithText("Delete").assertIsNotFocused()
    }

    @Test
    fun `with every action disabled the card still shows and nothing takes focus`() {
        show(listOf(SheetAction("Share", onClick = {}, enabled = false)))
        rule.onNodeWithText("Share").assertExists()
        rule.onAllNodes(isFocused()).assertCountEquals(0)
    }
}
