package com.example.hud.standard.core.perfhud.overlay

import android.graphics.RenderNode
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The pill must never draw a frame of its own: a redraw on a timer cost 1.3 points of jank on a
 * phone, because a lone frame after idle misses its deadline. So the node may redraw only when a
 * pre-draw pass, which runs inside a frame the app draws anyway, finds its period over, and must
 * stop listening once it leaves the composition.
 *
 * Robolectric lays a window out but never draws it, so each check draws the window itself into a
 * display list, as a frame would: the node's layer is recorded again only if the node invalidated
 * it since the last frame, and [draws] counts the recordings.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RedrawDuringFramesTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var view: View
    private var shown by mutableStateOf(true)
    private var draws = 0

    @Before
    fun setUp() {
        compose.setContent {
            view = LocalView.current
            if (shown) {
                Spacer(
                    Modifier
                        .size(10.dp)
                        // In a layer, as the pill is in the badge's, so a frame can reuse what it recorded.
                        .graphicsLayer()
                        .redrawDuringFrames(PERIOD_MILLIS)
                        .drawBehind { draws++ },
                )
            }
        }
        drawFrame()
        assertTrue("The content is drawn at all.", draws > 0)
        // The first pre-draw pass always finds the period over; every test starts after it.
        view.viewTreeObserver.dispatchOnPreDraw()
        drawFrame()
    }

    @Test
    fun `a frame without the period over reuses the layer`() {
        val before = draws

        view.viewTreeObserver.dispatchOnPreDraw()
        drawFrame()

        assertEquals(before, draws)
    }

    @Test
    fun `time passing without a pre-draw pass redraws nothing`() {
        val before = draws

        Thread.sleep(PERIOD_MILLIS + MARGIN_MILLIS)
        compose.mainClock.advanceTimeBy(PERIOD_MILLIS * 4)
        drawFrame()

        assertEquals(before, draws)
    }

    @Test
    fun `pre-draw passes redraw at most once a period`() {
        Thread.sleep(PERIOD_MILLIS + MARGIN_MILLIS)
        val before = draws

        view.viewTreeObserver.dispatchOnPreDraw()
        drawFrame()
        view.viewTreeObserver.dispatchOnPreDraw()
        drawFrame()

        assertEquals(before + 1, draws)

        Thread.sleep(PERIOD_MILLIS + MARGIN_MILLIS)
        view.viewTreeObserver.dispatchOnPreDraw()
        drawFrame()

        assertEquals(before + 2, draws)
    }

    @Test
    fun `the node stops listening when it leaves the composition`() {
        val listening = preDrawListeners()

        shown = false
        compose.waitForIdle()

        assertEquals(listening - 1, preDrawListeners())
    }

    private fun drawFrame() {
        compose.waitForIdle()
        compose.runOnIdle {
            val frame = RenderNode("frame").apply { setPosition(0, 0, view.width, view.height) }
            val canvas = frame.beginRecording()
            try {
                view.draw(canvas)
            } finally {
                frame.endRecording()
                frame.discardDisplayList()
            }
        }
    }

    /**
     * The platform offers no way to ask a [ViewTreeObserver] what listens to it, so its list is
     * read directly; Robolectric runs one fixed SDK, where the field cannot move.
     */
    private fun preDrawListeners(): Int {
        val field = ViewTreeObserver::class.java.getDeclaredField("mOnPreDrawListeners").apply { isAccessible = true }
        val listeners = field.get(view.viewTreeObserver) ?: return 0
        return listeners.javaClass.getDeclaredMethod("size").apply { isAccessible = true }.invoke(listeners) as Int
    }

    private companion object {
        const val PERIOD_MILLIS = 250L
        const val MARGIN_MILLIS = 20L
    }
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain, which is what this project builds with.
private const val ROBOLECTRIC_SDK = 35
