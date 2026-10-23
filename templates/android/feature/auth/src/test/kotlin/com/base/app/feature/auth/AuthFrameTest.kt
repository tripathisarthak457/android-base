package com.base.app.feature.auth

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.base.app.core.designsystem.component.navigation.AppTopBar
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.testing.compose.TEST_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The auth screens' frame keeps its form clear of their top bar, whatever the window. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK], qualifiers = "w411dp-h891dp")
class AuthFrameTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show() {
        compose.setContent {
            AppTheme {
                AuthFrame(topBar = { AppTopBar(title = "Reset", onBack = {}, modifier = Modifier.testTag(BAR)) }) {
                    AppText(text = "First", modifier = Modifier.testTag(FIRST))
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the form starts below the top bar, not under it`() {
        show()

        val bar = compose.onNodeWithTag(BAR).getUnclippedBoundsInRoot()
        val first = compose.onNodeWithTag(FIRST).getUnclippedBoundsInRoot()
        assertTrue("the form starts at ${first.top}, under the bar that ends at ${bar.bottom}", first.top >= bar.bottom)
    }

    @Test
    @Config(qualifiers = "w900dp-h700dp")
    fun `a short form centred in a wide window fits without scrolling`() {
        show()

        val range = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertEquals("the form scrolls although it fits", 0f, range.maxValue(), 0f)
    }

    private companion object {
        const val BAR = "bar"
        const val FIRST = "first"
    }
}
