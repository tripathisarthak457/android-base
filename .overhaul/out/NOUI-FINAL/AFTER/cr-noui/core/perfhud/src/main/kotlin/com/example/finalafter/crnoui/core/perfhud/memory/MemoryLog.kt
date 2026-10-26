package com.example.finalafter.crnoui.core.perfhud.memory

import kotlinx.serialization.Serializable

/**
 * Memory samples over time, the last [capacity] of them, and the garbage collections that ran
 * while they were taken. Written by the sampler's thread, read by the graph and the report.
 *
 * The sampler runs only while recording with the app in front, and each run of it begins with
 * [startRun]: collections between two runs, with recording off or the app in the background, are
 * not counted, as no frame of theirs is.
 */
internal class MemoryLog(val capacity: Int = DEFAULT_CAPACITY) {

    private val lock = Any()
    private val javaHeapMb = FloatArray(capacity)
    private val nativeHeapMb = FloatArray(capacity)
    private val pssMb = FloatArray(capacity)
    private var written = 0L

    private var latest: MemoryReading? = null
    private var peakJavaHeapMb = 0f
    private var peakNativeHeapMb = 0f
    private var peakPssMb = 0f
    private var lastPssMb = Float.NaN
    private var lastGraphicsMb = Float.NaN
    private var lastGc: GcCounters? = null
    private var gcCount = 0L
    private var gcMillis = 0L
    private var blockingGcCount = 0L
    private var blockingGcMillis = 0L

    /** Adds one sample. A sample without PSS leaves the last measured PSS standing. */
    fun add(reading: MemoryReading) = synchronized(lock) {
        val at = (written % capacity).toInt()
        javaHeapMb[at] = reading.javaHeapMb
        nativeHeapMb[at] = reading.nativeHeapMb
        pssMb[at] = reading.pssMb
        written++

        latest = reading
        peakJavaHeapMb = maxOf(peakJavaHeapMb, reading.javaHeapMb)
        peakNativeHeapMb = maxOf(peakNativeHeapMb, reading.nativeHeapMb)
        if (!reading.pssMb.isNaN()) {
            lastPssMb = reading.pssMb
            lastGraphicsMb = reading.graphicsMb
            peakPssMb = maxOf(peakPssMb, reading.pssMb)
        }
        lastGc?.let { before ->
            gcCount += reading.gc.count - before.count
            gcMillis += reading.gc.millis - before.millis
            blockingGcCount += reading.gc.blockingCount - before.blockingCount
            blockingGcMillis += reading.gc.blockingMillis - before.blockingMillis
        }
        lastGc = reading.gc
    }

    /** The next sample starts a new run: what the collector did since the last one is not counted. */
    fun startRun() = synchronized(lock) { lastGc = null }

    fun clear() = synchronized(lock) {
        written = 0
        latest = null
        peakJavaHeapMb = 0f
        peakNativeHeapMb = 0f
        peakPssMb = 0f
        lastPssMb = Float.NaN
        lastGraphicsMb = Float.NaN
        lastGc = null
        gcCount = 0L
        gcMillis = 0L
        blockingGcCount = 0L
        blockingGcMillis = 0L
    }

    /** Copies the last samples into [into], oldest first, as many as it holds. */
    fun copyRecent(into: MemorySeries) = synchronized(lock) {
        val n = minOf(written, capacity.toLong(), into.capacity.toLong()).toInt()
        for (i in 0 until n) {
            val at = ((written - n + i) % capacity).toInt()
            into.javaHeapMb[i] = javaHeapMb[at]
            into.nativeHeapMb[i] = nativeHeapMb[at]
            into.pssMb[i] = pssMb[at]
        }
        into.count = n
    }

    /** Null before the first sample. */
    fun summary(): MemorySummary? = synchronized(lock) {
        val now = latest ?: return@synchronized null
        MemorySummary(
            javaHeapMb = now.javaHeapMb,
            peakJavaHeapMb = peakJavaHeapMb,
            nativeHeapMb = now.nativeHeapMb,
            peakNativeHeapMb = peakNativeHeapMb,
            pssMb = lastPssMb.takeUnless { it.isNaN() },
            peakPssMb = peakPssMb.takeIf { it > 0f },
            appPssMb = (lastPssMb - lastGraphicsMb).takeUnless { it.isNaN() },
            gcCount = gcCount,
            gcMillis = gcMillis,
            blockingGcCount = blockingGcCount,
            blockingGcMillis = blockingGcMillis,
        )
    }

    private companion object {
        /** Ten minutes at one sample a second. */
        const val DEFAULT_CAPACITY = 600
    }
}

internal class MemorySeries(val capacity: Int) {
    val javaHeapMb = FloatArray(capacity)
    val nativeHeapMb = FloatArray(capacity)
    val pssMb = FloatArray(capacity)
    var count: Int = 0
}

/** One sample. Sizes in megabytes; NaN for what this sample did not measure. */
internal data class MemoryReading(
    val javaHeapMb: Float,
    val nativeHeapMb: Float,
    val pssMb: Float,
    /** The window's buffers and the GPU's: the part of PSS that is the display's, not the app's code. */
    val graphicsMb: Float,
    val gc: GcCounters,
)

/** ART's cumulative counters for this process. */
internal data class GcCounters(
    val count: Long,
    val millis: Long,
    val blockingCount: Long,
    val blockingMillis: Long,
)

/** Sizes in megabytes, null until measured; GC counts and times while the sampler ran. */
@Serializable
internal data class MemorySummary(
    val javaHeapMb: Float,
    val peakJavaHeapMb: Float,
    val nativeHeapMb: Float,
    val peakNativeHeapMb: Float,
    val pssMb: Float?,
    val peakPssMb: Float?,
    /** PSS less graphics: the app's own memory, the figure its memory ceiling holds. */
    val appPssMb: Float?,
    val gcCount: Long,
    val gcMillis: Long,
    val blockingGcCount: Long,
    val blockingGcMillis: Long,
)
