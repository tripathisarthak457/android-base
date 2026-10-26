package com.example.finalafter.crui2.core.designsystem.component.pickers

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui2.core.designsystem.TestSdk
import com.example.finalafter.crui2.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Two wheels that build one value from each other, as hours and minutes do. Each must report with
 * the other's current value, not the value it had when the wheel was first composed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class WheelPickerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `turning one wheel keeps what the other was turned to`() {
        var hour by mutableStateOf(1)
        var minute by mutableStateOf(0)
        val hours = (0..23).toList()
        val minutes = (0..59).toList()
        rule.setThemed {
            Row {
                AppWheelPicker(items = hours, selectedIndex = hour, onSelectedChange = { hour = it }, modifier = Modifier.width(80.dp).testTag("hours"), label = { "$it" })
                AppWheelPicker(items = minutes, selectedIndex = minute, onSelectedChange = { minute = it }, modifier = Modifier.width(80.dp).testTag("minutes"), label = { "$it" })
            }
        }
        rule.onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("minutes"))).performScrollToIndex(30)
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        assertEquals(30, minute)

        rule.onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("hours"))).performScrollToIndex(7)
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        assertEquals(7, hour)
        assertEquals(30, minute)
    }

    @Test
    fun `a wheel moved away and back reports the return`() {
        var index by mutableStateOf(3)
        val items = (0..9).toList()
        rule.setThemed {
            AppWheelPicker(items = items, selectedIndex = index, onSelectedChange = { index = it }, modifier = Modifier.testTag("wheel"), label = { "$it" })
        }
        val wheel = rule.onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("wheel")))
        wheel.performScrollToIndex(6)
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        assertEquals(6, index)
        wheel.performScrollToIndex(3)
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        assertEquals(3, index)
    }
}
