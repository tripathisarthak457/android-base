package com.example.finalafter.crui.core.perfhud.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class LineFormatTest {

    @Test
    fun `numbers go where the format puts them`() {
        val line = LineFormat("%1\$d/%2\$d fps")

        line.write(118, 120)

        assertEquals("118/120 fps", line.text())
    }

    @Test
    fun `arguments can come in any order, and a percent sign is written once`() {
        val line = LineFormat("%2\$d%% of %1\$d")

        line.write(120, 5)

        assertEquals("5% of 120", line.text())
    }

    @Test
    fun `a shorter number after a longer one leaves nothing of it behind`() {
        val line = LineFormat("%1\$d.%2\$d%% janky")
        line.write(100, 0)

        line.write(0, 4)

        assertEquals("0.4% janky", line.text())
    }

    @Test
    fun `negative and extreme numbers are written whole`() {
        val line = LineFormat("%1\$d %2\$d")

        line.write(Int.MIN_VALUE, Int.MAX_VALUE)

        assertEquals("-2147483648 2147483647", line.text())
    }

    @Test
    fun `text around the numbers is kept as it is`() {
        val line = LineFormat("idle · %1\$d Hz")

        line.write(60)

        assertEquals("idle · 60 Hz", line.text())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a conversion other than whole numbers is refused`() {
        LineFormat("%1\$s janky")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a third argument is refused`() {
        LineFormat("%1\$d %2\$d %3\$d")
    }

    private fun LineFormat.text(): String = String(chars, 0, length)
}
