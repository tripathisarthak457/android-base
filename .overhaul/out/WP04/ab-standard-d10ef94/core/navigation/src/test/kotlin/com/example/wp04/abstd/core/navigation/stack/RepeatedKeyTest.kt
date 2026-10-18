package com.example.wp04.abstd.core.navigation.stack

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.wp04.abstd.core.common.navigation.AppNavKey
import com.example.wp04.abstd.core.designsystem.theme.AppTheme
import com.example.wp04.abstd.core.navigation.AppBackStack
import com.example.wp04.abstd.core.navigation.FeedTab
import com.example.wp04.abstd.core.navigation.NavCommand
import com.example.wp04.abstd.core.navigation.NavPane
import com.example.wp04.abstd.core.navigation.NavRegistry
import com.example.wp04.abstd.core.navigation.Post
import com.example.wp04.abstd.core.navigation.ROBOLECTRIC_SDK
import com.example.wp04.abstd.core.navigation.back.StackChange
import com.example.wp04.abstd.core.navigation.navGraph
import com.example.wp04.abstd.core.navigation.transition.ScreenMotion
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A destination pushed while it is already in the stack (a profile opened from a profile it led to,
 * a row selected again beside its list) is a second screen, as a second push of one view controller
 * class is: its own composition and saved state, and Back returns to the first one as it was.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w1000dp-h800dp")
class RepeatedKeyTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var screens = 0

    /** Each screen is tagged with its key and the number it was given when it was first composed. */
    private fun registry(pane: NavPane) = NavRegistry(
        setOf(
            navGraph {
                entry<FeedTab>(pane = if (pane == NavPane.Detail) NavPane.List else NavPane.Single) { key ->
                    Instance(key)
                }
                entry<Post>(pane = pane) { key -> Instance(key) }
            },
        ),
    )

    @Composable
    private fun Instance(key: AppNavKey) {
        val instance = rememberSaveable { ++screens }
        Box(Modifier.fillMaxSize().testTag("$key/$instance"))
    }

    private fun show(stack: AppBackStack, pane: NavPane, window: DpSize) {
        val registry = registry(pane)
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(window)) {
                AppTheme { StackHost(stack = stack, registry = registry, motion = ScreenMotion()) }
            }
        }
        rule.waitForIdle()
    }

    private fun AppBackStack.push(key: AppNavKey) {
        rule.runOnIdle {
            apply(NavCommand.Navigate(key))
            gate.changed(StackChange.Push)
        }
        rule.waitForIdle()
    }

    private fun AppBackStack.back() {
        rule.runOnIdle { gate.onBack { pop(it) } }
        rule.waitForIdle()
    }

    @Test
    fun `a key pushed again over itself in a stack opens a second screen, and Back returns to the first`() {
        val stack = AppBackStack(mutableStateListOf(FeedTab, Post(1), Post(2)))
        show(stack, NavPane.Single, DpSize(400.dp, 800.dp))
        rule.onNodeWithTag("${Post(2)}/3").assertExists()

        stack.push(Post(1))
        rule.onNodeWithTag("${Post(1)}/4").assertExists()

        stack.back()
        rule.onNodeWithTag("${Post(2)}/3").assertExists()
        stack.back()
        rule.onNodeWithTag("${Post(1)}/2").assertExists()
        assertEquals("nothing was composed again", 4, screens)
    }

    @Test
    fun `a detail selected again beside its list opens a second detail, and Back walks back through both`() {
        val stack = AppBackStack(mutableStateListOf(FeedTab))
        show(stack, NavPane.Detail, DpSize(1000.dp, 800.dp))

        listOf(Post(1), Post(2), Post(1), Post(2)).forEach { stack.push(it) }
        rule.onNodeWithTag("${Post(2)}/5").assertExists()

        stack.back()
        rule.onNodeWithTag("${Post(1)}/4").assertExists()
        stack.back()
        rule.onNodeWithTag("${Post(2)}/3").assertExists()
        stack.back()
        rule.onNodeWithTag("${Post(1)}/2").assertExists()
    }
}
