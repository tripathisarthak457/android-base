package com.example.everything.catalog.media.video

import android.content.Intent
import android.os.SystemClock
import com.example.everything.catalog.media.photos.MediaPerfLog
import com.example.everything.core.video.VideoEvents

/**
 * What the perf harness reads from the short-video scene, on the `perf.media` log tag, when the
 * catalog is launched with `--ez perfLog true`: the open, each settle, the pages holding a player and
 * each hand-over, each clip's first frame, all on one clock, and the frames each clip drew and
 * dropped. Silent otherwise.
 */
internal object VideoPerfLog : VideoEvents {

    @Volatile
    private var enabled = false
    private var openedAt = 0L
    private var openReported = false

    fun events(intent: Intent?): VideoEvents? {
        enabled = MediaPerfLog.enabled(intent)
        return this.takeIf { enabled }
    }

    /** The scene composed for the first time: its time to first frame counts from here. */
    fun opened() {
        openedAt = SystemClock.elapsedRealtime()
        openReported = false
    }

    override fun onSettled(page: Int, elapsedRealtimeMs: Long) = log("settle page=$page t=$elapsedRealtimeMs")

    override fun onWindow(pages: IntArray) = log("window pages=${pages.joinToString(",")}")

    override fun onAcquire(clipId: String, page: Int) = log("acquire page=$page clip=$clipId t=${SystemClock.elapsedRealtime()}")

    override fun onFirstFrame(clipId: String, page: Int, elapsedRealtimeMs: Long) {
        log("frame page=$page clip=$clipId t=$elapsedRealtimeMs")
        if (page == 0 && !openReported) {
            openReported = true
            log("open page=0 ms=${elapsedRealtimeMs - openedAt}")
        }
    }

    override fun onFrameCounts(clipId: String, rendered: Int, dropped: Int) =
        log("drops clip=$clipId dropped=$dropped rendered=$rendered")

    override fun onDecoderReady(clipId: String, decoder: String, initMs: Long) = log("decoder clip=$clipId name=$decoder init=$initMs")

    override fun onRebuffer(clipId: String) = log("rebuffer clip=$clipId")

    private fun log(line: String) {
        if (enabled) MediaPerfLog.log(line)
    }
}
