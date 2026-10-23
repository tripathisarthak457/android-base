package com.base.app.core.video

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.media3.common.C
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.base.app.core.video.engine.MutedTrackSelector
import com.base.app.core.video.engine.PreloadingEngine
import com.base.app.core.designsystem.foundation.layout.LocalTabVisible
import com.base.app.core.designsystem.foundation.layout.TabVisibility
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.testing.compose.TEST_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A feed's players: never more than the pool, only the settled clip plays, released when the screen stops. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class VideoFeedStateTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val engine = FakeVideoEngine()
    private val clips = List(30) { VideoClip(id = "clip$it", url = "https://v.test/$it.mp4", posterUrl = null, width = 720, height = 1280) }
    private var page by mutableIntStateOf(0)
    private var visible by mutableStateOf(true)
    private var reduceMotion by mutableStateOf(false)
    private lateinit var state: VideoFeedState

    private fun show() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme(reduceMotion = reduceMotion) {
                CompositionLocalProvider(LocalTabVisible provides TabVisibility { visible }) {
                    state = rememberVideoFeedState(clips, { page }) { engine }
                }
            }
        }
        frames(10)
    }

    private fun frames(count: Int) = repeat(count) {
        rule.runOnUiThread { Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeByFrame()
    }

    private fun playerOn(page: Int): FakePlayer = state.playerAt(page) as FakePlayer

    private val heard = mutableListOf<String>()
    private val recorder = object : VideoEvents {
        override fun onWindow(pages: IntArray) {
            heard += "window ${pages.joinToString(",")}"
        }

        override fun onAcquire(clipId: String, page: Int) {
            heard += "acquire $page"
        }

        override fun onFirstFrame(clipId: String, page: Int, elapsedRealtimeMs: Long) {
            heard += "frame $page"
        }
    }

    @After
    fun forgetSetup() = AppVideoSetup.install(callFactory = { error("no network in tests") }, events = null)

    @Test
    fun `a feed scrolled to its end never holds more players than the pool`() {
        show()
        for (next in clips.indices) {
            page = next
            frames(6)
            assertTrue("${engine.holding.size} players at page $next", engine.holding.size <= VideoDefaults.POOL_SIZE)
        }
        assertTrue(engine.built.size <= VideoDefaults.POOL_SIZE)
    }

    @Test
    fun `only the settled clip plays`() {
        show()
        page = 5
        frames(6)

        assertTrue(playerOn(5).playing)
        assertFalse(playerOn(6).playing)
        assertFalse(playerOn(4).playing)
    }

    @Test
    fun `a screen covered by another pauses without releasing`() {
        show()
        visible = false
        frames(2)

        assertTrue(engine.built.none { it.playing })
        assertEquals(0, engine.releases)
    }

    @Test
    fun `stopping releases everything, and starting again resumes where each clip was`() {
        show()
        page = 3
        frames(6)
        val before = engine.built.toList()

        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        frames(2)
        assertEquals(1, engine.releases)
        assertTrue(before.all { it.released })
        assertTrue(state.playerAt(3) == null)

        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        frames(6)
        assertTrue(playerOn(3).playing)
    }

    @Test
    fun `muted clips have no audio track and take no focus, and sound goes to the settled clip only`() {
        show()
        page = 2
        frames(6)
        assertTrue(engine.built.none { it.audioOn || it.handlesFocus })

        rule.runOnUiThread { state.toggleSound() }
        frames(1)

        assertTrue(playerOn(2).audioOn && playerOn(2).handlesFocus)
        assertFalse(playerOn(3).audioOn)
        assertFalse(playerOn(1).audioOn)
    }

    @Test
    fun `under reduce motion nothing plays until tapped`() {
        reduceMotion = true
        show()
        assertTrue(engine.built.none { it.playing })

        rule.runOnUiThread { state.togglePlayback(0) }
        frames(1)

        assertTrue(playerOn(0).playing)
    }

    @Test
    fun `no player is built in the screen's first frames, and at most one per frame`() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme {
                state = rememberVideoFeedState(clips, { page }) { engine }
            }
        }
        val perFrame = List(8) {
            val before = engine.built.size
            frames(1)
            engine.built.size - before
        }

        assertEquals(listOf(0, 0), perFrame.take(2))
        assertTrue("$perFrame", perFrame.all { it <= 1 })
        assertEquals(VideoDefaults.POOL_SIZE, perFrame.sum())
    }

    @Test
    fun `a clip that fails shows its poster and a retry, and the feed goes on`() {
        show()
        rule.runOnUiThread { playerOn(0).fail() }
        frames(1)
        assertTrue(state.failed(0))

        rule.runOnUiThread { state.retry(0) }
        frames(1)
        assertFalse(state.failed(0))
    }

    @Test
    fun `the window and each hand-over are reported, the settled page first`() {
        AppVideoSetup.install(callFactory = { error("no network in tests") }, events = recorder)
        show()

        assertEquals(listOf("window 0,1,2", "acquire 0", "acquire 1", "acquire 2"), heard)
    }

    @Test
    fun `a clip that loops reports its first frame once and writes no state`() {
        AppVideoSetup.install(callFactory = { error("no network in tests") }, events = recorder)
        show()
        rule.runOnUiThread { playerOn(0).renderFirstFrame() }
        frames(1)
        assertEquals(1, heard.count { it == "frame 0" })

        var writes = 0
        val observer = Snapshot.registerApplyObserver { changed, _ -> writes += changed.size }
        try {
            // A loop of the same clip: its renderer draws a first frame again.
            rule.runOnUiThread { playerOn(0).renderFirstFrame() }
            frames(2)
        } finally {
            observer.dispose()
        }

        assertEquals(1, heard.count { it == "frame 0" })
        assertEquals(0, writes)
    }

    @Test
    fun `the preload manager and every player it builds select no audio track`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertTrue(C.TRACK_TYPE_AUDIO in MutedTrackSelector.createTrackSelector(context).parameters.disabledTrackTypes)

        rule.runOnUiThread {
            val engine = PreloadingEngine(context, callFactory = { error("no network in tests") }, events = null)
            try {
                assertTrue(C.TRACK_TYPE_AUDIO in engine.obtain().trackSelectionParameters.disabledTrackTypes)
            } finally {
                engine.release()
            }
        }
    }
}
