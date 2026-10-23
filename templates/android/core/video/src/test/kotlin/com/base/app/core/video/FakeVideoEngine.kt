@file:OptIn(UnstableApi::class)

package com.base.app.core.video

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import com.base.app.core.video.engine.VideoEngine
import com.base.app.core.video.policy.PreloadTarget
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** A player that records what the feed asked of it and renders nothing. */
internal class FakePlayer : SimpleBasePlayer(Looper.getMainLooper()) {
    var prepares = 0
        private set
    var released = false
        private set
    var output: Any? = null
        private set
    var item: MediaItem? = null
        private set
    var startedAt = 0L
        private set
    var handlesFocus = false
        private set

    private var playWhenReady = false
    private var parameters = TrackSelectionParameters.DEFAULT
    private var attributes = AudioAttributes.DEFAULT
    private var firstFrame = false
    private var error: PlaybackException? = null

    val playing: Boolean get() = playWhenReady
    val audioOn: Boolean get() = C.TRACK_TYPE_AUDIO !in parameters.disabledTrackTypes

    fun renderFirstFrame() {
        firstFrame = true
        invalidateState()
        firstFrame = false
    }

    fun fail() {
        error = PlaybackException("test", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        invalidateState()
    }

    override fun getState(): State {
        val state = State.Builder()
            .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
            .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setTrackSelectionParameters(parameters)
            .setAudioAttributes(attributes)
            .setNewlyRenderedFirstFrame(firstFrame)
            .setPlayerError(error)
        val current = item
        if (current != null) {
            // A prepared clip has its video track selected, as a real one does once its source is read.
            val tracks = if (prepares > 0) VideoTracks else Tracks.EMPTY
            state.setPlaylist(listOf(MediaItemData.Builder(current.mediaId).setMediaItem(current).setTracks(tracks).build()))
            state.setPlaybackState(if (prepares > 0 && error == null) Player.STATE_READY else Player.STATE_IDLE)
            if (prepares > 0) state.setVideoSize(VideoSize(720, 1280))
        }
        return state.build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> = done { this.playWhenReady = playWhenReady }

    override fun handlePrepare(): ListenableFuture<*> = done {
        error = null
        prepares++
    }

    override fun handleStop(): ListenableFuture<*> = done { prepares = 0 }

    override fun handleRelease(): ListenableFuture<*> = done { released = true }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> = done { }

    override fun handleSetTrackSelectionParameters(trackSelectionParameters: TrackSelectionParameters): ListenableFuture<*> =
        done { parameters = trackSelectionParameters }

    override fun handleSetAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean): ListenableFuture<*> =
        done {
            attributes = audioAttributes
            handlesFocus = handleAudioFocus
        }

    override fun handleSetVideoOutput(videoOutput: Any): ListenableFuture<*> = done { output = videoOutput }

    override fun handleClearVideoOutput(videoOutput: Any?): ListenableFuture<*> = done { output = null }

    override fun handleSetMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> =
        done {
            item = mediaItems.firstOrNull()
            startedAt = startPositionMs
        }

    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> = done { item = null }

    private inline fun done(change: () -> Unit): ListenableFuture<*> {
        change()
        return Futures.immediateVoidFuture()
    }
}

private val VideoTracks = Tracks(
    listOf(
        Tracks.Group(
            TrackGroup(Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).setWidth(720).setHeight(1280).build()),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(true),
        ),
    ),
)

/** An engine of [FakePlayer]s that counts what the feed built, prepared and released. */
internal class FakeVideoEngine : VideoEngine {
    val built = mutableListOf<FakePlayer>()
    val idle = ArrayDeque<FakePlayer>()
    var releases = 0
        private set
    var preloaded: Map<Int, PreloadTarget> = emptyMap()
        private set
    var settled = -1
        private set

    override fun obtain(): Player = idle.removeFirstOrNull() ?: FakePlayer().also(built::add)

    override fun prepare(player: Player, page: Int, clip: VideoClip, positionMs: Long) {
        player.setMediaItem(MediaItem.Builder().setMediaId("${clip.id}@$page").setUri(clip.url).build(), positionMs)
        player.prepare()
    }

    override fun recycle(player: Player) {
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        idle.addLast(player as FakePlayer)
    }

    override fun settle(page: Int) {
        settled = page
    }

    override fun preload(clips: List<VideoClip>, targets: Map<Int, PreloadTarget>) {
        preloaded = targets
    }

    override fun release() {
        releases++
        built.forEach(Player::release)
        idle.clear()
    }

    /** The players holding a clip right now. */
    val holding: List<FakePlayer> get() = built.filter { it.item != null && !it.released }
}
