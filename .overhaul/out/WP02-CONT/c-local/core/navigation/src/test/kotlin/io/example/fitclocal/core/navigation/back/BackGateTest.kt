package io.example.fitclocal.core.navigation.back

import androidx.compose.runtime.mutableStateListOf
import io.example.fitclocal.core.navigation.AppBackStack
import io.example.fitclocal.core.navigation.FeedTab
import io.example.fitclocal.core.navigation.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackGateTest {

    private val stack = AppBackStack(mutableStateListOf(FeedTab, Post(1), Post(2), Post(3)))
    private val gate = stack.gate
    private val pop = { count: Int -> stack.pop(count) }

    @Test
    fun `a press at rest pops one screen and holds the gate until the transition starts`() {
        gate.onBack(pop)

        assertEquals(listOf(FeedTab, Post(1), Post(2)), stack.keys)
        assertTrue(gate.moving)
    }

    @Test
    fun `presses while the screens move are counted, then popped together`() {
        gate.onBack(pop)
        val transition = Any()
        gate.report(transition, isMoving = true)
        gate.onBack(pop)
        gate.onBack(pop)

        assertEquals(listOf(FeedTab, Post(1), Post(2)), stack.keys)

        gate.report(transition, isMoving = false)
        gate.settle(pop)

        assertEquals(listOf(FeedTab), stack.keys)
        assertEquals(0, gate.counted)
    }

    @Test
    fun `a press in the frame before the transition starts is counted, not applied`() {
        gate.onBack(pop)
        gate.onBack(pop)

        assertEquals(listOf(FeedTab, Post(1), Post(2)), stack.keys)
        assertEquals(1, gate.counted)
    }

    @Test
    fun `presses past the root are dropped, so a burst never leaves the app`() {
        gate.onBack(pop)
        repeat(6) { gate.onBack(pop) }
        gate.noMotion()
        gate.settle(pop)

        assertEquals(listOf(FeedTab), stack.keys)
        assertFalse(stack.canGoBack)
    }

    @Test
    fun `a change with no transition does not keep Back counted`() {
        gate.onBack(pop)
        gate.noMotion()

        assertFalse(gate.moving)
        gate.onBack(pop)
        assertEquals(listOf(FeedTab, Post(1)), stack.keys)
    }

    @Test
    fun `counting stops at its cap`() {
        val capped = BackGate(maxCoalesced = 2)
        capped.changed(StackChange.Push)
        repeat(5) { capped.onBack(pop) }

        assertEquals(2, capped.counted)
    }
}
