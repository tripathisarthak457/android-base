package com.example.deskstandard.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.deskstandard.core.designsystem.TestSdk
import com.example.deskstandard.core.designsystem.component.container.AppScaffold
import com.example.deskstandard.core.designsystem.component.navigation.AppLargeTitleTopBar
import com.example.deskstandard.core.designsystem.component.navigation.AppTopBar
import com.example.deskstandard.core.designsystem.foundation.layout.LocalTopChrome
import com.example.deskstandard.core.designsystem.setThemed
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
 * bar's height, from the first frame, and a change in one must not recompose the other. The window
 * is a phone held upright: Robolectric's default is short enough to count as a phone held sideways,
 * where a large title renders inline.
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
}
