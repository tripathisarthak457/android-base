package com.example.lean.core.designsystem.component.navigation

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.example.lean.core.designsystem.TestSdk
import com.example.lean.core.designsystem.component.button.AppIconButton
import com.example.lean.core.designsystem.icon.AppIcons
import com.example.lean.core.designsystem.setThemed
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class TopBarTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a long title stops before the actions, however many there are`() {
        rule.setThemed {
            AppTopBar(
                title = "A title long enough to reach well past the middle of a narrow phone",
                onBack = {},
                modifier = Modifier.width(320.dp),
            ) {
                AppIconButton(AppIcons.Share, contentDescription = "Share", onClick = {})
                AppIconButton(AppIcons.Edit, contentDescription = "Edit", onClick = {})
            }
        }
        val title = rule.onNodeWithText("A title long enough to reach well past the middle of a narrow phone").getUnclippedBoundsInRoot()
        val firstAction = rule.onNodeWithContentDescription("Share").getUnclippedBoundsInRoot()
        val back = rule.onNodeWithContentDescription("Navigate back").getUnclippedBoundsInRoot()
        assertTrue("title ends at ${title.right}, action starts at ${firstAction.left}", title.right <= firstAction.left)
        assertTrue(title.left >= back.right)
    }
}
