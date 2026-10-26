package com.example.finalafter.crui.core.designsystem.component.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui.core.designsystem.TestSdk
import com.example.finalafter.crui.core.designsystem.component.container.AppScaffold
import com.example.finalafter.crui.core.designsystem.component.navigation.AppTopBar
import com.example.finalafter.crui.core.designsystem.foundation.layout.LocalTopChrome
import com.example.finalafter.crui.core.designsystem.foundation.layout.chromeContentPadding
import com.example.finalafter.crui.core.designsystem.setThemed
import com.example.finalafter.crui.core.designsystem.theme.AppTheme
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A state view that is part of a screen rather than the whole of it, here in a fixed-height box on a
 * page under a top bar, as the catalog shows one. Given a padding of its own, it centres in the box
 * and keeps its action inside it, though the screen around it has chrome.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class StateViewTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a state view in a box centres in the box and keeps its action inside it`() {
        var topChrome = Dp.Unspecified
        rule.setThemed {
            AppScaffold(topBar = { AppTopBar(title = "Feedback") }) {
                topChrome = LocalTopChrome.current.value
                // Clear of the top bar as a catalog page is, with the box at its top.
                Column(Modifier.padding(chromeContentPadding())) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(BoxHeight)
                            .testTag("box"),
                    ) {
                        AppStateView(
                            title = "No messages yet",
                            message = "Start a conversation.",
                            actionLabel = "New message",
                            onAction = {},
                            contentPadding = PaddingValues(AppTheme.spacing.xl),
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        assertTrue("the top bar reported no height", topChrome > 0.dp)

        val box = rule.onNodeWithTag("box").getBoundsInRoot()
        val title = rule.onNodeWithText("No messages yet").getBoundsInRoot()
        val action = rule.onNodeWithText("New message").getBoundsInRoot()
        assertTrue("the title at ${title.top} is above the box at ${box.top}", title.top >= box.top)
        assertTrue("the action at ${action.bottom} is below the box at ${box.bottom}", action.bottom <= box.bottom)

        val above = title.top - box.top
        val below = box.bottom - action.bottom
        assertTrue("not centred: $above above the title, $below below the action", abs((above - below).value) <= 1f)
    }

    private companion object {
        val BoxHeight = 280.dp
    }
}
