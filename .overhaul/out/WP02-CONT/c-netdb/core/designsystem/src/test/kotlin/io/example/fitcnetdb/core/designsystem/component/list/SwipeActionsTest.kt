package io.example.fitcnetdb.core.designsystem.component.list

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import io.example.fitcnetdb.core.designsystem.TestSdk
import io.example.fitcnetdb.core.designsystem.icon.AppIcons
import io.example.fitcnetdb.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class SwipeActionsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `the row is as tall as its content, not as its parent allows`() {
        rule.setThemed {
            Column {
                AppSwipeActions(
                    trailing = listOf(SwipeAction("Archive", AppIcons.Folder, onClick = {})),
                    modifier = Modifier.testTag("swipe"),
                ) {
                    AppRow(title = "Message", modifier = Modifier.testTag("row"))
                }
            }
        }
        // A row with no click of its own is merged into the swipe container's node.
        assertEquals(
            rule.onNodeWithTag("row", useUnmergedTree = true).getUnclippedBoundsInRoot().height,
            rule.onNodeWithTag("swipe").getUnclippedBoundsInRoot().height,
        )
    }

    @Test
    fun `a tap on an open row closes it instead of opening the row`() {
        var rowClicks = 0
        rule.setThemed {
            Column {
                AppSwipeActions(
                    trailing = listOf(SwipeAction("Archive", AppIcons.Folder, onClick = {})),
                    allowFullSwipe = false,
                    modifier = Modifier.testTag("swipe"),
                ) {
                    AppRow(title = "Message", onClick = { rowClicks++ }, modifier = Modifier.testTag("row"))
                }
            }
        }
        rule.onNodeWithTag("swipe").performTouchInput { swipeLeft(startX = right - 10f, endX = right - 200f) }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        assertTrue("the row is open", rule.onNodeWithTag("row").getUnclippedBoundsInRoot().left < 0.dp)
        rule.onNodeWithTag("row").performClick()
        assertEquals(0, rowClicks)
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("row").performClick()
        assertEquals(1, rowClicks)
    }

    @Test
    fun `the hidden actions are offered to a screen reader`() {
        rule.setThemed {
            AppSwipeActions(
                trailing = listOf(SwipeAction("Delete", AppIcons.Trash, onClick = {}, destructive = true)),
                modifier = Modifier.testTag("swipe"),
            ) {
                AppRow(title = "Message")
            }
        }
        rule.onNodeWithTag("swipe").assert(hasDeleteAction)
    }

    @Test
    fun `a clickable row carries the hidden actions itself, so a screen reader stops on it once`() {
        rule.setThemed {
            AppSwipeActions(
                trailing = listOf(SwipeAction("Delete", AppIcons.Trash, onClick = {}, destructive = true)),
                modifier = Modifier.testTag("swipe"),
            ) {
                AppRow(title = "Message", onClick = {}, modifier = Modifier.testTag("row"))
            }
        }
        rule.onNodeWithTag("row").assert(hasDeleteAction)
        rule.onNodeWithTag("swipe", useUnmergedTree = true).assert(!hasDeleteAction)
    }

    private val hasDeleteAction = SemanticsMatcher("has a Delete action") { node ->
        node.config.getOrNull(SemanticsActions.CustomActions)?.any { it.label == "Delete" } == true
    }
}
