package com.example.deskstandard.core.designsystem.foundation.interaction

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.example.deskstandard.core.designsystem.TestSdk
import com.example.deskstandard.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class PressableTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a tap clicks once and a small control still gets a 48dp target`() {
        var clicks = 0
        rule.setThemed {
            Box(Modifier.testTag("target").pressable(onClick = { clicks++ }, feedback = PressFeedback.Scale)) {
                Box(Modifier.size(20.dp))
            }
        }
        rule.onNodeWithTag("target").performClick()
        assertEquals(1, clicks)
        rule.onNodeWithTag("target").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        val bounds = rule.onNodeWithTag("target").getUnclippedBoundsInRoot()
        assertTrue(bounds.width >= 48.dp && bounds.height >= 48.dp)
    }

    @Test
    fun `an inline control keeps its label's width and still takes a touch beside it`() {
        var clicks = 0
        rule.setThemed {
            Box(Modifier.padding(start = 40.dp)) {
                Box(Modifier.testTag("link").inlinePressable(onClick = { clicks++ }, feedback = PressFeedback.Dim)) {
                    Box(Modifier.size(width = 24.dp, height = 16.dp))
                }
            }
        }
        val bounds = rule.onNodeWithTag("link").getUnclippedBoundsInRoot()
        assertEquals(40.dp, bounds.left)
        assertEquals(24.dp, bounds.width)
        assertEquals(48.dp, bounds.height)
        // 10dp left of the label: outside its layout, inside the 48dp a finger is given.
        rule.onNodeWithTag("link").performTouchInput { click(Offset(-10.dp.toPx(), centerY)) }
        assertEquals(1, clicks)
    }

    @Test
    fun `a disabled control takes no click and says it is disabled`() {
        var clicks = 0
        rule.setThemed {
            Box(Modifier.testTag("target").pressable(onClick = { clicks++ }, feedback = PressFeedback.Dim, enabled = false)) {
                Box(Modifier.size(40.dp))
            }
        }
        rule.onNodeWithTag("target").performClick()
        assertEquals(0, clicks)
        rule.onNodeWithTag("target").assertIsNotEnabled()
    }

    @Test
    fun `a long press runs the long action, not the click`() {
        var clicks = 0
        var longs = 0
        rule.setThemed {
            Box(Modifier.testTag("target").pressable(onClick = { clicks++ }, feedback = PressFeedback.Highlight, onLongClick = { longs++ })) {
                Box(Modifier.size(60.dp))
            }
        }
        rule.onNodeWithTag("target").performTouchInput { longClick() }
        assertEquals(0, clicks)
        assertEquals(1, longs)
    }

    @Test
    fun `pressing and releasing never recomposes the control`() {
        var compositions = 0
        rule.setThemed {
            Box(Modifier.testTag("target").pressable(onClick = {}, feedback = PressFeedback.Scale)) {
                SideEffect { compositions++ }
                Box(Modifier.size(60.dp))
            }
        }
        rule.waitForIdle()
        val before = compositions
        rule.onNodeWithTag("target").performTouchInput {
            down(center)
            advanceEventTime(400)
            up()
        }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        assertEquals(before, compositions)
    }

    @Test
    fun `a quick tap is reported as a whole press, even before the press delay`() {
        val seen = mutableListOf<Interaction>()
        rule.setThemed {
            val source = remember { MutableInteractionSource() }
            LaunchedEffect(source) { source.interactions.collect { seen += it } }
            Box(Modifier.testTag("target").pressable(onClick = {}, feedback = PressFeedback.Highlight, interactionSource = source)) {
                Box(Modifier.size(60.dp))
            }
        }
        rule.onNodeWithTag("target").performTouchInput {
            down(center)
            advanceEventTime(20)
            up()
        }
        rule.waitForIdle()
        assertTrue(seen.first() is PressInteraction.Press)
        assertTrue(seen.last() is PressInteraction.Release)
    }
}
