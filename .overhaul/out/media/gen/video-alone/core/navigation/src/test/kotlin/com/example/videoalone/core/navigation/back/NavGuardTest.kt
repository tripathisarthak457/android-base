package com.example.videoalone.core.navigation.back

import androidx.compose.runtime.mutableStateListOf
import com.example.videoalone.core.navigation.AppBackStack
import com.example.videoalone.core.navigation.FeedTab
import com.example.videoalone.core.navigation.NavCommand
import com.example.videoalone.core.navigation.Post
import org.junit.Assert.assertEquals
import org.junit.Test

/** What happens to a command that arrives while its host is moving. */
class NavGuardTest {

    private val stack = AppBackStack(mutableStateListOf(FeedTab, Post(1)))

    @Test
    fun `the destination already on top is never pushed again`() {
        assertEquals(Verdict.Drop, guard(NavCommand.Navigate(Post(1)), stack))
    }

    @Test
    fun `a push at rest applies`() {
        assertEquals(Verdict.Apply, guard(NavCommand.Navigate(Post(2)), stack))
    }

    @Test
    fun `a second push during a push is dropped, so the first row tapped wins`() {
        stack.gate.changed(StackChange.Push)

        assertEquals(Verdict.Drop, guard(NavCommand.Navigate(Post(2)), stack))
    }

    @Test
    fun `a push during a pop waits for it to settle`() {
        stack.gate.changed(StackChange.Pop)

        assertEquals(Verdict.Wait, guard(NavCommand.Navigate(Post(2)), stack))
    }

    @Test
    fun `Up and pop-to from an effect wait for a running push`() {
        stack.gate.changed(StackChange.Push)

        assertEquals(Verdict.Wait, guard(NavCommand.Up, stack))
        assertEquals(Verdict.Wait, guard(NavCommand.PopTo(FeedTab), stack))
    }
}
