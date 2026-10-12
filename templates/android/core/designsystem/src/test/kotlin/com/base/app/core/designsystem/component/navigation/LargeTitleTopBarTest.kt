package com.base.app.core.designsystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.TestSdk
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.foundation.LocalTopChrome
import com.base.app.core.designsystem.setThemed
import com.base.app.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class LargeTitleTopBarTest {

    @get:Rule
    val rule = createComposeRule()

    private var inset = Dp.Unspecified
    private var scrolled by mutableIntStateOf(0)
    private var accessoryCompositions = 0
    private var contentCompositions = 0
    private var withAccessory by mutableStateOf(true)

    private fun setBar() {
        rule.setThemed {
            AppScaffold(
                topBar = {
                    AppLargeTitleTopBar(
                        title = "Inbox",
                        scrolled = { scrolled },
                        accessory = if (withAccessory) {
                            {
                                SideEffect { accessoryCompositions++ }
                                Box(Modifier.height(AccessoryHeight))
                            }
                        } else {
                            null
                        },
                    )
                },
            ) {
                inset = LocalTopChrome.current.value
                SideEffect { contentCompositions++ }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun `the accessory is part of the height content starts below`() {
        withAccessory = false
        setBar()
        val without = inset
        withAccessory = true
        rule.waitForIdle()
        assertTrue("$without then $inset", inset >= without + AccessoryHeight)
    }

    @Test
    fun `scrolling collapses the title without recomposing the bar's slots or the screen`() {
        setBar()
        val accessoryBefore = accessoryCompositions
        val contentBefore = contentCompositions
        for (offset in 0..2_000 step 25) {
            scrolled = offset
            rule.waitForIdle()
        }
        assertEquals(accessoryBefore, accessoryCompositions)
        assertEquals(contentBefore, contentCompositions)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `content that runs ahead of the title goes under it, never across it`() {
        var screen = Color.Unspecified
        var barBottom = 0
        rule.setThemed {
            screen = AppTheme.colors.backgroundGrouped
            AppScaffold(
                modifier = Modifier.testTag("screen"),
                background = screen,
                topBar = { AppLargeTitleTopBar(title = "Inbox", subtitle = "All mail", scrolled = { scrolled }) },
            ) {
                barBottom = with(LocalDensity.current) { LocalTopChrome.current.value.roundToPx() }
                // Rows drawn from the very top, as a list that is further along than the bar was told.
                Box(Modifier.fillMaxSize().background(Row))
            }
        }
        rule.waitForIdle()
        for (offset in listOf(0, 12, 40)) {
            scrolled = offset
            rule.waitForIdle()
            val pixels = rule.onNodeWithTag("screen").captureToImage().toPixelMap()
            val edge = pixels.width - 2
            assertEquals("the title's band at a scroll of $offset", screen.toArgb(), pixels[edge, barBottom - offset - 3].toArgb())
            assertEquals("the content below the bar at a scroll of $offset", Row.toArgb(), pixels[edge, barBottom + 3].toArgb())
        }
        scrolled = 2_000
        rule.waitForIdle()
        val collapsed = rule.onNodeWithTag("screen").captureToImage().toPixelMap()
        assertEquals("once collapsed, content shows where the title was", Row.toArgb(), collapsed[collapsed.width - 2, barBottom - 3].toArgb())
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp")
    fun `in a window held sideways the title is inline from the start`() {
        withAccessory = false
        setBar()
        assertEquals(1, rule.onAllNodesWithText("Inbox").fetchSemanticsNodes().size)
    }

    private companion object {
        val AccessoryHeight = 44.dp
        val Row = Color(0xFFE5332A)
    }
}
