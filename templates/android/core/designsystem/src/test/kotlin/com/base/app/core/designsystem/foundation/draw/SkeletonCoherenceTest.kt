package com.base.app.core.designsystem.foundation.draw

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.TestSdk
import com.base.app.core.designsystem.component.feedback.AppSkeleton
import com.base.app.core.designsystem.component.feedback.AppSkeletonCircle
import com.base.app.core.designsystem.setThemed
import com.base.app.core.designsystem.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The band, as drawn, is one band: every pixel of every bone has the colour the sweep gives its
 * phase in the window, whatever the bone's size or place. A wide block is the reference for what
 * each phase looks like; a circle and a line elsewhere on screen must match it pixel for pixel.
 * Bones that each swept a band of their own, sized by their width, would not.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkeletonCoherenceTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `every bone on screen shows the one band at its own place`() {
        var clock: ShimmerClock? = null
        var bone = Color.Unspecified
        rule.mainClock.autoAdvance = false
        rule.setThemed {
            clock = LocalShimmerClock.current
            bone = AppTheme.colors.skeleton
            Box(Modifier.fillMaxSize()) {
                AppSkeleton(Modifier.offset(0.dp, ReferenceTop).size(411.dp, ReferenceHeight), shape = RectangleShape)
                AppSkeleton(Modifier.offset(LineLeft, LineTop).size(LineWidth, LineHeight), shape = RectangleShape)
                AppSkeletonCircle(Modifier.offset(CircleLeft, CircleTop), size = CircleSize)
            }
        }
        val sweep = checkNotNull(clock)
        val density = rule.density.density
        fun px(dp: Float) = dp * density

        var captures = 0
        var bandOnLine = false
        var bandOnCircle = false
        var frames = 0
        while (captures < 6 && frames < MaxFrames) {
            rule.mainClock.advanceTimeByFrame()
            frames++
            val progress = sweep.progress
            if (progress.isNaN() || progress !in 0.25f..0.75f || frames % 3 != 0) continue
            captures++
            val pixels = rule.onRoot().captureToImage().toPixelMap()
            val phases = PhaseColours()
            val reference = Region(0f, px(ReferenceTop.value), px(411f), px(ReferenceTop.value + ReferenceHeight.value))
            reference.forEachInside(pixels, inset = 2) { x, y, colour -> phases.add(x, y, colour) }

            val line = Region(
                left = px(LineLeft.value),
                top = px(LineTop.value),
                right = px(LineLeft.value + LineWidth.value),
                bottom = px(LineTop.value + LineHeight.value),
            )
            line.forEachInside(pixels, inset = 3) { x, y, colour ->
                assertMatches(phases, x, y, colour, "line, at progress $progress")
                if (colour.differsFrom(bone)) bandOnLine = true
            }

            val radius = px(CircleSize.value) / 2
            val centreX = px(CircleLeft.value) + radius
            val centreY = px(CircleTop.value) + radius
            val circle = Region(centreX - radius, centreY - radius, centreX + radius, centreY + radius)
            circle.forEachInside(pixels, inset = 0) { x, y, colour ->
                val dx = x + 0.5f - centreX
                val dy = y + 0.5f - centreY
                if (dx * dx + dy * dy > (radius - 3) * (radius - 3)) return@forEachInside
                assertMatches(phases, x, y, colour, "circle, at progress $progress")
                if (colour.differsFrom(bone)) bandOnCircle = true
            }
        }

        assertTrue("no frame of the sweep was captured", captures >= 4)
        assertTrue("the band never crossed the line", bandOnLine)
        assertTrue("the band never crossed the circle", bandOnCircle)
    }

    private fun assertMatches(phases: PhaseColours, x: Int, y: Int, colour: Color, what: String) {
        val expected = phases.at(x, y) ?: return
        assertTrue(
            "$what ($x, $y): ${colour.levels()} where the band is ${expected.levels()} at this phase",
            !colour.differsFrom(expected, tolerance = Tolerance),
        )
    }

    /** The mean colour of each phase, in one-pixel steps, from the pixels of a reference bone. */
    private class PhaseColours {
        private val sums = HashMap<Int, FloatArray>()

        fun add(x: Int, y: Int, colour: Color) {
            val sum = sums.getOrPut(phase(x, y)) { FloatArray(4) }
            sum[0] += colour.red
            sum[1] += colour.green
            sum[2] += colour.blue
            sum[3] += 1f
        }

        fun at(x: Int, y: Int): Color? {
            val sum = sums[phase(x, y)] ?: return null
            return Color(sum[0] / sum[3], sum[1] / sum[3], sum[2] / sum[3])
        }

        private fun phase(x: Int, y: Int) = floor((x + 0.5f) * TiltCos + (y + 0.5f) * TiltSin).toInt()
    }

    private class Region(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        fun forEachInside(pixels: PixelMap, inset: Int, action: (Int, Int, Color) -> Unit) {
            for (y in top.roundToInt() + inset until minOf(bottom.roundToInt() - inset, pixels.height)) {
                for (x in left.roundToInt() + inset until minOf(right.roundToInt() - inset, pixels.width)) {
                    action(x, y, pixels[x, y])
                }
            }
        }
    }

    private companion object {
        val ReferenceTop = 100.dp
        val ReferenceHeight = 120.dp
        val LineLeft = 200.dp
        val LineTop = 300.dp
        val LineWidth = 160.dp
        val LineHeight = 16.dp
        val CircleLeft = 100.dp
        val CircleTop = 400.dp
        val CircleSize = 40.dp

        /** Levels of 255 per channel: rounding, and dithering where the platform applies it. */
        const val Tolerance = 3
        const val MaxFrames = 400
    }
}

private fun Color.levels(): String =
    "(${(red * 255).roundToInt()}, ${(green * 255).roundToInt()}, ${(blue * 255).roundToInt()})"

private fun Color.differsFrom(other: Color, tolerance: Int = 3): Boolean =
    abs(red - other.red) * 255 > tolerance + 0.5f ||
        abs(green - other.green) * 255 > tolerance + 0.5f ||
        abs(blue - other.blue) * 255 > tolerance + 0.5f
