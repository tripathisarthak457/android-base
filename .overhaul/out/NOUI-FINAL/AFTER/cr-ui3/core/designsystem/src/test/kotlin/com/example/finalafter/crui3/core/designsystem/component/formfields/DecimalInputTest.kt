package com.example.finalafter.crui3.core.designsystem.component.formfields

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecimalInputTest {

    @Test
    fun `digits and one point are kept, and grouping and symbols from a paste are dropped`() {
        assertEquals("1234.50", DecimalInput.accept("$1,234.50", maxDecimals = 2, maxIntegerDigits = 7))
        assertNull(DecimalInput.accept("1.2.3", maxDecimals = 2, maxIntegerDigits = 7))
    }

    @Test
    fun `the locale's comma is stored as a point`() {
        assertEquals("12.5", DecimalInput.accept("12,5", maxDecimals = 2, maxIntegerDigits = 7, decimalSeparator = ','))
    }

    @Test
    fun `a keystroke past the limits is refused`() {
        assertNull(DecimalInput.accept("1.234", maxDecimals = 2, maxIntegerDigits = 7))
        assertNull(DecimalInput.accept("12345678", maxDecimals = 2, maxIntegerDigits = 7))
        assertNull(DecimalInput.accept("1.", maxDecimals = 0, maxIntegerDigits = 7))
    }

    @Test
    fun `money is typed the way it is said`() {
        assertEquals(9950L, DecimalInput.toMinor("99.5", digits = 2))
        assertEquals(10000L, DecimalInput.toMinor("100", digits = 2))
        assertNull(DecimalInput.toMinor("", digits = 2))
        assertNull(DecimalInput.toMinor(".", digits = 2))
        assertEquals(1234L, DecimalInput.toMinor("1234", digits = 0))
    }

    @Test
    fun `an amount comes back as the shortest exact text`() {
        assertEquals("99.50", DecimalInput.fromMinor(9950, digits = 2))
        assertEquals("100", DecimalInput.fromMinor(10000, digits = 2))
        assertEquals("1234", DecimalInput.fromMinor(1234, digits = 0))
    }

    @Test
    fun `separators go every three digits from the right`() {
        assertEquals(emptyList<Int>(), DecimalInput.groupingPositions(3))
        assertEquals(listOf(1), DecimalInput.groupingPositions(4))
        assertEquals(listOf(4, 1), DecimalInput.groupingPositions(7))
    }
}
