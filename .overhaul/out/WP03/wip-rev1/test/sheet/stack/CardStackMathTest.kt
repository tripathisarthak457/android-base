package com.base.app.core.designsystem.component.sheet.stack

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** §6.4: the presenter's recede and the one dim that replaces the overlay and the scrim. */
class CardStackMathTest {

    private val lightOverlay = Color.Black
    private val darkOverlay = Color(0xFFC8C8C8)
    private val lightScrim = Color(0x660B0F1A)
    private val darkScrim = Color(0x8F000000)

    @Test
    fun `the presenter recedes to 0_9165 and drops to the safe top`() {
        assertEquals(1f, CardStackMath.scale(0f), 0f)
        assertEquals(0.9165f, CardStackMath.scale(1f), 0.0001f)
        assertEquals((1f + 0.9165f) / 2f, CardStackMath.scale(0.5f), 0.0001f)
        assertEquals(16.5f, CardStackMath.translationY(0.5f, safeTop = 33f), 0f)
    }

    @Test
    fun `corners start from the display's and end at the card's`() {
        assertEquals(0.9f * 30f, CardStackMath.corner(0f, displayCorner = 30f, density = 1f), 0.001f)
        assertEquals(0f, CardStackMath.corner(0f, displayCorner = 12f, density = 1f), 0f)
        assertEquals(12f, CardStackMath.corner(1f, displayCorner = 30f, density = 1f), 0.001f)
    }

    @Test
    fun `the merged dim equals the overlay under the scrim, drawn as two rects`() {
        for ((overlay, scrim) in listOf(lightOverlay to lightScrim, darkOverlay to darkScrim)) {
            for (u in listOf(0f, 0.3f, 1f)) {
                for (l in listOf(0f, 0.5f, 1f)) {
                    val merged = CardStackMath.mergedDim(u, l, overlay, overlayAlpha = 0.1f, scrim = scrim)
                    val base = Color(0.2f, 0.6f, 0.9f)
                    val expected = over(scrim.copy(alpha = scrim.alpha * u), over(overlay.copy(alpha = 0.1f * l), base))
                    val actual = over(merged, base)
                    assertEquals(expected.red, actual.red, 1f / 255f)
                    assertEquals(expected.green, actual.green, 1f / 255f)
                    assertEquals(expected.blue, actual.blue, 1f / 255f)
                }
            }
        }
    }

    @Test
    fun `the dim never lightens as the sheet rises`() {
        var last = 0f
        for (step in 0..10) {
            val e = step / 10f
            val alpha = CardStackMath.mergedDim(1f, e, lightOverlay, 0.1f, lightScrim).alpha
            assertTrue(alpha >= last)
            last = alpha
        }
        assertEquals(0.46f, last, 0.01f)
    }

    @Test
    fun `nothing to draw is transparent`() {
        assertEquals(Color.Transparent, CardStackMath.mergedDim(0f, 0f, lightOverlay, 0.1f, lightScrim))
    }

    /** Source-over of [top] onto an opaque [bottom]. */
    private fun over(top: Color, bottom: Color) = Color(
        red = top.red * top.alpha + bottom.red * (1f - top.alpha),
        green = top.green * top.alpha + bottom.green * (1f - top.alpha),
        blue = top.blue * top.alpha + bottom.blue * (1f - top.alpha),
    )
}
