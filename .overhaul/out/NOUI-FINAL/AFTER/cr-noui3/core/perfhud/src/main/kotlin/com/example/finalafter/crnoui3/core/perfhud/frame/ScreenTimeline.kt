package com.example.finalafter.crnoui3.core.perfhud.frame

/**
 * Which screen was in front when, so that each frame JankStats reports, a little after it was
 * drawn, counts for the screen it was drawn for.
 *
 * A screen is timed the way JankStats times a state put on the window: a frame belongs to the
 * newest screen entered before its main-thread work ended, so a frame that spans a navigation
 * counts for the screen it opens. It is kept here rather than put on the window's
 * `PerformanceMetricsState`, because every JankStats on the window shares that state: the
 * analytics recorder keys each frame by the states it carries, so a route put there would make it
 * build a string on every frame while the overlay records and change the events it sends.
 *
 * Entered on the main thread, read on JankStats' thread. Each entry publishes a new immutable
 * [Recent], so a frame reads one volatile field and allocates nothing; a navigation allocates one.
 */
internal class ScreenTimeline {

    @Volatile
    private var recent = NONE

    /**
     * Starts the timeline over with [name], or with no screen, as the one in front since before
     * any frame now on its way: when recording starts, frames drawn just before it are reported
     * after it, and they were drawn for the screen in front then.
     */
    fun restart(name: String?) {
        recent = if (name == null) NONE else Recent(names = arrayOf(name), sinceNanos = longArrayOf(Long.MIN_VALUE))
    }

    /** [name] is in front from [atNanos] (`System.nanoTime`) on. A repeat of the screen in front is ignored. */
    fun enter(name: String, atNanos: Long) {
        val current = recent
        val count = current.names.size
        if (count > 0 && current.names[count - 1] == name) return

        val kept = minOf(count, KEPT - 1)
        val from = count - kept
        recent = Recent(
            names = Array(kept + 1) { if (it < kept) current.names[from + it] else name },
            sinceNanos = LongArray(kept + 1) { if (it < kept) current.sinceNanos[from + it] else atNanos },
        )
    }

    /** The screen in front when a frame's main-thread work ended at [endNanos]; null before any. */
    fun screenAt(endNanos: Long): String? {
        val current = recent
        for (index in current.names.size - 1 downTo 0) {
            if (current.sinceNanos[index] < endNanos) return current.names[index]
        }
        // Before every screen kept: before the first one, or a frame reported later than KEPT
        // navigations after it was drawn, which a report a few frames late cannot be.
        return null
    }

    private class Recent(val names: Array<String>, val sinceNanos: LongArray)

    private companion object {
        /** Frames are reported a few frames after they are drawn; a navigation takes at least one. */
        const val KEPT = 8

        val NONE = Recent(names = emptyArray(), sinceNanos = LongArray(0))
    }
}
