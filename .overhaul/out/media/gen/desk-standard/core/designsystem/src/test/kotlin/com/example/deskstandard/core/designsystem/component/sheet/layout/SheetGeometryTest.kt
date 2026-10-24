package com.example.deskstandard.core.designsystem.component.sheet.layout

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * In the sheet layer's own coordinates: floating at a partial detent, attached at Large, one
 * uniform round rect throughout, constant while attached. An 873 dp tall window at density 1, inset 8, radius 24.
 */
class SheetGeometryTest {

    private val height = 873f
    private val radius = 24f
    private val inset = 8f

    private fun bottom(expansion: Float, placedTop: Float, onBar: Boolean = false, restBottom: Float = 857f): Float {
        val resting = SheetGeometry.restingBottom(height, restBottom, inset, floats = true, radius = radius, onBar = onBar)
        return SheetGeometry.floatingBottom(resting, height, radius, expansion, placedTop)
    }

    @Test
    fun `a floating sheet sits inset at a partial detent`() {
        assertEquals(8f, SheetGeometry.sideInset(inset, SheetGeometry.floatAmount(true, 0f)), 0f)
        assertEquals(865f - 436f, bottom(expansion = 0f, placedTop = 436f), 0f)
    }

    @Test
    fun `halfway to Large the gaps are half closed`() {
        assertEquals(4f, SheetGeometry.sideInset(inset, SheetGeometry.floatAmount(true, 0.5f)), 0f)
        assertEquals((865f + 897f) / 2f - 240f, bottom(expansion = 0.5f, placedTop = 240f), 0f)
    }

    @Test
    fun `at Large it is attached with its bottom corners below the window`() {
        assertEquals(0f, SheetGeometry.sideInset(inset, SheetGeometry.floatAmount(true, 1f)), 0f)
        assertEquals(897f - 43f, bottom(expansion = 1f, placedTop = 43f), 0f)
    }

    @Test
    fun `the floating bottom stays put in the window while the sheet moves`() {
        for (top in listOf(436f, 420f, 400f)) assertEquals(865f, bottom(expansion = 0f, placedTop = top) + top, 0f)
    }

    @Test
    fun `an attached sheet's outline is constant and reaches past the window however far it is pulled up`() {
        assertEquals(0f, SheetGeometry.sideInset(inset, SheetGeometry.floatAmount(false, 0.3f)), 0f)
        val layer = height - 43f
        val band = 0.008f * height
        val attached = SheetGeometry.attachedBottom(layer, radius, band)
        assertEquals(layer + radius + band, attached, 0f)
        // Pulled up by the whole band, the layer's bottom is `band` above the window: the surface still reaches past it.
        assertEquals(true, attached - band >= layer)
    }

    @Test
    fun `B-52 a sheet resting on the tab bar sits on top of it`() {
        assertEquals(873f - 80f - 8f, SheetGeometry.restingBottom(height, 793f, inset, true, radius, onBar = true), 0f)
        assertEquals(873f - 80f, SheetGeometry.restingBottom(height, 793f, inset, false, radius, onBar = true), 0f)
        assertEquals(873f + 24f, SheetGeometry.restingBottom(height, 857f, inset, false, radius, onBar = false), 0f)
    }

    @Test
    fun `the rect rewrites in place and reports when it is unchanged`() {
        val rect = SurfaceRect()
        rect.set(4f, 0f, 389f, 400f, 24f)
        assertEquals(385f, rect.width, 0f)
        assertEquals(true, rect.same(4f, 0f, 389f, 400f, 24f))
        assertEquals(false, rect.same(4f, 0f, 389f, 401f, 24f))
    }
}
