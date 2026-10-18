package com.example.wp04.everything.core.navigation.stack

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import com.example.wp04.everything.core.designsystem.theme.AppTheme
import com.example.wp04.everything.core.navigation.AppBackStack
import com.example.wp04.everything.core.navigation.FeedTab
import com.example.wp04.everything.core.navigation.NavCommand
import com.example.wp04.everything.core.navigation.Post
import com.example.wp04.everything.core.navigation.ROBOLECTRIC_SDK
import com.example.wp04.everything.core.navigation.testRegistry
import com.example.wp04.everything.core.navigation.transition.ScreenMotion
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
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SlideRecordingTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val input = DirectNavigationEventInput()
    private val stack = AppBackStack(mutableStateListOf(FeedTab, Post(1)))
    private val draws = HashMap<String, Int>()

    private val registry = testRegistry { key ->
        Box(Modifier.fillMaxSize().drawBehind { draws.merge(key.toString(), 1, Int::plus) })
    }

    private fun show() {
        rule.setContent {
            AppTheme { StackHost(stack = stack, registry = registry, motion = ScreenMotion()) }
        }
        rule.runOnIdle { rule.activity.navigationEventDispatcher.addInput(input) }
        rule.waitForIdle()
    }

    /** Runs [action] with the clock stopped, steps each frame it causes, and returns how many there were. */
    private fun frames(action: () -> Unit): Int {
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { draws.clear() }
        rule.runOnUiThread(action)
        var frames = 0
        do {
            rule.mainClock.advanceTimeByFrame()
            frames++
        } while (rule.hasPendingWork() && frames < MAX_FRAMES)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        return frames
    }

    @Test
    fun `a pop records neither screen on the frames it slides them`() {
        show()

        val frames = frames { stack.apply(NavCommand.Up) }

        assertTrue("the pop slides over several frames, was $frames", frames > 10)
        assertRecordedAtMost(2, FeedTab.toString())
        assertRecordedAtMost(2, Post(1).toString())
    }

    @Test
    fun `the back card records neither screen while it follows the finger`() {
        show()
        rule.runOnIdle { input.backStarted(event(0f)) }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { draws.clear() }

        // One move per frame, as a finger gives them.
        for (step in 1..30) {
            rule.runOnUiThread { input.backProgressed(event(step / 40f)) }
            rule.mainClock.advanceTimeByFrame()
        }
        val following = rule.runOnUiThread { HashMap(draws) }
        rule.mainClock.autoAdvance = true
        rule.runOnIdle { input.backCancelled() }

        assertRecordedAtMost(2, FeedTab.toString(), following)
        assertRecordedAtMost(2, Post(1).toString(), following)
    }

    private fun assertRecordedAtMost(times: Int, screen: String, recordings: Map<String, Int> = draws) {
        val recorded = recordings[screen] ?: 0
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
