package com.base.app.core.video.policy

import com.base.app.core.video.VideoDefaults

/** How far ahead a clip's bytes are fetched: into the player's memory, or to the disk cache only. */
internal sealed interface PreloadTarget {
    /** The clip's source prepared and its first [millis] loaded into memory. */
    data class Loaded(val millis: Long) : PreloadTarget

    /** The clip's first [millis] written to the disk cache, nothing held in memory. */
    data class Cached(val millis: Long) : PreloadTarget
}

/** The rules of a video feed, apart from Media3 and Compose so each can be tested on its own. */
internal object PlaybackWindow {

    /**
     * The pages that hold a player while [current] is settled: the settled page first, then the next
     * one in the direction of travel, then the one behind, never more than [poolSize]. Pages passed
     * during a fling are never settled, so they never get one.
     */
    fun holders(current: Int, count: Int, poolSize: Int, forward: Boolean): List<Int> {
        if (current !in 0 until count || poolSize <= 0) return emptyList()
        val pages = mutableListOf(current)
        var step = 1
        while (pages.size < poolSize && (current - step >= 0 || current + step < count)) {
            val ahead = if (forward) current + step else current - step
            val behind = if (forward) current - step else current + step
            if (ahead in 0 until count) pages += ahead
            if (pages.size < poolSize && behind in 0 until count) pages += behind
            step++
        }
        return pages
    }

    /**
     * What a clip [distance] pages from the settled one keeps ready: the neighbours a few seconds in
     * memory, the next ones a second, the far ones on disk only; null outside the window, where it is
     * dropped. Google's short-form demo keeps the same shape of window.
     */
    fun preload(distance: Int): PreloadTarget? = when {
        distance < -VideoDefaults.LOOK_BEHIND || distance > VideoDefaults.LOOK_AHEAD -> null
        distance in -1..1 -> PreloadTarget.Loaded(VideoDefaults.NEAR_PRELOAD_MS)
        distance in -3..3 -> PreloadTarget.Loaded(VideoDefaults.FAR_PRELOAD_MS)
        else -> PreloadTarget.Cached(VideoDefaults.CACHED_PRELOAD_MS)
    }

    /**
     * Whether the clip on [page] plays: only the settled one, only while its screen shows and is
     * started, and under reduce motion only after a tap. [choice] is the person's own play or pause
     * for the settled clip, null until they make one.
     */
    fun plays(page: Int, current: Int, shown: Boolean, reduceMotion: Boolean, choice: Boolean?): Boolean =
        page == current && shown && (choice ?: !reduceMotion)
}
