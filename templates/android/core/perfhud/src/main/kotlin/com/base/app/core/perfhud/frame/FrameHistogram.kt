package com.base.app.core.perfhud.frame

/**
 * Frame times counted into fixed buckets, fine where frames are judged and coarse beyond: a
 * quarter of a millisecond up to 32 ms, then 2 ms up to 160 ms, 20 ms up to 800 ms, and one bucket
 * for everything slower. Percentiles read from it are within one bucket's width of the exact
 * value, which keeps every frame of a session in a kilobyte instead of a list that grows.
 *
 * Not thread-safe: [FrameLog] guards each one.
 */
internal class FrameHistogram {

    private val counts = IntArray(BUCKETS)

    var total: Int = 0
        private set

    fun add(millis: Float) {
        counts[bucketOf(millis)]++
        total++
    }

    fun clear() {
        counts.fill(0)
        total = 0
    }

    fun copyInto(other: FrameHistogram) {
        counts.copyInto(other.counts)
        other.total = total
    }

    /** This one less [earlier], an older copy of it: the frames counted since that copy. */
    fun subtract(earlier: FrameHistogram, into: FrameHistogram) {
        for (index in 0 until BUCKETS) into.counts[index] = counts[index] - earlier.counts[index]
        into.total = total - earlier.total
    }

    fun count(bucket: Int): Int = counts[bucket]

    /**
     * The frame time below which [fraction] of the frames fall, interpolated within the bucket
     * it lands in; 0 with no frames.
     */
    fun percentile(fraction: Float): Float =
        percentileOf(counts, total, fraction, ::lowerEdge, ::upperEdge)

    companion object {
        private const val FINE_END = 32f
        private const val FINE_STEP = 0.25f
        private const val MEDIUM_END = 160f
        private const val MEDIUM_STEP = 2f
        private const val COARSE_END = 800f
        private const val COARSE_STEP = 20f

        private const val FINE_BUCKETS = (FINE_END / FINE_STEP).toInt()
        private const val MEDIUM_BUCKETS = ((MEDIUM_END - FINE_END) / MEDIUM_STEP).toInt()
        private const val COARSE_BUCKETS = ((COARSE_END - MEDIUM_END) / COARSE_STEP).toInt()

        /** The last bucket holds every frame of [COARSE_END] ms or more. */
        const val BUCKETS = FINE_BUCKETS + MEDIUM_BUCKETS + COARSE_BUCKETS + 1

        fun bucketOf(millis: Float): Int = when {
            millis.isNaN() || millis < 0f -> 0

            millis < FINE_END -> (millis / FINE_STEP).toInt()

            millis < MEDIUM_END -> FINE_BUCKETS + ((millis - FINE_END) / MEDIUM_STEP).toInt()

            millis < COARSE_END ->
                FINE_BUCKETS + MEDIUM_BUCKETS +
                    ((millis - MEDIUM_END) / COARSE_STEP).toInt()

            else -> BUCKETS - 1
        }

        fun lowerEdge(bucket: Int): Float = when {
            bucket < FINE_BUCKETS -> bucket * FINE_STEP

            bucket < FINE_BUCKETS + MEDIUM_BUCKETS ->
                FINE_END +
                    (bucket - FINE_BUCKETS) * MEDIUM_STEP

            bucket < BUCKETS - 1 ->
                MEDIUM_END +
                    (bucket - FINE_BUCKETS - MEDIUM_BUCKETS) * COARSE_STEP

            else -> COARSE_END
        }

        /** The last bucket has no upper edge; a frame in it is reported at [COARSE_END]. */
        fun upperEdge(bucket: Int): Float = if (bucket >=
            BUCKETS - 1
        ) {
            COARSE_END
        } else {
            lowerEdge(bucket + 1)
        }
    }
}

/**
 * The value below which [fraction] of the [total] counted fall, interpolated within the bucket it
 * lands in; 0 with nothing counted. The last bucket is open-ended and reads as its lower edge.
 */
internal inline fun percentileOf(
    counts: IntArray,
    total: Int,
    fraction: Float,
    lowerEdge: (bucket: Int) -> Float,
    upperEdge: (bucket: Int) -> Float,
): Float {
    if (total == 0) return 0f
    val rank = fraction.coerceIn(0f, 1f) * total
    var below = 0
    for (bucket in counts.indices) {
        val inBucket = counts[bucket]
        if (inBucket > 0 && below + inBucket >= rank) {
            val within = ((rank - below) / inBucket).coerceIn(0f, 1f)
            return lowerEdge(bucket) + within * (upperEdge(bucket) - lowerEdge(bucket))
        }
        below += inBucket
    }
    return lowerEdge(counts.size - 1)
}
