package com.example.finalafter.crnoui3.core.perfhud.frame

/**
 * Frames counted by their main-thread time as a share of their own refresh interval, in buckets
 * [STEP] wide up to [END] intervals and one for everything slower.
 *
 * A display with an adaptive rate draws a scroll at 120 Hz and a lone frame after idle at 60, so a
 * session's frames have different budgets. Dividing pooled frame times by any one interval would
 * let whichever rate came last decide the grade; dividing each frame by its own does not.
 *
 * Not thread-safe: [FrameLog] guards it.
 */
internal class BudgetShareHistogram {

    private val counts = IntArray(BUCKETS)

    var total: Int = 0
        private set

    fun add(share: Float) {
        counts[bucketOf(share)]++
        total++
    }

    fun clear() {
        counts.fill(0)
        total = 0
    }

    /** The share below which [fraction] of the frames fall, within [STEP] of the exact value. */
    fun percentile(fraction: Float): Float =
        percentileOf(counts, total, fraction, ::lowerEdge, ::upperEdge)

    companion object {
        const val STEP = 0.02f

        /** The last bucket holds every frame of this many intervals or more. */
        const val END = 4f

        private const val LINEAR_BUCKETS = (END / STEP).toInt()
        const val BUCKETS = LINEAR_BUCKETS + 1

        fun bucketOf(share: Float): Int = when {
            share.isNaN() || share < 0f -> 0
            share < END -> (share / STEP).toInt().coerceAtMost(LINEAR_BUCKETS - 1)
            else -> BUCKETS - 1
        }

        private fun lowerEdge(bucket: Int): Float = if (bucket >= LINEAR_BUCKETS) END else bucket * STEP

        private fun upperEdge(bucket: Int): Float = if (bucket >= LINEAR_BUCKETS) END else (bucket + 1) * STEP
    }
}
