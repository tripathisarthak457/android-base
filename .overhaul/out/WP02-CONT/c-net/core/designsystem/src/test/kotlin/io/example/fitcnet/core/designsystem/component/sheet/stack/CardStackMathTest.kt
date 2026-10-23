package io.example.fitcnet.core.designsystem.component.sheet.stack

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The presenter's recede and the one dim that replaces the overlay and every scrim. */
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
    fun `B-54 a covered sheet scales to 0_9165 and lifts half a percent of the height`() {
        assertEquals(0.9165f, CardStackMath.coveredScale(1f), 0.0001f)
        assertEquals(4.365f, CardStackMath.coveredLift(1f, boundsHeight = 873f), 0.001f)
        assertEquals(0f, CardStackMath.coveredLift(0f, boundsHeight = 873f), 0f)
    }

    @Test
    fun `the merged dim equals the overlay under the scrim, drawn as two rects`() {
        for ((overlay, scrim) in listOf(lightOverlay to lightScrim, darkOverlay to darkScrim)) {
            for (u in listOf(0f, 0.3f, 1f)) {
                for (l in listOf(0f, 0.5f, 1f)) {
                    val merged = fold(overlay to 0.1f * l, scrim to scrim.alpha * u)
                    for (base in listOf(Color(0.2f, 0.6f, 0.9f), Color.White, Color.Black)) {
                        val expected = over(scrim.copy(alpha = scrim.alpha * u), over(overlay.copy(alpha = 0.1f * l), base))
                        assertSame(expected, over(merged, base))
                    }
                }
            }
        }
    }

    @Test
    fun `the fold of several scrims equals drawing them one by one`() {
        val layers = listOf(lightOverlay to 0.1f, lightScrim to 0.25f, darkScrim to 0.3f)
        val merged = fold(*layers.toTypedArray())
        val base = Color(0.4f, 0.4f, 0.1f)
        var expected = base
        for ((color, alpha) in layers) expected = over(color.copy(alpha = alpha), expected)
        assertSame(expected, over(merged, base))
    }

    @Test
    fun `F6 the dim never lightens as the sheet rises or the scrim strengthens`() {
        var last = 0f
        for (step in 0..10) {
            val e = step / 10f
            val alpha = fold(lightOverlay to 0.1f * e, lightScrim to lightScrim.alpha).alpha
            assertTrue(alpha >= last)
            last = alpha
        }
        assertEquals(0.46f, last, 0.01f)
        var lastU = 0f
        for (step in 0..10) {
            val alpha = fold(lightOverlay to 0.05f, lightScrim to lightScrim.alpha * step / 10f).alpha
            assertTrue(alpha >= lastU)
            lastU = alpha
        }
    }

    @Test
    fun `nothing to draw is transparent`() {
        assertEquals(Color.Transparent, fold(lightOverlay to 0f, lightScrim to 0f))
    }

    private fun fold(vararg layers: Pair<Color, Float>): Color {
        val fold = DimFold()
        for ((color, alpha) in layers) fold.over(color, alpha)
        return fold.color()
    }

    private fun assertSame(expected: Color, actual: Color) {
        assertEquals(expected.red, actual.red, 1f / 255f)
        assertEquals(expected.green, actual.green, 1f / 255f)
        assertEquals(expected.blue, actual.blue, 1f / 255f)
    }

    /** Source-over of [top] onto an opaque [bottom]. */
    private fun over(top: Color, bottom: Color) = Color(
        red = top.red * top.alpha + bottom.red * (1f - top.alpha),
        green = top.green * top.alpha + bottom.green * (1f - top.alpha),
        blue = top.blue * top.alpha + bottom.blue * (1f - top.alpha),
    )
}
