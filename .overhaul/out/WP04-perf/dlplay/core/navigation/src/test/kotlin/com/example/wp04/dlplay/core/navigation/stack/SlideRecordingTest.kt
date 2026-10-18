package com.example.wp04.dlplay.core.navigation.stack

import android.graphics.RenderNode
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import com.example.wp04.dlplay.core.designsystem.theme.AppTheme
import com.example.wp04.dlplay.core.navigation.AppBackStack
import com.example.wp04.dlplay.core.navigation.FeedTab
import com.example.wp04.dlplay.core.navigation.NavCommand
import com.example.wp04.dlplay.core.navigation.Post
import com.example.wp04.dlplay.core.navigation.ROBOLECTRIC_SDK
import com.example.wp04.dlplay.core.navigation.testRegistry
import com.example.wp04.dlplay.core.navigation.transition.ScreenMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What the frames of a slide and of the back card record: the clip, scrim and shadow the scene
 * draws over each screen, never the screens' own content, which their layers keep from the frame
 * before. A screen recorded again on every frame of its slide is the whole page's drawing on the
 * main thread and its upload to the render thread, every 8 ms.
 *
 * Each frame draws the window into a display list, as the renderer does, so a layer whose content
 * has not changed is drawn from what it recorded before; the screens count their own recordings.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SlideRecordingTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val input = DirectNavigationEventInput()
    private val stack = AppBackStack(mutableStateListOf(FeedTab, Post(1)))
    private val recordings = HashMap<String, Int>()
    private val window = RenderNode("window")

    private val registry = testRegistry { key ->
        Box(Modifier.fillMaxSize().drawBehind { recordings.merge(key.toString(), 1, Int::plus) })
    }

    private fun show() {
        rule.setContent {
            AppTheme { StackHost(stack = stack, registry = registry, motion = ScreenMotion()) }
        }
        rule.runOnIdle { rule.activity.navigationEventDispatcher.addInput(input) }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        drawWindow()
        val first = recorded(Post(1).toString())
        drawWindow()
        // Without kept display lists every draw would record everything, and the counts below would mean nothing.
        assertTrue("the window's first draw records the screen on top", first >= 1)
        assertEquals("a draw with nothing changed records nothing again", first, recorded(Post(1).toString()))
        rule.runOnUiThread { recordings.clear() }
    }

    private fun drawWindow() = rule.runOnUiThread {
        val view = rule.activity.window.decorView
        window.setPosition(0, 0, view.width, view.height)
        val canvas = window.beginRecording()
        try {
            view.draw(canvas)
        } finally {
            window.endRecording()
        }
    }

    /** Steps one frame and draws the window, as the renderer would for it. */
    private fun frame() {
        rule.mainClock.advanceTimeByFrame()
        drawWindow()
    }

    private fun recorded(screen: String): Int = rule.runOnUiThread { recordings[screen] ?: 0 }

    @Test
    fun `a pop records neither screen on the frames it slides them`() {
        show()

        rule.runOnUiThread { stack.apply(NavCommand.Up) }
        var frames = 0
        do {
            frame()
            frames++
        } while (rule.hasPendingWork() && frames < MAX_FRAMES)

        assertTrue("the pop slides over several frames, was $frames", frames > 10)
        assertRecordedAtMost(2, FeedTab.toString())
        assertRecordedAtMost(2, Post(1).toString())
    }

    @Test
    fun `the back card records neither screen while it follows the finger`() {
        show()
        rule.runOnUiThread { input.backStarted(event(0f)) }
        frame()
        rule.runOnUiThread { recordings.clear() }

        // One move per frame, as a finger gives them.
        for (step in 1..30) {
            rule.runOnUiThread { input.backProgressed(event(step / 40f)) }
            frame()
        }

        assertRecordedAtMost(1, FeedTab.toString())
        assertRecordedAtMost(1, Post(1).toString())
    }

    private fun assertRecordedAtMost(times: Int, screen: String) {
        val recorded = recorded(screen)
        assertTrue("$screen was recorded $recorded times", recorded <= times)
    }

    private fun event(progress: Float) = NavigationEvent(
        swipeEdge = NavigationEvent.EDGE_LEFT,
        progress = progress,
        touchX = progress * 400f,
        touchY = 600f,
        frameTimeMillis = rule.mainClock.currentTime,
    )

    private companion object {
        const val MAX_FRAMES = 600
    }
}
