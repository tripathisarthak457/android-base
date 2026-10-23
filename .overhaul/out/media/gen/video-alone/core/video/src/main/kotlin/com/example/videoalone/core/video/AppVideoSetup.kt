package com.example.videoalone.core.video

import okhttp3.Call

/**
 * Where the app's video comes from. Install it in `Application.onCreate`: nothing of Media3 is loaded
 * then, only the arguments are kept, and each feed reads them when it first builds its players.
 */
object AppVideoSetup {

    @Volatile
    internal var callFactory: (() -> Call.Factory)? = null
        private set

    @Volatile
    internal var events: VideoEvents? = null
        private set

    /**
     * [callFactory] is the one HTTP client the app's images and video share, so video uses its
     * connections and its certificate pins; Media3 first calls it on a loading thread, never on the
     * main one. [events] lets dev tooling and the catalog's perf log follow playback.
     */
    fun install(callFactory: () -> Call.Factory, events: VideoEvents? = null) {
        this.callFactory = callFactory
        this.events = events
    }

    internal fun requireCallFactory(): () -> Call.Factory = checkNotNull(callFactory) {
        "Video has no HTTP client: call AppVideoSetup.install(callFactory = …) in Application.onCreate"
    }
}

/** What dev tooling and the catalog's perf log hear about playback. Called on the main thread. */
interface VideoEvents {
    /** The pages that hold a player, the settled one first. */
    fun onWindow(pages: IntArray) {}

    /** A player was handed to [page], which now prepares its clip. */
    fun onAcquire(clipId: String, page: Int) {}

    fun onSettled(page: Int, elapsedRealtimeMs: Long) {}

    /** Once per preparation: a clip that loops does not report again. */
    fun onFirstFrame(clipId: String, page: Int, elapsedRealtimeMs: Long) {}

    /** The frames a clip's renderer drew and dropped, reported when it lets go of the clip. */
    fun onFrameCounts(clipId: String, rendered: Int, dropped: Int) {}

    fun onDecoderReady(clipId: String, decoder: String, initMs: Long) {}

    fun onRebuffer(clipId: String) {}
}
