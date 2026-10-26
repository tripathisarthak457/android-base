package com.base.app.core.designsystem.foundation.draw

import com.base.app.core.designsystem.theme.tokens.DesignTokens.Motion
import kotlin.math.tan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The band's place, worked out from each bone's own origin as the bones draw it, against where the
 * one band of the sweep is in the area. A bone that swept a band of its own, sized or timed by its
 * width, would put the band somewhere else.
 */
class SweepGeometryTest {

    private val density = 2.75f
    private val width = 393 * density
    private val height = 873 * density
    private val band = Motion.skeletonBandWidth.value * density

    @Test
    fun `two bones at different places sample the same band`() {
        for (progress in listOf(0.2f, 0.45f, 0.7f)) {
            // An 80 dp line on the left and a 300 dp card further right, on the same row.
            val near =
                bandCentre(
                    progress,
                    x = 16 * density,
                    y = 300 * density,
                    row = 300 * density,
                    rtl = false,
                )
            val far =
                bandCentre(
                    progress,
                    x = 220 * density,
                    y = 300 * density,
                    row = 300 * density,
                    rtl = false,
                )
            assertEquals("at $progress", near, far, Tolerance)
            assertEquals(
                "at $progress",
                expectedCentre(progress, row = 300 * density, rtl = false),
                near,
                Tolerance,
            )
        }
    }

    @Test
    fun `a bone further down sees the band further back, by the tilt`() {
        val progress = 0.5f
        val upper =
            bandCentre(
                progress,
                x = 16 * density,
                y = 300 * density,
                row = 300 * density,
                rtl = false,
            )
        val lower =
            bandCentre(
                progress,
                x = 16 * density,
                y = 500 * density,
                row = 500 * density,
                rtl = false,
            )
        val lean =
            200 * density * tan(Math.toRadians(Motion.skeletonTiltDegrees.toDouble())).toFloat()
        assertEquals(upper - lean, lower, Tolerance)
    }

    @Test
    fun `right to left, the band is mirrored and enters at the right edge`() {
        for (progress in listOf(0.1f, 0.5f, 0.9f)) {
            val ltr =
                bandCentre(
                    progress,
                    x = 40 * density,
                    y = 120 * density,
                    row = 140 * density,
                    rtl = false,
                )
            val rtl =
                bandCentre(
                    progress,
                    x = 40 * density,
                    y = 120 * density,
                    row = 140 * density,
                    rtl = true,
                )
            assertEquals("at $progress", width - ltr, rtl, Tolerance)
        }
        val early = bandCentre(0.1f, x = 0f, y = 0f, row = 0f, rtl = true)
        assertTrue("right to left starts at the right: $early", early > width / 2)
    }

    @Test
    fun `the band starts wholly before the area and ends wholly past it`() {
        val farCorner = width * TiltCos + height * TiltSin
        assertEquals(-band, SweepGeometry.start(0f, width, height, band), Tolerance)
        assertTrue(SweepGeometry.start(0f, width, height, band) + band <= 0f)
        assertTrue(SweepGeometry.start(1f, width, height, band) >= farCorner - Tolerance)
    }

    /**
     * Where the band's middle falls on the area's row [row], as a bone at ([x], [y]) draws it: the
     * gradient, laid out from the bone's origin and moved by its shift, peaks where its phase is
     * half a band.
     */
    private fun bandCentre(progress: Float, x: Float, y: Float, row: Float, rtl: Boolean): Float {
        val shift = SweepGeometry.shift(progress, x, y, width, height, band, rtl)
        val localRow = row - y
        val reach = (band / 2 - localRow * TiltSin) / TiltCos
        val local = if (rtl) shift - reach else shift + reach
        return x + local
    }

    private fun expectedCentre(progress: Float, row: Float, rtl: Boolean): Float {
        val centre =
            (SweepGeometry.start(progress, width, height, band) + band / 2 - row * TiltSin) /
                TiltCos
        return if (rtl) width - centre else centre
    }

    private companion object {
        const val Tolerance = 0.5f
    }
}
