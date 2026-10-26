package com.example.cmp.core.navigation.shell

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cmp.core.designsystem.foundation.layout.ChromeInset
import com.example.cmp.core.designsystem.foundation.layout.LocalBottomChrome
import com.example.cmp.core.designsystem.foundation.layout.chromeContentPadding
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CONTENT = "content"

/**
 * `chromeContentPadding()` follows the chrome in the layout phase, as window insets do: a bar that
 * settles or a keyboard that rises moves the content without composing its screen again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class ChromePaddingTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val chrome = ChromeInset()
    private var compositions = 0
    private lateinit var view: View

    @Composable
    private fun Content() {
        SideEffect { compositions++ }
        view = LocalView.current
        Box(Modifier.fillMaxSize().padding(chromeContentPadding()).testTag(CONTENT))
    }

    private fun show() {
        rule.setContent {
            CompositionLocalProvider(LocalBottomChrome provides chrome) { Content() }
        }
        rule.waitForIdle()
    }

    @Test
    fun `a chrome change lays the content out again and composes nothing`() {
        show()
        val full = contentBottom()
        val composed = compositions

        rule.runOnIdle { chrome.value = 80.dp }
        rule.waitForIdle()

        assertEquals(full - with(rule.density) { 80.dp.toPx() }, contentBottom(), 0.5f)
        assertEquals("the screen composed again for a chrome change", composed, compositions)
    }

    @Test
    fun `the bottom padding never falls below the navigation bar`() {
        show()
        val full = contentBottom()
        val navigationBar = 48
        rule.runOnIdle {
            val insets = WindowInsetsCompat.Builder()
                .setInsets(
                    WindowInsetsCompat.Type.navigationBars(),
                    Insets.of(0, 0, 0, navigationBar),
                )
                .build()
            ViewCompat.dispatchApplyWindowInsets(view, insets)
        }
        rule.waitForIdle()
        assertEquals("no chrome: the navigation bar", full - navigationBar, contentBottom(), 0.5f)

        rule.runOnIdle { chrome.value = 80.dp }
        rule.waitForIdle()
        assertEquals(
            "chrome taller than the bar: the chrome",
            full - with(rule.density) { 80.dp.toPx() },
            contentBottom(),
            0.5f,
        )
    }

    private fun contentBottom(): Float =
        rule.onNodeWithTag(CONTENT).fetchSemanticsNode().boundsInRoot.bottom
}
