@file:OptIn(UnstableApi::class)

package com.example.videoalone.core.video.engine

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager.PreloadStatus
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import com.example.videoalone.core.video.VideoClip
import com.example.videoalone.core.video.VideoDefaults
import com.example.videoalone.core.video.VideoEvents
import com.example.videoalone.core.video.policy.PlaybackWindow
import com.example.videoalone.core.video.policy.PreloadTarget
import okhttp3.Call

/**
 * The feed's players and preloading on Media3. One [DefaultPreloadManager] ranks the clips by their
 * distance from the settled one and keeps each as ready as the window asks; the players come from its
 * builder, so they share its load control, bandwidth meter, renderers and playback thread, and take
 * the sources it has already prepared.
 *
 * Every method runs on the main thread, as Media3 asks of a preload manager and its players.
 */
internal class PreloadingEngine(
    context: Context,
    callFactory: () -> Call.Factory,
    private val events: VideoEvents?,
) : VideoEngine {

    private var settled = 0

    private val control = TargetPreloadStatusControl<Int, PreloadStatus> { page ->
        when (val target = PlaybackWindow.preload(page - settled)) {
            is PreloadTarget.Loaded -> PreloadStatus.specifiedRangeLoaded(target.millis)
            is PreloadTarget.Cached -> PreloadStatus.specifiedRangeCached(target.millis)
            null -> PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
        }
    }

    private val builder = DefaultPreloadManager.Builder(context.applicationContext, control)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    VideoDefaults.MIN_BUFFER_MS,
                    VideoDefaults.MAX_BUFFER_MS,
                    VideoDefaults.START_BUFFER_MS,
                    VideoDefaults.REBUFFER_MS,
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build(),
        )
        // The manager selects tracks for the sources it prepares, and every player built from it uses the
        // same factory: muted from the start, a hand-over from preload to player selects nothing again.
        .setTrackSelectorFactory(MutedTrackSelector)
        // The manager reads this cache for playback and writes it when it pre-caches far clips; the bytes
        // a player streams reach it through the writer inside the data source below.
        .setCache(VideoCache.get(context))
        .setDataSourceFactory(videoUpstream(VideoCache.get(context), callFactory))

    private val manager = builder.build()
    private val players = mutableListOf<ExoPlayer>()
    private val idle = ArrayDeque<ExoPlayer>()

    /** The media item each page was added to the manager with: the manager finds sources by item. */
    private val items = HashMap<Int, MediaItem>()

    override fun obtain(): Player = idle.removeFirstOrNull() ?: newPlayer().also(players::add)

    override fun prepare(player: Player, page: Int, clip: VideoClip, positionMs: Long) {
        val exo = player as ExoPlayer
        val item = items.getOrPut(page) { mediaItem(page, clip).also { manager.add(it, page) } }
        val source = manager.getMediaSource(item)
        if (source != null) exo.setMediaSource(source, positionMs) else exo.setMediaItem(item, positionMs)
        exo.prepare()
    }

    override fun recycle(player: Player) {
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        idle.addLast(player as ExoPlayer)
    }

    override fun settle(page: Int) {
        settled = page
        manager.setCurrentPlayingIndex(page)
    }

    override fun preload(clips: List<VideoClip>, targets: Map<Int, PreloadTarget>) {
        items.keys.filter { it !in targets }.forEach { page -> items.remove(page)?.let(manager::remove) }
        for (page in targets.keys) {
            if (page in items || page !in clips.indices) continue
            items[page] = mediaItem(page, clips[page]).also { manager.add(it, page) }
        }
        manager.invalidate()
    }

    override fun release() {
        // The manager first, then the players: the order Media3 asks for.
        manager.release()
        players.forEach(ExoPlayer::release)
        players.clear()
        idle.clear()
        items.clear()
    }

    private fun newPlayer(): ExoPlayer = builder.buildExoPlayer().apply {
        // Keeps the decoder between clips, so a swipe back reuses a warm one. Safe here because
        // every player is released when the screen stops.
        setForegroundMode(true)
        repeatMode = Player.REPEAT_MODE_ONE
        // Muted until the person asks: no audio track is decoded, and their music keeps playing.
        trackSelectionParameters = muted(trackSelectionParameters)
        setAudioAttributes(AudioAttributes.DEFAULT, false)
        if (events != null) addAnalyticsListener(EventsListener(events))
    }

    private fun mediaItem(page: Int, clip: VideoClip): MediaItem = MediaItem.Builder()
        .setUri(clip.url)
        // A clip repeated further down the feed is another item, but the same bytes on disk.
        .setMediaId("${clip.id}$PAGE_SEPARATOR$page")
        .setCustomCacheKey(clip.id)
        .build()

    /** Hands decoder start-up and each clip's frame counts to [events], by the clip each player is on. */
    private class EventsListener(private val events: VideoEvents) : AnalyticsListener {
        private val window = Timeline.Window()

        override fun onVideoDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long,
        ) = events.onDecoderReady(clipId(eventTime), decoderName, initializationDurationMs)

        // The counters cover everything the renderer drew since it was enabled for the clip.
        override fun onVideoDisabled(eventTime: AnalyticsListener.EventTime, decoderCounters: DecoderCounters) {
            decoderCounters.ensureUpdated()
            events.onFrameCounts(clipId(eventTime), decoderCounters.renderedOutputBufferCount, decoderCounters.droppedBufferCount)
        }

        private fun clipId(eventTime: AnalyticsListener.EventTime): String {
            if (eventTime.timeline.isEmpty) return ""
            val item = eventTime.timeline.getWindow(eventTime.windowIndex, window).mediaItem
            return item.mediaId.substringBefore(PAGE_SEPARATOR)
        }
    }

    private companion object {
        const val PAGE_SEPARATOR = "@"
    }
}

/** Track selection with the audio track off: the manager's own, and each player's from the start. */
internal object MutedTrackSelector : TrackSelector.Factory {
    override fun createTrackSelector(context: Context): TrackSelector =
        DefaultTrackSelector(context, muted(TrackSelectionParameters.getDefaults(context)))
}

internal fun muted(parameters: TrackSelectionParameters): TrackSelectionParameters =
    parameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true).build()

/**
 * Where a player's bytes come from: the disk cache, and otherwise the app's own client, writing what
 * it streams into the cache as it goes. The preload manager wraps this in a reader of its own that
 * never writes, so without the writer here played bytes would never reach the disk and every swipe
 * back, and every second visit, would fetch again. When the manager pre-caches a far clip it writes
 * through a writer of its own over this chain; this one then finds the span locked and passes the
 * bytes through without writing them twice.
 */
internal fun videoUpstream(cache: Cache, callFactory: () -> Call.Factory): DataSource.Factory =
    CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(videoNetwork(callFactory))

/**
 * The app's own client, built on the first request, which Media3 makes on a loading thread. Media3
 * asks for identity encoding itself, so byte ranges stay exact and the client's own compression stays
 * out of video.
 */
internal fun videoNetwork(callFactory: () -> Call.Factory): DataSource.Factory =
    OkHttpDataSource.Factory(Call.Factory { request -> callFactory().newCall(request) })
