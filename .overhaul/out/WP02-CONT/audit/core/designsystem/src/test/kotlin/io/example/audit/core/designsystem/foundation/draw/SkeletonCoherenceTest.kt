package io.example.audit.core.designsystem.foundation.draw

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import io.example.audit.core.designsystem.TestSdk
import io.example.audit.core.designsystem.component.feedback.AppSkeleton
import io.example.audit.core.designsystem.component.feedback.AppSkeletonCircle
import io.example.audit.core.designsystem.setThemed
import io.example.audit.core.designsystem.theme.AppTheme
import io.example.audit.core.designsystem.theme.tokens.DesignTokens.Motion
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The band, as drawn, is one band: every pixel of every bone has the colour the sweep gives its
 * phase in the window, whatever the bone's size or place. A wide block is the reference for what
 * each phase looks like; a circle and a line elsewhere on screen must match it pixel for pixel.
 * Bones that each swept a band of their own, sized by their width, would not. Inside a
 * `shimmerBounds` area the band is the area's, laid out from its corner and timed to its size.
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

    @Test
    fun `bones in a shimmer area share the area's band, and a bone outside keeps the window's`() {
        var clock: ShimmerClock? = null
        var bone = Color.Unspecified
        var highlight = Color.Unspecified
        rule.mainClock.autoAdvance = false
        rule.setThemed {
            clock = LocalShimmerClock.current
            bone = AppTheme.colors.skeleton
            highlight = AppTheme.colors.skeletonHighlight
            Box(Modifier.fillMaxSize()) {
                AppSkeleton(Modifier.offset(0.dp, ReferenceTop).size(411.dp, ReferenceHeight), shape = RectangleShape)
                Box(Modifier.offset(AreaLeft, AreaTop).size(AreaWidth, AreaHeight).shimmerBounds()) {
                    AppSkeleton(Modifier.fillMaxWidth().height(AreaBlockHeight), shape = RectangleShape)
                    AppSkeleton(
                        Modifier.offset(AreaLineLeft, AreaLineTop).size(LineWidth, LineHeight),
                        shape = RectangleShape,
                    )
                }
            }
        }
        val sweep = checkNotNull(clock)
        val root = rule.onRoot().fetchSemanticsNode().size
        val band = with(rule.density) { Motion.skeletonBandWidth.toPx() }
        val area = with(rule.density) {
            Region(
                left = AreaLeft.roundToPx().toFloat(),
                top = AreaTop.roundToPx().toFloat(),
                right = (AreaLeft.roundToPx() + AreaWidth.roundToPx()).toFloat(),
                bottom = (AreaTop.roundToPx() + AreaHeight.roundToPx()).toFloat(),
            )
        }
        val window = Band(Region(0f, 0f, root.width.toFloat(), root.height.toFloat()), band)
        val inArea = Band(area, band)
        val outside = with(rule.density) {
            Region(0f, ReferenceTop.toPx(), root.width.toFloat(), (ReferenceTop + ReferenceHeight).toPx())
        }
        val block = with(rule.density) { Region(area.left, area.top, area.right, area.top + AreaBlockHeight.toPx()) }
        val line = with(rule.density) {
            Region(
                left = area.left + AreaLineLeft.toPx(),
                top = area.top + AreaLineTop.toPx(),
                right = area.left + (AreaLineLeft + LineWidth).toPx(),
                bottom = area.top + (AreaLineTop + LineHeight).toPx(),
            )
        }

        var captures = 0
        var bandInArea = false
        var bandOutside = false
        var frames = 0
        while (captures < 6 && frames < MaxFrames) {
            rule.mainClock.advanceTimeByFrame()
            frames++
            val progress = sweep.progress
            if (progress.isNaN() || progress !in 0.15f..0.85f || frames % 3 != 0) continue
            captures++
            val pixels = rule.onRoot().captureToImage().toPixelMap()
            fun assertBand(region: Region, sweptBy: Band, what: String): Boolean {
                var lit = false
                region.forEachInside(pixels, inset = 2) { x, y, colour ->
                    val expected = sweptBy.colour(x, y, progress, bone, highlight)
                    assertTrue(
                        "$what ($x, $y) at progress $progress: ${colour.levels()}, the band ${expected.levels()}",
                        !colour.differsFrom(expected, tolerance = Tolerance),
                    )
                    if (expected.differsFrom(bone)) lit = true
                }
                return lit
            }
            bandOutside = assertBand(outside, window, "outside the area") || bandOutside
            bandInArea = assertBand(block, inArea, "block in the area") || bandInArea
            bandInArea = assertBand(line, inArea, "line in the area") || bandInArea
        }

        assertTrue("no frame of the sweep was captured", captures >= 4)
        assertTrue("the band never crossed the area", bandInArea)
        assertTrue("the band never crossed the bone outside it", bandOutside)
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

    /**
     * The colour the sweep gives a pixel of [area] at a given progress, worked out from the area's own
     * origin and size: the band runs from bone to highlight and back across [band] pixels of phase.
     */
    private class Band(private val area: Region, private val band: Float) {
        fun colour(x: Int, y: Int, progress: Float, bone: Color, highlight: Color): Color {
            val phase = (x + 0.5f - area.left) * TiltCos + (y + 0.5f - area.top) * TiltSin
            val start = SweepGeometry.start(progress, area.right - area.left, area.bottom - area.top, band)
            val t = (phase - start) / band
            val lift = if (t <= 0f || t >= 1f) 0f else 1f - abs(2f * t - 1f)
            // A gradient blends the encoded sRGB values, so the expected colour is blended the same way.
            return Color(
                red = bone.red + (highlight.red - bone.red) * lift,
                green = bone.green + (highlight.green - bone.green) * lift,
                blue = bone.blue + (highlight.blue - bone.blue) * lift,
            )
        }
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

        // An area smaller than the window and away from its corner, so its band is somewhere else.
        val AreaLeft = 60.dp
        val AreaTop = 420.dp
        val AreaWidth = 200.dp
        val AreaHeight = 100.dp
        val AreaBlockHeight = 56.dp
        val AreaLineLeft = 20.dp
        val AreaLineTop = 72.dp

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
