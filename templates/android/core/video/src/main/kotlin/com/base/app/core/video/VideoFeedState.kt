package com.base.app.core.video

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.base.app.core.designsystem.foundation.layout.LocalTabVisible
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.video.engine.PreloadingEngine
import com.base.app.core.video.engine.VideoEngine
import com.base.app.core.video.policy.PlaybackWindow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The players, preloading and playback of one feed. A few players are shared by the whole feed and
 * handed from page to page as it settles, never one per page; only the settled clip plays, muted
 * until the person asks, and only while its screen shows. Everything is released when the screen
 * stops, and rebuilt where each clip was when it starts again.
 *
 * Every method runs on the main thread.
 */
@Stable
class VideoFeedState internal constructor(
    clips: List<VideoClip>,
    private val poolSize: Int,
    private val events: () -> VideoEvents?,
    private val engineFactory: () -> VideoEngine,
) {
    internal var clips: List<VideoClip> by mutableStateOf(clips)

    /** The settled page, or -1 before the feed has started. */
    internal var current by mutableIntStateOf(-1)
        private set

    /** The feed's screen shows: not behind a pushed screen, a sheet or a cover. */
    internal var shown by mutableStateOf(false)
        private set

    internal var reduceMotion by mutableStateOf(false)

    /** True until the person turns the sound on. While muted no audio track is decoded and no focus is taken. */
    var muted by mutableStateOf(true)
        private set

    /** The person's own play or pause of the settled clip; null until they make one. */
    private var choice by mutableStateOf<Boolean?>(null)

    private val players = mutableStateMapOf<Int, Player>()
    private val failures = mutableStateMapOf<Int, Boolean>()
    private val pageOf = HashMap<Player, Int>()
    private val listeners = HashMap<Player, Listener>()
    private val positions = HashMap<Int, Long>()
    private var audible: Player? = null
    private var engine: VideoEngine? = null
    private var forward = true

    internal fun playerAt(page: Int): Player? = players[page]

    internal fun failed(page: Int): Boolean = failures[page] == true

    internal fun plays(page: Int): Boolean = PlaybackWindow.plays(page, current, shown, reduceMotion, choice)

    /** The settled clip is still, waiting for a tap: under reduce motion, or after the person paused it. */
    internal fun waitsForTap(page: Int): Boolean = page == current && shown && !plays(page) && !failed(page)

    fun toggleSound() {
        muted = !muted
        apply()
    }

    /** Plays or pauses the clip on [page] when it is the settled one; a tap on any other page does nothing. */
    fun togglePlayback(page: Int) {
        if (page != current) return
        choice = !plays(page)
        apply()
    }

    internal fun retry(page: Int) {
        failures.remove(page)
        val player = players[page] ?: return
        listeners[player]?.prepared()
        player.prepare()
    }

    internal fun start() {
        if (engine == null) engine = engineFactory()
    }

    internal fun show(shown: Boolean) {
        this.shown = shown
        apply()
    }

    /**
     * The pager settled on [page]: the window moves, pages that leave it give their player back, and
     * pages that enter take one, one per frame, the settled page first. Building a player costs
     * milliseconds on the main thread, so no two land in the same frame.
     */
    internal suspend fun settle(page: Int) {
        val engine = engine ?: return
        if (page !in clips.indices) return
        if (current >= 0 && page != current) forward = page > current
        current = page
        choice = null
        events()?.onSettled(page, SystemClock.elapsedRealtime())

        val holders = PlaybackWindow.holders(page, clips.size, poolSize, forward)
        events()?.onWindow(holders.toIntArray())
        for (leaving in players.keys.filter { it !in holders }) {
            val player = players.remove(leaving) ?: continue
            positions[leaving] = player.currentPosition
            pageOf.remove(player)
            if (audible === player) {
                sound(player, on = false)
                audible = null
            }
            engine.recycle(player)
        }
        engine.settle(page)
        engine.preload(clips, preloadWindow(page))
        apply()

        for (holder in holders) {
            if (holder in players) continue
            withFrameNanos { }
            val player = engine.obtain()
            listeners.getOrPut(player) { Listener(player).also(player::addListener) }.prepared()
            // Every player starts muted; apply() gives sound to the settled one when the person asked for it.
            sound(player, on = false)
            pageOf[player] = holder
            failures.remove(holder)
            events()?.onAcquire(clips[holder].id, holder)
            engine.prepare(player, holder, clips[holder], positions[holder] ?: 0L)
            players[holder] = player
            apply()
        }
    }

    /** The screen stopped: each clip's place is kept, then the preloading and every player are released. */
    internal fun stop() {
        val engine = engine ?: return
        for ((page, player) in players) positions[page] = player.currentPosition
        players.clear()
        pageOf.clear()
        listeners.clear()
        audible = null
        this.engine = null
        engine.release()
    }

    private fun preloadWindow(page: Int) = (page - VideoDefaults.LOOK_BEHIND..page + VideoDefaults.LOOK_AHEAD)
        .filter { it in clips.indices }
        .mapNotNull { index -> PlaybackWindow.preload(index - page)?.let { index to it } }
        .toMap()

    /** Plays the settled clip and pauses the rest; gives sound and audio focus to the settled clip only. */
    private fun apply() {
        val loud = players[current]?.takeIf { !muted && plays(current) }
        for ((page, player) in players) {
            player.playWhenReady = plays(page)
        }
        if (loud === audible) return
        audible?.let { sound(it, on = false) }
        loud?.let { sound(it, on = true) }
        audible = loud
    }

    private fun sound(player: Player, on: Boolean) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, !on)
            .build()
        player.setAudioAttributes(AudioAttributes.DEFAULT, on)
    }

    /** Reports a player's first frame once per clip it prepares; plain fields, so a loop writes no state. */
    private inner class Listener(private val player: Player) : Player.Listener {
        private var wasReady = false
        private var reported = false

        fun prepared() {
            reported = false
            wasReady = false
        }

        override fun onRenderedFirstFrame() {
            if (reported) return
            val page = pageOf[player] ?: return
            reported = true
            events()?.onFirstFrame(clips[page].id, page, SystemClock.elapsedRealtime())
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val page = pageOf[player] ?: return
            if (playbackState == Player.STATE_BUFFERING && wasReady) events()?.onRebuffer(clips[page].id)
            wasReady = playbackState == Player.STATE_READY
        }

        override fun onPlayerError(error: PlaybackException) {
            val page = pageOf[player] ?: return
            failures[page] = true
        }
    }
}

/**
 * A [VideoFeedState] for [clips], driven by the settled page [current] reads, such as
 * `{ pagerState.settledPage }`. Players are built only after the screen has drawn its first frames,
 * released when it stops (Home, the screen off, a hidden tab) and rebuilt when it starts again.
 */
@Composable
fun rememberVideoFeedState(clips: List<VideoClip>, current: () -> Int): VideoFeedState {
    val context = LocalContext.current.applicationContext
    return rememberVideoFeedState(clips, current) {
        PreloadingEngine(context, AppVideoSetup.requireCallFactory(), AppVideoSetup.events)
    }
}

@Composable
internal fun rememberVideoFeedState(clips: List<VideoClip>, current: () -> Int, engineFactory: () -> VideoEngine): VideoFeedState {
    val state = remember {
        VideoFeedState(
            clips = clips,
            poolSize = VideoDefaults.POOL_SIZE,
            events = { AppVideoSetup.events },
            engineFactory = engineFactory,
        )
    }
    val reduceMotion = AppTheme.reduceMotion
    SideEffect {
        state.clips = clips
        state.reduceMotion = reduceMotion
    }
    val visibility = LocalTabVisible.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val settled by rememberUpdatedState(current)
    LaunchedEffect(state, lifecycle, visibility) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Never in the frame that composes the screen, nor the one after: the push lands first.
            withFrameNanos { }
            withFrameNanos { }
            state.start()
            try {
                coroutineScope {
                    launch { snapshotFlow { visibility.isVisible }.collect(state::show) }
                    snapshotFlow { settled() }.collectLatest { page -> state.settle(page) }
                }
            } finally {
                state.stop()
            }
        }
    }
    return state
}
