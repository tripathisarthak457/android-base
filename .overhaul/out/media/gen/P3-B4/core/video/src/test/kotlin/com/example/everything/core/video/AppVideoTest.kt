package com.example.everything.core.video

import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.example.everything.core.designsystem.theme.AppTheme
import com.example.everything.core.testing.compose.TEST_SDK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A clip draws on a SurfaceView, shows its poster until the first frame, and keeps the screen on while it plays. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class AppVideoTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val engine = FakeVideoEngine()
    private val clips = List(3) { VideoClip(id = "clip$it", url = "https://v.test/$it.mp4", posterUrl = null, width = 720, height = 1280) }
    private val page = mutableIntStateOf(0)
    private lateinit var state: VideoFeedState

    private fun show() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme {
                state = rememberVideoFeedState(clips, { page.intValue }) { engine }
                AppVideo(state, index = 0, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        repeat(10) {
            rule.runOnUiThread { Snapshot.sendApplyNotifications() }
            rule.mainClock.advanceTimeByFrame()
        }
    }

    private fun surfaces(view: View): List<SurfaceView> = when (view) {
        is SurfaceView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { surfaces(view.getChildAt(it)) }
        else -> emptyList()
    }

    @Test
    fun `the clip draws on a SurfaceView the player renders into`() {
        show()

        val surface = surfaces(rule.activity.window.decorView).single()
        assertNotNull((state.playerAt(0) as FakePlayer).output)
        assertTrue(surface.isAttachedToWindow)
    }

    @Test
    fun `the playing clip keeps the screen on`() {
        show()

        assertTrue(rule.activity.window.decorView.rootView.keepScreenOn || hasKeepScreenOn(rule.activity.window.decorView))
        rule.runOnUiThread { state.togglePlayback(0) }
        repeat(2) {
            rule.runOnUiThread { Snapshot.sendApplyNotifications() }
            rule.mainClock.advanceTimeByFrame()
        }
        assertFalse(hasKeepScreenOn(rule.activity.window.decorView))
    }

    private fun hasKeepScreenOn(view: View): Boolean =
        view.keepScreenOn || (view is ViewGroup && (0 until view.childCount).any { hasKeepScreenOn(view.getChildAt(it)) })

    @Test
    fun `a clip with a player shows its poster over the surface until it has a frame`() {
        show()

        // The poster is ContentFrame's shutter, which Media3 takes away on the player's first rendered frame.
        assertNotNull(state.playerAt(0))
        assertTrue(surfaces(rule.activity.window.decorView).isNotEmpty())
        rule.onAllNodesWithTag(PosterTag, useUnmergedTree = true).assertCountEquals(1)
    }
}
