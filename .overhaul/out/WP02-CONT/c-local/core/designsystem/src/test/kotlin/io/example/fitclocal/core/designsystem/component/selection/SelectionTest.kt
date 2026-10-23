package io.example.fitclocal.core.designsystem.component.selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import io.example.fitclocal.core.designsystem.TestSdk
import io.example.fitclocal.core.designsystem.component.list.AppRow
import io.example.fitclocal.core.designsystem.component.list.AppToggleRow
import io.example.fitclocal.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class SelectionTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a switch or checkbox with no callback leaves the row's tap to the row`() {
        var rowClicks = 0
        rule.setThemed {
            AppRow(
                title = "Notifications",
                onClick = { rowClicks++ },
                modifier = Modifier.testTag("row"),
                trailing = {
                    AppSwitch(checked = true, onCheckedChange = null, modifier = Modifier.testTag("switch"))
                    AppCheckbox(checked = false, onCheckedChange = null, modifier = Modifier.testTag("box"))
                },
            )
        }
        rule.onNodeWithTag("switch", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("box", useUnmergedTree = true).performClick()
        assertEquals(2, rowClicks)
    }

    @Test
    fun `a toggle row is one switch, named by its title, across its whole width`() {
        var checked = false
        rule.setThemed {
            AppToggleRow(title = "Dark mode", checked = checked, onCheckedChange = { checked = it }, modifier = Modifier.testTag("row"))
        }
        rule.onAllNodes(isToggleable()).assertCountEquals(1)
        rule.onNodeWithTag("row")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOff()
            .performTouchInput { click(centerLeft + Offset(40f, 0f)) }
        assertTrue(checked)
    }

    @Test
    fun `selection controls answer taps across 48dp`() {
        rule.setThemed {
            Box {
                AppCheckbox(checked = true, onCheckedChange = {}, modifier = Modifier.testTag("box"))
            }
        }
        val bounds = rule.onNodeWithTag("box").getUnclippedBoundsInRoot()
        assertTrue(bounds.height >= 48.dp && bounds.width >= 48.dp)
        rule.onNodeWithTag("box").assertIsOn()
    }

    @Test
    fun `a slow drag on a stepped slider moves it step by step`() {
        var value by mutableFloatStateOf(0f)
        val seen = mutableListOf<Float>()
        rule.setThemed {
            AppSlider(
                value = value,
                onValueChange = {
                    value = it
                    seen += it
                },
                steps = 3,
                modifier = Modifier.fillMaxWidth().testTag("slider"),
            )
        }
        rule.onNodeWithTag("slider").performTouchInput {
            down(centerLeft)
            repeat(200) { moveBy(Offset(width / 200f, 0f)) }
            up()
        }
        assertEquals(1f, value, 0.001f)
        assertEquals(listOf(0.25f, 0.5f, 0.75f, 1f), seen.distinct())
    }

    @Test
    fun `starting a vertical scroll over a slider leaves its value alone`() {
        var value by mutableFloatStateOf(0.5f)
        rule.setThemed {
            AppSlider(value = value, onValueChange = { value = it }, modifier = Modifier.fillMaxWidth().testTag("slider"))
        }
        rule.onNodeWithTag("slider").performTouchInput {
            down(centerRight)
            repeat(20) { moveBy(Offset(0f, 20f)) }
            up()
        }
        assertEquals(0.5f, value, 0.001f)
    }
}
