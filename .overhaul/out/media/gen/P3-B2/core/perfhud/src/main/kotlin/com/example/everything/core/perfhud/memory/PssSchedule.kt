package com.example.everything.core.perfhud.memory

/**
 * When the sampler may read PSS: at most once every [intervalMillis], and only while the app is not
 * drawing.
 *
 * Reading PSS walks every memory mapping of the process while holding the kernel's lock on its
 * address space, and a thread that faults in a page or maps memory meanwhile waits for it, the
 * render thread included: a read during drawing can hold a frame past its deadline (measured in
 * `docs/PERFORMANCE-OVERLAY.md`). So a read waits for a pause in the drawing: an app that draws
 * without pause has its PSS read later, never its frames delayed. Not thread-safe; the sampler's
 * loop is its only caller.
 */
internal class PssSchedule(private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS) {

    private var lastReadAt = NEVER

    /** Whether to read PSS now; a true answer counts as the read. */
    fun shouldRead(nowMillis: Long, drawing: Boolean): Boolean {
        if (drawing) return false
        if (lastReadAt != NEVER && nowMillis - lastReadAt < intervalMillis) return false
        lastReadAt = nowMillis
        return true
    }

    companion object {
        const val DEFAULT_INTERVAL_MILLIS = 15_000L

        /**
         * How long no frame may have started for the app to count as not drawing. Shorter than the
         * Performance tab's half-second refresh, so an open tab still lets PSS be read between its
         * redraws.
         */
        const val QUIET_NANOS = 250_000_000L

        private const val NEVER = Long.MIN_VALUE
    }
}
