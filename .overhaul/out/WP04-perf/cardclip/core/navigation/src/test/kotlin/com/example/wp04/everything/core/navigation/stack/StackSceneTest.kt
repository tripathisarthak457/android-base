package com.example.wp04.everything.core.navigation.stack

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.wp04.everything.core.common.navigation.AppNavKey
import com.example.wp04.everything.core.designsystem.foundation.layout.LocalTabVisible
import com.example.wp04.everything.core.designsystem.foundation.layout.TabVisibility
import com.example.wp04.everything.core.designsystem.theme.AppTheme
import com.example.wp04.everything.core.navigation.AppBackStack
import com.example.wp04.everything.core.navigation.FeedTab
import com.example.wp04.everything.core.navigation.LocalNavigationSettled
import com.example.wp04.everything.core.navigation.NavCommand
import com.example.wp04.everything.core.navigation.Post
import com.example.wp04.everything.core.navigation.ROBOLECTRIC_SDK
import com.example.wp04.everything.core.navigation.Screen
import com.example.wp04.everything.core.navigation.back.StackChange
import com.example.wp04.everything.core.navigation.onNodeWithTagExists
import com.example.wp04.everything.core.navigation.testRegistry
import com.example.wp04.everything.core.navigation.transition.ScreenMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The screens under the top stay composed, attached and laid out, parked out of sight, so a pop
 * shows one again without composing, attaching or measuring it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class StackSceneTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val probe = Probe()
    private var rootLifecycle: Lifecycle? = null
    private var rootVisible: TabVisibility? = null
    private val stack = AppBackStack(mutableStateListOf(FeedTab))

    private val registry = testRegistry { key ->
        if (key == FeedTab) {
            remember { probe.compositions++ }
            rootLifecycle = LocalLifecycleOwner.current.lifecycle
            rootVisible = LocalTabVisible.current
            Box(Modifier.fillMaxSize().testTag(FeedTab.toString()).then(ProbeElement(probe)))
        } else {
            settled[key] = LocalNavigationSettled.current
            DisposableEffect(key) { onDispose { disposed += key } }
            Screen(key)
        }
    }
    private val settled = HashMap<AppNavKey, () -> Boolean>()
    private val disposed = mutableListOf<AppNavKey>()

    private fun show() {
        rule.setContent {
            AppTheme { StackHost(stack = stack, registry = registry, motion = ScreenMotion()) }
        }
        rule.waitForIdle()
    }

    private fun push(key: AppNavKey) {
        rule.runOnIdle {
            stack.apply(NavCommand.Navigate(key))
            stack.gate.changed(StackChange.Push)
        }
        rule.waitForIdle()
    }

    @Test
    fun `the covered root stays attached and parked, and a pop shows the same nodes`() {
        show()
        push(Post(1))

        assertEquals("composed once", 1, probe.compositions)
        assertEquals("never detached", 0, probe.detaches)
        assertEquals("the page under the top is started, so its data stays current", Lifecycle.State.STARTED, rootLifecycle!!.currentState)
        assertFalse(rootVisible!!.isVisible)
        // What accessibility reads: the parked root's semantics are cleared.
        rule.onNodeWithTag(FeedTab.toString()).assertDoesNotExist()

        rule.runOnIdle { stack.gate.onBack { stack.pop(it) } }
        rule.waitForIdle()

        assertEquals("the pop composed nothing", 1, probe.compositions)
        assertEquals("the pop detached nothing", 0, probe.detaches)
        assertEquals(Lifecycle.State.RESUMED, rootLifecycle!!.currentState)
        assertTrue(rootVisible!!.isVisible)
        rule.onNodeWithTagExists(FeedTab)
    }

    @Test
    fun `a pop starts no layout of the screen it returns to`() {
        show()
        push(Post(1))
        val measuresBeforePop = probe.measures

        rule.runOnIdle { stack.gate.onBack { stack.pop(it) } }
        rule.waitForIdle()

        assertEquals(listOf<AppNavKey>(FeedTab), stack.keys)
        assertEquals("the root was measured again for the pop", measuresBeforePop, probe.measures)
    }

    @Test
    fun `a popped screen leaves composition once its slide has ended`() {
        show()
        push(Post(1))

        rule.runOnIdle { stack.gate.onBack { stack.pop(it) } }
        rule.waitForIdle()

        assertEquals(listOf<AppNavKey>(Post(1)), disposed)
        rule.onNodeWithTag(Post(1).toString(), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `a pushed screen is told when its slide has settled`() {
        show()
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread {
            stack.apply(NavCommand.Navigate(Post(1)))
            stack.gate.changed(StackChange.Push)
        }
        var frames = 0
        while (Post(1) !in settled && frames++ < MAX_FRAMES) rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        assertTrue("the slide has not started", stack.gate.moving)
        assertFalse("settled in the middle of the slide", settled.getValue(Post(1))())

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertTrue(settled.getValue(Post(1))())
    }

    @Test
    fun `pages deeper than the parked depth leave composition, and come back from saved state`() {
        show()
        (1..4).forEach { push(Post(it)) }

        assertEquals(Lifecycle.State.DESTROYED, rootLifecycle!!.currentState)

        rule.runOnIdle {
            stack.pop(3)
            stack.gate.changed(StackChange.Pop)
        }
        rule.waitForIdle()

        rule.onNodeWithTagExists(Post(1))
        assertEquals("composed again once it was back within reach", 2, probe.compositions)
    }
}

private class Probe {
    var compositions = 0
    var measures = 0
    var detaches = 0
}

private data class ProbeElement(val probe: Probe) : ModifierNodeElement<ProbeNode>() {
    override fun create() = ProbeNode(probe)

    override fun update(node: ProbeNode) {
        node.probe = probe
    }
}

private class ProbeNode(var probe: Probe) : Modifier.Node(), LayoutModifierNode {
    override fun onDetach() {
        probe.detaches++
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        probe.measures++
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}

private const val MAX_FRAMES = 10
