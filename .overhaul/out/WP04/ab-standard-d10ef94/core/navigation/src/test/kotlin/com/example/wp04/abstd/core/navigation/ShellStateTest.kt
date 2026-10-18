package com.example.wp04.abstd.core.navigation

import com.example.wp04.abstd.core.common.navigation.AppNavKey
import com.example.wp04.abstd.core.navigation.back.StackChange
import com.example.wp04.abstd.core.navigation.back.submit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellStateTest {

    @Test
    fun `switching tabs leaves the other tab's stack alone`() {
        val state = shell()
        state.submit(NavCommand.Navigate(Post(1)))

        state.select(1)
        state.select(0)

        assertEquals(listOf(FeedTab, Post(1)), state.current.keys)
    }

    @Test
    fun `reselecting a tab pops it to its root in one change`() {
        val state = shell(stacks = listOf(listOf(FeedTab, Post(1), Post(2)), listOf(ProfileTab)))

        state.select(0)

        assertEquals(listOf(FeedTab), state.current.keys)
        assertEquals(StackChange.Pop, state.current.gate.lastChange)
    }

    @Test
    fun `reselecting a tab at its root asks its list to scroll to the top`() {
        val state = shell()

        state.select(0)

        assertEquals(1, state.reselections[0].intValue)
    }

    @Test
    fun `reselecting a tab while it moves does nothing`() {
        val state = shell(stacks = listOf(listOf(FeedTab, Post(1)), listOf(ProfileTab)))
        state.current.gate.changed(StackChange.Push)

        state.select(0)

        assertEquals(listOf(FeedTab, Post(1)), state.current.keys)
    }

    @Test
    fun `navigating to another tab's root selects that tab instead of pushing a copy`() {
        val state = shell(stacks = listOf(listOf(FeedTab), listOf(ProfileTab, Post(3))))

        state.submit(NavCommand.Navigate(ProfileTab))

        assertEquals(1, state.selectedIndex)
        assertEquals(listOf(ProfileTab), state.current.keys)
        assertEquals(listOf(FeedTab), state.tabs[0].keys)
    }

    @Test
    fun `a presented key goes to the presentation stack, whichever tab is in front`() {
        val state = shell(selected = 1)

        state.submit(NavCommand.Navigate(Compose))

        assertEquals(listOf(Compose), state.presentedKeys)
        assertEquals(listOf(ProfileTab), state.current.keys)
        assertEquals(state.presented, state.front)
    }

    @Test
    fun `Up dismisses what is presented before it pops a tab`() {
        val state = shell(stacks = listOf(listOf(FeedTab, Post(1)), listOf(ProfileTab)))
        state.submit(NavCommand.Navigate(Compose))
        state.presented.gate.noMotion()

        state.submit(NavCommand.Up)

        assertEquals(emptyList<AppNavKey>(), state.presentedKeys)
        assertEquals(listOf(FeedTab, Post(1)), state.current.keys)
    }

    @Test
    fun `a reset inside a tab keeps the tab's root under the new screen`() {
        val state = shell(stacks = listOf(listOf(FeedTab, Post(1)), listOf(ProfileTab)))

        state.submit(NavCommand.ResetTo(Post(2)))

        assertEquals(listOf(FeedTab, Post(2)), state.current.keys)
    }

    @Test
    fun `popUpTo never removes a tab's root`() {
        val state = shell(stacks = listOf(listOf(FeedTab, Post(1)), listOf(ProfileTab)))

        state.submit(NavCommand.Navigate(Post(2), popUpTo = FeedTab, inclusive = true))

        assertEquals(listOf(FeedTab, Post(2)), state.current.keys)
    }

    @Test
    fun `back at another tab's root goes to the first tab`() {
        val state = shell(selected = 1)

        assertTrue(state.backAtRoot())

        assertEquals(0, state.selectedIndex)
    }

    @Test
    fun `back at the first tab's root is the system's to handle`() {
        val state = shell()

        assertFalse(state.canBackAtRoot)
        assertFalse(state.backAtRoot())
    }

    @Test
    fun `back at a root does not switch tabs while the pop to that root is still moving`() {
        val state = shell(selected = 1)
        state.current.gate.changed(StackChange.Pop)

        assertFalse(state.canBackAtRoot)
    }

    @Test
    fun `a deep link opens on the tab whose feature registered it`() {
        val state = shell(selected = 1)

        state.open(Post(2))

        assertEquals(0, state.selectedIndex)
        assertEquals(listOf(FeedTab, Post(2)), state.current.keys)
    }

    @Test
    fun `a deep link to a tab's root selects that tab`() {
        val state = shell(stacks = listOf(listOf(FeedTab, Post(1)), listOf(ProfileTab)), selected = 1)

        state.open(FeedTab)

        assertEquals(0, state.selectedIndex)
        assertEquals(listOf(FeedTab), state.current.keys)
    }

    @Test
    fun `a deep link to a presented key presents it`() {
        val state = shell(selected = 1)

        state.open(Compose)

        assertEquals(listOf(Compose), state.presentedKeys)
    }
}
