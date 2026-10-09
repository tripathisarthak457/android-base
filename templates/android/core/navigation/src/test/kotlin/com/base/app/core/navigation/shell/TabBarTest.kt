package com.base.app.core.navigation.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.navigation.AppTabBar
import com.base.app.core.designsystem.component.navigation.AppTabBarState
import com.base.app.core.designsystem.component.navigation.BarMinimize
import com.base.app.core.designsystem.component.navigation.BarOrientation
import com.base.app.core.designsystem.component.navigation.TabItem
import com.base.app.core.designsystem.component.navigation.rememberAppTabBarState
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.BarTreatment
import com.base.app.core.navigation.ROBOLECTRIC_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The tab bar the shell draws: what accessibility reads, mirroring, the rail in a short window, minimize. */
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
