package com.base.app.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.base.app.core.designsystem.foundation.LocalTabVisible
import com.base.app.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The entry under the top stays composed, costs nothing while covered, and a pop does not rebuild it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class KeepUnderTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var rootCompositions = 0
    private var rootMeasures = 0
    private var rootLifecycle: Lifecycle? = null
    private var rootVisible = true
    private var inset by mutableStateOf(0.dp)
    private val stack = AppBackStack(mutableStateListOf(FeedTab))

    private val registry = testRegistry { key ->
        if (key == FeedTab) {
            remember { rootCompositions++ }
            rootLifecycle = LocalLifecycleOwner.current.lifecycle
            rootVisible = LocalTabVisible.current
            Box(
                Modifier.fillMaxSize().testTag(FeedTab.toString()).layout { measurable, constraints ->
                    rootMeasures++
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
            )
        } else {
            Screen(key)
        }
    }

    @Test
    fun `the covered root stays composed and unmeasured, and a pop brings the same composition back`() {
        rule.setContent {
            AppTheme {
                StackHost(stack = stack, registry = registry, motion = ScreenMotion(), modifier = Modifier.padding(inset))
            }
        }
        rule.runOnIdle {
            stack.apply(NavCommand.Navigate(Post(1)))
            stack.gate.changed(StackChange.Push)
        }
        rule.waitForIdle()
        val measuresWhileCovered = rootMeasures

        inset = 4.dp
        rule.waitForIdle()

        assertEquals("composed once", 1, rootCompositions)
        assertEquals("not measured while covered", measuresWhileCovered, rootMeasures)
        assertEquals(Lifecycle.State.CREATED, rootLifecycle!!.currentState)
        assertFalse(rootVisible)

        rule.runOnIdle { stack.gate.onBack { stack.pop(it) } }
        rule.waitForIdle()

        assertEquals("the pop moved the composition instead of starting a new one", 1, rootCompositions)
        assertEquals(Lifecycle.State.RESUMED, rootLifecycle!!.currentState)
        rule.onNodeWithTagExists(FeedTab)
    }

    @Test
    fun `a pop does not lay out the screen beneath the one it returns to`() {
        rule.setContent {
            AppTheme { StackHost(stack = stack, registry = registry, motion = ScreenMotion()) }
        }
        listOf(Post(1), Post(2)).forEach { key ->
            rule.runOnIdle {
                stack.apply(NavCommand.Navigate(key))
                stack.gate.changed(StackChange.Push)
            }
            rule.waitForIdle()
        }
        val measuresBeforePop = rootMeasures

        rule.runOnIdle { stack.gate.onBack { stack.pop(it) } }
        rule.waitForIdle()

        // The root sits under Post(1), which arrives opaque over it: laying it out would be a whole
        // screen of work per frame that nobody sees.
        assertEquals(listOf(FeedTab, Post(1)), stack.keys)
        assertEquals("the root was laid out during the pop", measuresBeforePop, rootMeasures)
    }
}
