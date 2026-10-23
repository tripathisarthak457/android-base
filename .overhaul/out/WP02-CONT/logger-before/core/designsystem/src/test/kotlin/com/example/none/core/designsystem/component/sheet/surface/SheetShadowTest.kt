package com.example.none.core.designsystem.component.sheet.surface

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The nine slices around a surface of any size; corners never scale, the centre is never drawn. */
class SheetShadowTest {

    private val slice = NineSlice(padding = 30, corner = 24)
    private val out = IntArray(4)

    @Test
    fun `the bitmap holds a small rect with its padding`() {
        assertEquals(2 * 24 + NineSlice.MiddlePx, slice.rectSide)
        assertEquals(slice.rectSide + 60, slice.bitmapSide)
        assertEquals(54, slice.cut)
    }

    @Test
    fun `corners keep their size whatever the surface`() {
        for ((width, height) in listOf(377 to 400, 393 to 830, 600 to 620)) {
            for (corner in listOf(0, 2, 6, 8)) {
                assertTrue(slice.destination(corner, 0, 0, width, height, out))
                assertEquals(slice.cut, out[2])
                assertEquals(slice.cut, out[3])
                slice.source(corner, out)
                assertEquals(slice.cut, out[2])
                assertEquals(slice.cut, out[3])
            }
        }
    }

    @Test
    fun `edges stretch along their axis to meet the corners`() {
        assertTrue(slice.destination(1, 8, 100, 385, 865, out))
        assertEquals(8 - 30 + 54, out[0])
        assertEquals((385 + 30) - (8 - 30) - 2 * 54, out[2])
        assertEquals(54, out[3])
        assertTrue(slice.destination(3, 8, 100, 385, 865, out))
        assertEquals(54, out[2])
        assertEquals((865 + 30) - (100 - 30) - 2 * 54, out[3])
    }

    @Test
    fun `the centre is never drawn`() {
        assertFalse(slice.destination(4, 0, 0, 400, 400, out))
    }

    @Test
    fun `a surface too small for an edge draws no edge`() {
        assertFalse(slice.destination(1, 0, 0, 40, 400, out))
    }

    @Test
    fun `sources cut corners and a two-pixel middle out of the bitmap`() {
        slice.source(1, out)
        assertEquals(54, out[0])
        assertEquals(NineSlice.MiddlePx, out[2])
        slice.source(8, out)
        assertEquals(slice.bitmapSide - 54, out[0])
        assertEquals(slice.bitmapSide - 54, out[1])
    }
}
