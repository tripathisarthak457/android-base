package com.example.finalafter.crui.core.navigation.shell.stack

import android.graphics.Rect
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.finalafter.crui.core.navigation.shell.ROBOLECTRIC_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The page beneath the back card is drawn around the card, never under it, and never left out of a
 * frame: a frame that does not draw it takes it out of the renderer's tree, which frees every
 * display list in it, so the whole page would be recorded again on the frame that shows it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CardStripsTest {

    @Test
    fun `a card that hides the whole page still draws it, clipped to nothing`() {
        val draws = drawAround(MutableRect(0f, 0f, WIDTH, HEIGHT))
        assertEquals(1, draws.size)
        assertTrue("drawn with the clip ${draws.single()}", draws.single().isEmpty)
    }

    @Test
    fun `a hole past the page's edges hides all of it too, and the page is still drawn`() {
        val draws = drawAround(MutableRect(-40f, -10f, WIDTH + 40f, HEIGHT + 10f))
        assertEquals(1, draws.size)
        assertTrue(draws.single().isEmpty)
    }

    @Test
    fun `a shrunk card draws the page in the four strips around it`() {
        val draws = drawAround(MutableRect(10f, 20f, 90f, 180f))
        assertEquals(
            listOf(Rect(0, 0, 100, 20), Rect(0, 180, 100, 200), Rect(0, 20, 10, 180), Rect(90, 20, 100, 180)),
            draws,
        )
    }

    /** The clip of each time the page is drawn around [hole], on a page [WIDTH] by [HEIGHT] px. */
    private fun drawAround(hole: MutableRect): List<Rect> {
        val clips = mutableListOf<Rect>()
        val canvas = Canvas(ImageBitmap(WIDTH.toInt(), HEIGHT.toInt()))
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(WIDTH, HEIGHT)) {
            PageScope(this) { clips += Rect().also { drawContext.canvas.nativeCanvas.getClipBounds(it) } }
                .drawAroundHole(hole, scrim = 0.2f)
        }
        return clips
    }

    /** A draw scope whose content is [page]. */
    private class PageScope(
        scope: DrawScope,
        private val page: DrawScope.() -> Unit,
    ) : ContentDrawScope, DrawScope by scope {
        override fun drawContent() = page()
    }
}

private const val WIDTH = 100f
private const val HEIGHT = 200f
