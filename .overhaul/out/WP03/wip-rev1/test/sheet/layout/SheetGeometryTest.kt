package com.base.app.core.designsystem.component.sheet.layout

import org.junit.Assert.assertEquals
import org.junit.Test

/** §6.3: floating at a partial detent, attached at Large, one uniform round rect throughout. */
class SheetGeometryTest {

    private val rect = SurfaceRect()

    private fun outline(expansion: Float, floating: Boolean = true, bottomChrome: Float? = null) = SheetGeometry.bottomSheet(
        into = rect,
        frameLeft = 0f,
        frameRight = 393f,
        top = 436f,
        boundsHeight = 873f,
        inset = 8f,
        floating = floating,
        radius = 24f,
        expansion = expansion,
        bottomChrome = bottomChrome,
    )

    @Test
    fun `a floating sheet sits inset at a partial detent`() {
        outline(expansion = 0f)
        assertEquals(8f, rect.left, 0f)
        assertEquals(385f, rect.right, 0f)
        assertEquals(865f, rect.bottom, 0f)
        assertEquals(24f, rect.radius, 0f)
    }

    @Test
    fun `halfway to Large the gaps are half closed`() {
        outline(expansion = 0.5f)
        assertEquals(4f, rect.left, 0f)
        assertEquals((865f + 897f) / 2f, rect.bottom, 0f)
    }

    @Test
    fun `at Large it is attached with its bottom corners below the window`() {
        outline(expansion = 1f)
        assertEquals(0f, rect.left, 0f)
        assertEquals(393f, rect.right, 0f)
        assertEquals(873f + 24f, rect.bottom, 0f)
        assertEquals(24f, rect.radius, 0f)
    }

    @Test
    fun `an attached sheet never shows a gap`() {
        for (expansion in listOf(0f, 0.3f, 1f)) {
            outline(expansion, floating = false)
            assertEquals(0f, rect.left, 0f)
            assertEquals(897f, rect.bottom, 0f)
        }
    }

    @Test
    fun `a sheet resting on the bottom chrome sits on top of it`() {
        outline(expansion = 0f, bottomChrome = 80f)
        assertEquals(873f - 80f - 8f, rect.bottom, 0f)
        outline(expansion = 0f, floating = false, bottomChrome = 80f)
        assertEquals(873f - 80f, rect.bottom, 0f)
    }

    @Test
    fun `a card keeps its size`() {
        SheetGeometry.card(rect, frameLeft = 100f, frameRight = 640f, top = 330f, height = 620f, radius = 24f)
        assertEquals(540f, rect.width, 0f)
        assertEquals(620f, rect.height, 0f)
    }
}
