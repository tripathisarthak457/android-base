package com.base.app.core.navigation.shell.stack

import android.graphics.RenderNode
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.AppBackStack
import com.base.app.core.navigation.NavCommand
import com.base.app.core.navigation.screenNode
import com.base.app.core.navigation.shell.FeedTab
import com.base.app.core.navigation.shell.Post
import com.base.app.core.navigation.shell.ROBOLECTRIC_SDK
import com.base.app.core.navigation.shell.testRegistry
import com.base.app.core.navigation.shell.transition.ScreenMotion
import com.base.app.core.navigation.texturesAround
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
        val name = key.toString()
        Box(Modifier.fillMaxSize().testTag(name).drawBehind { recordings.merge(name, 1, Int::plus) })
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

    @Test
    fun `the screen under the card is recorded neither while it follows the finger nor while it fades`() {
        show()
        rule.runOnUiThread { input.backStarted(event(0f)) }
        frame()
        rule.runOnUiThread { recordings.clear() }

        for (step in 1..20) {
            rule.runOnUiThread { input.backProgressed(event(step / 40f)) }
            frame()
        }
        rule.runOnUiThread { input.backCompleted() }
        var frames = 0
        while (stack.keys.size > 1 && frames < MAX_FRAMES) {
            frame()
            frames++
        }

        assertEquals(listOf(FeedTab), stack.keys)
        assertRecordedAtMost(0, Post(1).toString())
    }

    /**
     * The other half of rendering the screen once per gesture: what is not recorded again must also
     * be what the texture holds. Only the screen's innermost layer may be the texture, and only
     * while the card is up; one layer further out it would also hold what the scene draws over the
     * card, and be rendered again whenever that changes.
     */
    @Test
    fun `the screen under the card is a texture in its innermost layer from the gesture's start until the pop`() {
        show()
        val top = rule.screenNode(Post(1))
        val beneath = rule.screenNode(FeedTab)
        assertTextures("before the gesture", top, none = true)

        rule.runOnUiThread { input.backStarted(event(0f)) }
        assertTextures("on the gesture's first frame", top)
        frame()
        for (step in 1..20) {
            rule.runOnUiThread { input.backProgressed(event(step / 40f)) }
            frame()
            assertTextures("while the card follows the finger", top)
            assertTextures("beneath the card", beneath, none = true)
        }
        rule.runOnUiThread { input.backCompleted() }
        var frames = 0
        while (stack.keys.size > 1 && frames < MAX_FRAMES) {
            assertTextures("through the commit's fade", top)
            frame()
            frames++
        }

        assertEquals(listOf(FeedTab), stack.keys)
        assertTextures("after the pop", beneath, none = true)
    }

    @Test
    fun `a cancel back at full size gives the screen its plain layer back at once`() {
        show()
        val top = rule.screenNode(Post(1))
        rule.runOnUiThread { input.backStarted(event(0f)) }
        frame()
        for (progress in listOf(0.1f, 0.2f, 0.3f, 0.2f, 0.1f, 0f)) {
            rule.runOnUiThread { input.backProgressed(event(progress)) }
            frame()
        }
        assertTextures("back at full size, the finger still down", top)

        rule.runOnUiThread { input.backCancelled() }
        var frames = 0
        while (rule.texturesAround(top).isNotEmpty() && frames < MAX_FRAMES) {
            frame()
            frames++
        }

        assertEquals(listOf(FeedTab, Post(1)), stack.keys)
        assertTextures("after the cancel", top, none = true)
        assertTrue("the texture was held $frames frames after the cancel", frames <= 1)
    }

    private fun assertTextures(moment: String, screen: LayoutInfo, none: Boolean = false) {
        val expected = if (none) emptyList() else listOf(0)
        val textures = rule.texturesAround(screen)
        assertEquals("$moment: the layers around the screen that are textures, 0 the innermost", expected, textures)
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
