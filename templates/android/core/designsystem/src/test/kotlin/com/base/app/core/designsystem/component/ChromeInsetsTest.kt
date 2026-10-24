package com.base.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.TestSdk
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.navigation.AppLargeTitleTopBar
import com.base.app.core.designsystem.component.navigation.AppTopBar
import com.base.app.core.designsystem.foundation.layout.LocalTopChrome
import com.base.app.core.designsystem.foundation.layout.chromeContentPadding
import com.base.app.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Two screens are composed at once during a push or pop. Each scaffold's top inset must be its own
 * bar's height, from the first frame, and a change in one must not recompose the other. A pushed
 * screen is composed in the frame that starts the push, so the scaffold composes its slots with the
 * screen rather than in a composition of its own inside its measure pass. The window is a phone held
 * upright: Robolectric's default is short enough to count as a phone held sideways, where a large
 * title renders inline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class ChromeInsetsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `two scaffolds keep their own top inset and do not recompose each other`() {
        var inline = Dp.Unspecified
        var large = Dp.Unspecified
        var inlineReads = 0
        var subtitle by mutableStateOf<String?>(null)
        rule.setThemed {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    AppScaffold(topBar = { AppTopBar(title = "Inline") }) {
                        inline = LocalTopChrome.current.value
                        SideEffect { inlineReads++ }
                    }
                }
                Box(Modifier.weight(1f)) {
                    AppScaffold(topBar = { AppLargeTitleTopBar(title = "Large", subtitle = subtitle, scrolled = { 0 }) }) {
                        large = LocalTopChrome.current.value
                    }
                }
            }
        }
        rule.waitForIdle()
        assertTrue(inline > 0.dp)
        assertTrue(large > inline)
        val readsBefore = inlineReads
        val inlineBefore = inline
        val largeBefore = large

        subtitle = "A second line"
        rule.waitForIdle()
        assertNotEquals(largeBefore, large)
        assertEquals(inlineBefore, inline)
        assertEquals(readsBefore, inlineReads)
    }

    @Test
    fun `content that reads its inset as it lays out is below its bar in the first layout`() {
        val tops = mutableListOf<Dp>()
        rule.setThemed {
            AppScaffold(topBar = { AppTopBar(title = "Inline", modifier = Modifier.testTag("bar")) }) {
                val padding = chromeContentPadding()
                Box(
                    Modifier.fillMaxSize().layout { measurable, constraints ->
                        tops += padding.calculateTopPadding()
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    },
                )
            }
        }
        rule.waitForIdle()
        val bounds = rule.onNodeWithTag("bar").getBoundsInRoot()
        val bar = bounds.bottom - bounds.top
        assertTrue(bar > 0.dp)
        assertEquals(bar.value, tops.first().value, 0.5f)
    }

    @Test
    fun `the bars and the content are composed with the screen, not in the scaffold's measure pass`() {
        var measuring = false
        val composedWhileMeasuring = mutableListOf<String>()
        rule.setThemed {
            AppScaffold(
                modifier = Modifier.layout { measurable, constraints ->
                    measuring = true
                    val placeable = measurable.measure(constraints)
                    measuring = false
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
                topBar = {
                    SideEffect { if (measuring) composedWhileMeasuring += "top bar" }
                    AppTopBar(title = "Inline")
                },
                bottomBar = {
                    SideEffect { if (measuring) composedWhileMeasuring += "bottom bar" }
                    Box(Modifier.fillMaxWidth().height(48.dp))
                },
            ) {
                SideEffect { if (measuring) composedWhileMeasuring += "content" }
            }
        }
        rule.waitForIdle()
        assertEquals(emptyList<String>(), composedWhileMeasuring)
    }
}
