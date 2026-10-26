package com.example.finalafter.crui3.core.designsystem.foundation.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GpuBudgetWindowTest {

    private val period = 8_333_333L

    @Test
    fun `two consecutive windows over the share decide that the GPU fell behind`() {
        val window = GpuBudgetWindow()

        val first = window.feed(frames = 120, slow = 30)
        val second = window.feed(frames = 120, slow = 30)

        assertEquals(listOf(false), first.distinct())
        assertEquals(true, second.last())
        assertFalse(second.dropLast(1).any { it })
    }

    @Test
    fun `a clean window in between starts the count again`() {
        val window = GpuBudgetWindow()

        val verdicts = window.feed(frames = 120, slow = 30) +
            window.feed(frames = 120, slow = 0) +
            window.feed(frames = 120, slow = 30)

        assertFalse(verdicts.any { it })
    }

    @Test
    fun `a window at exactly the share is not over it`() {
        val window = GpuBudgetWindow()

        val verdicts = window.feed(frames = 120, slow = 18) + window.feed(frames = 120, slow = 18)

        assertFalse(verdicts.any { it })
    }

    @Test
    fun `one burst of heavy frames, however slow, does not decide`() {
        val window = GpuBudgetWindow()

        val verdicts = window.feed(frames = 120, slow = 120) + window.feed(frames = 240, slow = 0)

        assertFalse(verdicts.any { it })
    }

    @Test
    fun `a frame that takes exactly one period is not slow`() {
        val window = GpuBudgetWindow(windowFrames = 1, slowShare = 0f, badWindowsInARow = 1)

        assertFalse(window.record(gpuNanos = period, budgetNanos = period))
        assertEquals(true, window.record(gpuNanos = period + 1, budgetNanos = period))
    }

    /** Feeds [frames] frames, the first [slow] of them longer than a period; one verdict each. */
    private fun GpuBudgetWindow.feed(frames: Int, slow: Int): List<Boolean> =
        List(frames) { index ->
            record(gpuNanos = if (index < slow) period * 2 else period / 2, budgetNanos = period)
        }
}
