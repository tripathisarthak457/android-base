package com.example.everything.core.designsystem.foundation.draw

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.everything.core.designsystem.TestSdk
import java.lang.management.ManagementFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A sweep frame allocates nothing on the heap: drawing a bone with the band across it costs what
 * drawing it plain does, for a shaped bone and for the lines of a paragraph. At 120 Hz a garbage
 * object per bone per frame is what turns a loading screen into collector pauses.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkeletonDrawAllocationTest {

    private val density = Density(2.75f)
    private val bone = Color(0xFFD7DADF)
    private val plain = SolidColor(bone)
    private val sweep = SweepBrush(bone, Color(0xFFE5E8ED), band = 240 * density.density, rtl = false)

    @Test
    fun `a shaped bone draws the band without allocating`() =
        assertNoMoreThanPlain(OutlineBones(RoundedCornerShape(10.dp)))

    @Test
    fun `the lines of a paragraph draw the band without allocating`() {
        val fonts = createFontFamilyResolver(RuntimeEnvironment.getApplication())
        val measurer = TextMeasurer(fonts, density, LayoutDirection.Ltr)
        val layout = measurer.measure(
            "A paragraph long enough to wrap onto a second line and a third at this width.",
            TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
            constraints = Constraints(maxWidth = 600),
        )
        val bones = TextBones().apply { update(layout) }
        assertTrue("the text did not wrap", bones.lines.size >= 8)
        assertNoMoreThanPlain(bones)
    }

    /**
     * Compares the bytes [Draws] sweep frames allocate with what as many plain frames allocate, in
     * several rounds, and takes the round where the sweep cost least. Until the JIT has compiled the
     * drawing code the thread's count includes work that a compiled frame does not do, and that
     * compilation can land in any round; an object made on every frame shows in every round.
     */
    private fun assertNoMoreThanPlain(bones: Bones) {
        val canvas = Canvas(ImageBitmap(Width, Height))
        val extra = LongArray(Rounds)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, canvas, Size(Width.toFloat(), Height.toFloat())) {
            // The first draws build the outline, the shader and the native objects behind them.
            repeat(Draws) { index ->
                drawPlain(bones)
                drawSweptFrame(bones, index)
            }
            for (round in 0 until Rounds) {
                val plainBytes = allocatedBytes { repeat(Draws) { drawPlain(bones) } }
                val sweptBytes = allocatedBytes { repeat(Draws) { index -> drawSweptFrame(bones, index) } }
                extra[round] = sweptBytes - plainBytes
            }
        }
        assertTrue(
            "$Draws sweep frames allocated more than plain ones in every round: ${extra.joinToString()} bytes",
            extra.min() <= Slack,
        )
    }

    // The same canvas moves as a sweep frame makes, so only the band's own work is compared.
    private fun DrawScope.drawPlain(bones: Bones) =
        translate(left = 1f) { with(bones) { draw(plain, -1f, LineRadius) } }

    private fun DrawScope.drawSweptFrame(bones: Bones, index: Int) = drawSwept(
        bones = bones,
        brush = sweep,
        progress = (index % Draws) / Draws.toFloat(),
        x = 48f,
        y = 900f,
        width = 1080f,
        height = 2400f,
        lineRadius = LineRadius,
    )

    private inline fun allocatedBytes(block: () -> Unit): Long {
        val threads = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        val before = threads.currentThreadAllocatedBytes
        block()
        return threads.currentThreadAllocatedBytes - before
    }

    private companion object {
        const val Width = 900
        const val Height = 200
        const val Draws = 1_000
        const val Rounds = 5
        const val LineRadius = 16f

        /**
         * Half the smallest object per frame. An allocation on every frame is at least 16 bytes, so
         * it shows as 16 KB over the run; what the JVM and the native canvas account to the thread
         * on their own comes to a few kilobytes at most.
         */
        const val Slack = Draws * 8L
    }
}
