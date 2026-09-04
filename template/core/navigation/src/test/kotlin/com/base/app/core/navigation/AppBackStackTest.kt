package com.base.app.core.navigation

import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test

@Serializable
private data object Home : AppNavKey

@Serializable
private data class Detail(val id: Int) : AppNavKey

@Serializable
private data object Login : AppNavKey

/**
 * The navigation semantics, tested without a composition.
 *
 * This is the whole reason [AppBackStack.apply] lives on the stack rather than inside the host
 * composable: the rules that actually matter — what singleTop does, what popUpTo removes, that
 * the stack can never empty — are testable as plain data here, and would need an instrumented
 * test with a real display otherwise.
 */
class AppBackStackTest {

    private fun stackOf(vararg keys: AppNavKey) =
        AppBackStack(mutableStateListOf(*keys))

    @Test
    fun `navigate pushes onto the stack`() {
        val stack = stackOf(Home)

        stack.apply(NavCommand.Navigate(Detail(1)))

        assertEquals(listOf(Home, Detail(1)), stack.entries.toList())
    }

    @Test
    fun `singleTop does not stack a second copy of the current destination`() {
        val stack = stackOf(Home, Detail(1))

        stack.apply(NavCommand.Navigate(Detail(1)))

        assertEquals(listOf(Home, Detail(1)), stack.entries.toList())
    }

    @Test
    fun `singleTop still pushes a different instance of the same type`() {
        val stack = stackOf(Home, Detail(1))

        stack.apply(NavCommand.Navigate(Detail(2)))

        assertEquals(listOf(Home, Detail(1), Detail(2)), stack.entries.toList())
    }

    @Test
    fun `popUpTo inclusive removes the target too`() {
        val stack = stackOf(Login, Home, Detail(1))

        stack.apply(NavCommand.Navigate(Home, popUpTo = Login, inclusive = true))

        assertEquals(listOf(Home), stack.entries.toList())
    }

    @Test
    fun `popUpTo exclusive keeps the target`() {
        val stack = stackOf(Home, Detail(1), Detail(2))

        stack.apply(NavCommand.PopTo(Home))

        assertEquals(listOf(Home), stack.entries.toList())
    }

    @Test
    fun `popTo finds the most recent occurrence, not the first`() {
        val stack = stackOf(Home, Detail(1), Home, Detail(2))

        stack.apply(NavCommand.PopTo(Home))

        assertEquals(listOf(Home, Detail(1), Home), stack.entries.toList())
    }

    @Test
    fun `popTo a destination that is not on the stack does nothing`() {
        val stack = stackOf(Home, Detail(1))

        stack.apply(NavCommand.PopTo(Login))

        assertEquals(listOf(Home, Detail(1)), stack.entries.toList())
    }

    @Test
    fun `up at the root is ignored so the stack can never empty`() {
        val stack = stackOf(Home)

        stack.apply(NavCommand.Up)

        assertEquals(listOf(Home), stack.entries.toList())
    }

    @Test
    fun `popUpTo inclusive on the only entry still leaves one`() {
        val stack = stackOf(Home)

        stack.apply(NavCommand.PopTo(Home, inclusive = true))

        assertEquals(listOf(Home), stack.entries.toList())
    }

    @Test
    fun `resetTo drops everything`() {
        val stack = stackOf(Home, Detail(1), Detail(2))

        stack.apply(NavCommand.ResetTo(Login))

        assertEquals(listOf(Login), stack.entries.toList())
    }
}
