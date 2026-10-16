package com.base.app.core.navigation

import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@Serializable
private data object FeedTab : AppNavKey

@Serializable
private data object ProfileTab : AppNavKey

@Serializable
private data class Post(val id: Int) : AppNavKey

class ShellStateTest {

    private fun shell(selected: Int = 0) = ShellState(
        stacks = listOf(mutableStateListOf<AppNavKey>(FeedTab), mutableStateListOf<AppNavKey>(ProfileTab)),
        initialTab = selected,
    )

    @Test
    fun `switching tabs leaves the other tab's stack alone`() {
        val state = shell()
        state.current.apply(NavCommand.Navigate(Post(1)))

        state.select(1)
        state.select(0)

        assertEquals(listOf(FeedTab, Post(1)), state.current.entries.toList())
    }

    @Test
    fun `re-tapping the active tab returns it to its root`() {
        val state = shell()
        state.current.apply(NavCommand.Navigate(Post(1)))
        state.current.apply(NavCommand.Navigate(Post(2)))

        state.select(0)

        assertEquals(listOf(FeedTab), state.current.entries.toList())
    }

    @Test
    fun `back inside a tab pops that tab`() {
        val state = shell(selected = 1)
        state.current.apply(NavCommand.Navigate(Post(3)))

        state.handleBack(onExitRequested = {})

        assertEquals(1, state.selectedIndex)
        assertEquals(listOf(ProfileTab), state.current.entries.toList())
    }

    @Test
    fun `back at another tab's root goes to the first tab`() {
        val state = shell(selected = 1)

        state.handleBack(onExitRequested = {})

        assertEquals(0, state.selectedIndex)
    }

    @Test
    fun `back at the first tab's root is the app's to decide`() {
        val state = shell()
        var exited = false

        state.handleBack(onExitRequested = { exited = true })

        assertTrue(exited)
    }
}
