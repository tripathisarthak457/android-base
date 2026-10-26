package com.example.finalafter.crnoui.core.perfhud.frame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenTableTest {

    private val table = ScreenTable()

    @Test
    fun `frames before any screen go to the unknown index`() {
        assertEquals(ScreenTable.UNKNOWN, table.indexOf(null))
        assertNull(table.nameAt(ScreenTable.UNKNOWN))
    }

    @Test
    fun `a name keeps its index, whether or not it is the same string instance`() {
        val home = table.indexOf("Home")
        val detail = table.indexOf("Detail")

        assertEquals(home, table.indexOf(String(charArrayOf('H', 'o', 'm', 'e'))))
        assertEquals(detail, table.indexOf("Detail"))
        assertEquals("Home", table.nameAt(home))
        assertEquals(3, table.size)
    }

    @Test
    fun `screens past the capacity share the other index`() {
        repeat(ScreenTable.CAPACITY - 2) { table.indexOf("screen$it") }

        assertEquals(ScreenTable.OTHER, table.indexOf("one too many"))
        assertEquals(ScreenTable.OTHER, table.indexOf("and another"))
        assertEquals(1, table.indexOf("screen0"))
        assertNull(table.nameAt(ScreenTable.OTHER))
    }

    @Test
    fun `clear forgets every name`() {
        table.indexOf("Home")

        table.clear()

        assertEquals(1, table.size)
        assertEquals(1, table.indexOf("Detail"))
    }
}
