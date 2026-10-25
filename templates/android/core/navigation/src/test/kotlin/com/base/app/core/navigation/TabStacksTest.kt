package com.base.app.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.navigation.back.StackChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tabs that each keep their own history, without a shell to draw them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class TabStacksTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun tabs(
        vararg stacks: List<AppNavKey> = arrayOf(listOf(FeedTab), listOf(ProfileTab)),
        selected: Int = 0,
    ) = TabStacks(roots = stacks.map { it.first() }, stacks = stacks.toList(), selected = selected)

    @Test
    fun `selecting another tab shows its stack as it was left`() {
        val tabs = tabs(listOf(FeedTab, Post(1)), listOf(ProfileTab, Post(2)))

        assertTrue(tabs.select(1))

        assertEquals(1, tabs.selectedIndex)
        assertEquals(listOf(ProfileTab, Post(2)), tabs.current.keys)
        assertEquals(listOf(FeedTab, Post(1)), tabs.stacks[0].keys)
    }

    @Test
    fun `reselecting the tab in front pops it to its root in one change`() {
        val tabs = tabs(listOf(FeedTab, Post(1), Post(2)), listOf(ProfileTab))

        assertFalse(tabs.select(0))

        assertEquals(listOf(FeedTab), tabs.current.keys)
        assertEquals(StackChange.Pop, tabs.current.gate.lastChange)
        assertEquals(0, tabs.reselections(0).intValue)
    }

    @Test
    fun `reselecting the tab in front at its root counts a reselection`() {
        val tabs = tabs()

        tabs.select(0)
        tabs.select(0)

        assertEquals(2, tabs.reselections(0).intValue)
        assertEquals(0, tabs.reselections(1).intValue)
        assertEquals(listOf(FeedTab), tabs.current.keys)
    }

    @Test
    fun `reselecting the tab in front while it moves does nothing`() {
        val tabs = tabs(listOf(FeedTab, Post(1)), listOf(ProfileTab))
        tabs.current.gate.changed(StackChange.Push)

        tabs.select(0)

        assertEquals(listOf(FeedTab, Post(1)), tabs.current.keys)
        assertEquals(0, tabs.reselections(0).intValue)
    }

    @Test
    fun `a tab that is not there selects nothing`() {
        val tabs = tabs()

        assertFalse(tabs.select(2))

        assertEquals(0, tabs.selectedIndex)
    }

    @Test
    fun `every stack and the tab in front survive a restore`() {
        val restoration = StateRestorationTester(rule)
        var tabs: TabStacks? = null
        restoration.setContent {
            tabs = rememberTabStacks(roots = listOf(FeedTab, ProfileTab), serialization = testSerialization)
        }
        rule.runOnIdle {
            tabs!!.current.apply(NavCommand.Navigate(Post(1)))
            tabs!!.select(1)
            tabs!!.current.apply(NavCommand.Navigate(Post(2)))
        }
        val before = tabs

        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        val restored = tabs!!
        assertNotSame("the tabs were rebuilt from what was saved", before, restored)
        assertEquals(1, restored.selectedIndex)
        assertEquals(listOf(listOf(FeedTab, Post(1)), listOf(ProfileTab, Post(2))), restored.stacks.map { it.keys })
    }

    @Test
    fun `reordered tabs each get their own stack back, and the tab in front stays in front`() {
        val restoration = StateRestorationTester(rule)
        var roots = listOf<AppNavKey>(FeedTab, ProfileTab)
        var tabs: TabStacks? = null
        restoration.setContent {
            tabs = rememberTabStacks(roots = roots, serialization = testSerialization)
        }
        rule.runOnIdle { tabs!!.current.apply(NavCommand.Navigate(Post(1))) }
        roots = roots.reversed()

        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        assertEquals(listOf(listOf(ProfileTab), listOf(FeedTab, Post(1))), tabs!!.stacks.map { it.keys })
        assertEquals(1, tabs!!.selectedIndex)
    }
}
