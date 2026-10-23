package com.example.videoalone.core.designsystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.videoalone.core.designsystem.TestSdk
import com.example.videoalone.core.designsystem.component.container.AppScaffold
import com.example.videoalone.core.designsystem.foundation.layout.LocalTopChrome
import com.example.videoalone.core.designsystem.setThemed
import com.example.videoalone.core.designsystem.theme.AppTheme
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

    private var gap = Dp.Unspecified
    private var fontScale by mutableFloatStateOf(1f)

    private fun setBar() {
        rule.setThemed {
            gap = AppTheme.spacing.sm
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) { Bar() }
        }
        rule.waitForIdle()
    }

    @Composable
    private fun Bar() {
        AppScaffold(
            topBar = {
                AppLargeTitleTopBar(
                    title = "Inbox",
                    scrolled = { scrolled },
                    accessory = if (withAccessory) {
                        {
                            SideEffect { accessoryCompositions++ }
                            Box(Modifier.height(AccessoryHeight).fillMaxWidth().testTag("accessory"))
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
    fun `a first row shorter than the title is followed under the bar, not skipped`() {
        val list = LazyListState()
        rule.setThemed {
            LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(top = ListTopPadding)) {
                items(40) { Box(Modifier.fillMaxWidth().height(ShortRow)) }
            }
        }
        val past = with(rule.density) { (ShortRow + 20.dp).roundToPx() }
        rule.runOnIdle { list.dispatchRawDelta(past.toFloat()) }
        rule.waitForIdle()
        // The first visible index has moved on: the row is behind the padding, under the bar.
        assertEquals(1, list.firstVisibleItemIndex)
        assertEquals(past, list.scrolledFromTop())

        val gone = with(rule.density) { ListTopPadding.roundToPx() }
        rule.runOnIdle { list.dispatchRawDelta(gone.toFloat()) }
        rule.waitForIdle()
        assertEquals("once the row has left the list, padding included", Int.MAX_VALUE, list.scrolledFromTop())
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp")
    fun `in a window held sideways the title is inline from the start`() {
        withAccessory = false
        setBar()
        assertEquals(1, rule.onAllNodesWithText("Inbox").fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp")
    fun `in a window held sideways the accessory keeps its gap below the inline bar's hairline`() {
        setBar()
        val top = rule.onNodeWithTag("accessory").getUnclippedBoundsInRoot().top
        assertEquals(TopBarHeight + gap, top)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `at a larger font scale the large title grows with the text, up to its cap`() {
        setBar()
        val regular = titleHeight()
        fontScale = 1.5f
        rule.waitForIdle()
        val larger = titleHeight()
        assertTrue("$regular then $larger", larger >= regular * 1.25f)
        fontScale = 2f
        rule.waitForIdle()
        val largest = titleHeight()
        assertEquals(larger.value, largest.value, 2f)
    }

    private fun titleHeight(): Dp {
        val bounds = rule.onAllNodesWithText("Inbox")[1].getUnclippedBoundsInRoot()
        return bounds.bottom - bounds.top
    }

    private companion object {
        val TopBarHeight = 52.dp
        val AccessoryHeight = 44.dp
        val ListTopPadding = 200.dp
        val ShortRow = 60.dp
        val Row = Color(0xFFE5332A)
    }
}
