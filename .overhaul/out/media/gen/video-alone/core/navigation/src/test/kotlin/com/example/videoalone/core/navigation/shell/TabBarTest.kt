package com.example.videoalone.core.navigation.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.videoalone.core.designsystem.component.navigation.AppTabBar
import com.example.videoalone.core.designsystem.component.navigation.AppTabBarState
import com.example.videoalone.core.designsystem.component.navigation.BarMinimize
import com.example.videoalone.core.designsystem.component.navigation.BarOrientation
import com.example.videoalone.core.designsystem.component.navigation.TabItem
import com.example.videoalone.core.designsystem.component.navigation.rememberAppTabBarState
import com.example.videoalone.core.designsystem.icon.AppIcons
import com.example.videoalone.core.designsystem.theme.AppTheme
import com.example.videoalone.core.designsystem.theme.BarTreatment
import com.example.videoalone.core.navigation.ROBOLECTRIC_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The tab bar the shell draws: what accessibility reads, mirroring, large text, the rail in a short window, minimize. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class TabBarTest {

    @get:Rule
    val rule = createComposeRule()

    private val five = listOf("Home", "Feed", "Search", "Profile", "Settings").map { TabItem(it, AppIcons.Home) }

    @Test
    fun `each tab is read out once, the selected one as selected`() {
        var selected = 1
        rule.setContent {
            AppTheme { AppTabBar(five, selected = selected, onSelect = { selected = it }, onReselect = {}) }
        }

        rule.onAllNodesWithText("Feed").assertCountEquals(1)
        rule.onNodeWithTag("tab_1").assertIsSelected()
        rule.onNodeWithTag("tab_3").assertIsNotSelected().performClick()
        assertEquals(3, selected)
    }

    @Test
    fun `right to left, the first tab is at the right end`() {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AppTheme { AppTabBar(five, selected = 0, onSelect = {}, onReselect = {}) }
            }
        }

        val first = rule.onNodeWithTag("tab_0").fetchSemanticsNode().boundsInRoot
        val last = rule.onNodeWithTag("tab_4").fetchSemanticsNode().boundsInRoot
        assertTrue(first.left > last.left)
    }

    @Test
    fun `at twice the font size every label stays inside its slot's pill`() {
        val long = five.map { it.copy(label = it.label + " and more") }
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                AppTheme {
                    Box(Modifier.width(360.dp)) { AppTabBar(long, selected = 4, onSelect = {}, onReselect = {}) }
                }
            }
        }

        // The pill keeps 4 dp from the slot's edge and at least 4 dp of padding around the label.
        val inset = 8 * rule.density.density
        long.forEachIndexed { index, item ->
            val cell = rule.onNodeWithTag("tab_$index").fetchSemanticsNode().boundsInRoot
            rule.onAllNodesWithText(item.label, useUnmergedTree = true).fetchSemanticsNodes().forEach { label ->
                assertTrue("${item.label} starts outside its pill", label.boundsInRoot.left >= cell.left + inset - 1)
                assertTrue("${item.label} ends outside its pill", label.boundsInRoot.right <= cell.right - inset + 1)
            }
        }
    }

    @Test
    fun `at a large font a docked bar's labels keep clear of the screen's edges and shrink to fit, not cut`() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                AppTheme {
                    Box(Modifier.width(411.dp)) {
                        AppTabBar(five, selected = 0, onSelect = {}, onReselect = {}, treatment = BarTreatment.Docked)
                    }
                }
            }
        }

        // The bar's 8 dp inset, the pill's 4 dp from its slot and 4 dp of padding inside it.
        val edge = 16 * rule.density.density
        val width = 411 * rule.density.density
        assertTrue("Home runs into the leading edge", label("Home").boundsInRoot.left >= edge - 1)
        assertTrue("Settings runs into the trailing edge", label("Settings").boundsInRoot.right <= width - edge + 1)
        five.forEach { item ->
            val layouts = mutableListOf<TextLayoutResult>()
            label(item.label).config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            assertFalse("${item.label} is cut", layouts.single().isLineEllipsized(0))
        }
    }

    private fun label(text: String) = rule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().first()

    @Test
    fun `a rail in a short window keeps every tab within reach`() {
        val seven = five + listOf(TabItem("Orders", AppIcons.Home), TabItem("Wallet", AppIcons.Home))
        rule.setContent {
            AppTheme {
                Box(Modifier.height(360.dp)) {
                    AppTabBar(seven, selected = 0, onSelect = {}, onReselect = {}, orientation = BarOrientation.Vertical)
                }
            }
        }

        val height = rule.onNodeWithTag("tab_0").fetchSemanticsNode().boundsInRoot.height
        val density = rule.density.density
        assertTrue("items never shrink below 56 dp", height >= 56 * density - 1)
        rule.onNodeWithTag("tab_6").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the bar folds only past the hysteresis, and opens on the way back up`() {
        lateinit var state: AppTabBarState
        lateinit var density: Density
        rule.setContent {
            state = rememberAppTabBarState()
            density = LocalDensity.current
            AppTheme {
                AppTabBar(
                    five, selected = 0, onSelect = {}, onReselect = {},
                    treatment = BarTreatment.Floating, minimize = BarMinimize.OnScrollDown, state = state,
                )
            }
        }

        scroll(state, readOn = with(density) { 6.dp.toPx() })
        assertEquals("a nudge leaves the bar alone", 0f, state.collapse)

        scroll(state, readOn = with(density) { 6.dp.toPx() })
        assertEquals(1f, state.collapse)

        scroll(state, readOn = -with(density) { 11.dp.toPx() })
        assertEquals(0f, state.collapse)
    }

    private fun scroll(state: AppTabBarState, readOn: Float) {
        rule.runOnIdle {
            state.nestedScrollConnection.onPostScroll(Offset(0f, -readOn), Offset.Zero, NestedScrollSource.UserInput)
        }
        rule.waitForIdle()
    }
}
