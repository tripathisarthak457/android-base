package com.example.everything.core.perfhud.frame

import kotlin.math.ceil
import kotlin.math.min

/**
 * Every frame recorded since the last [clear]: the most recent [capacity] in full, for the graphs
 * and the live numbers, and all of them counted per screen, for the summaries.
 *
 * Written on JankStats' thread and read from the main thread and the sampler's, under one lock
 * that the writer holds for a few array stores. Recording a frame and the live reads allocate
 * nothing, so a frame costs no garbage and no more than a microsecond; only [summary] allocates.
 */
internal class FrameLog(val capacity: Int = DEFAULT_CAPACITY) {

    private val lock = Any()

    private val starts = LongArray(capacity)
    private val totalMillis = FloatArray(capacity)
    private val uiMillis = FloatArray(capacity)
    private val intervalMillis = FloatArray(capacity)
    private val janky = BooleanArray(capacity)
    private var written = 0L
    private var shortestIntervalMillis = Float.MAX_VALUE

    private val screens = ScreenTable()

    // One slot per screen index, and one more for the whole recording.
    private val histograms = Array(SLOTS) { FrameHistogram() }
    private val uiHistogram = FrameHistogram()

    // What the score grades. An adaptive display draws a session's frames at more than one rate,
    // and each frame is held to the interval it had rather than to the newest frame's.
    private val uiShareHistogram = BudgetShareHistogram()

    private val jankyCount = IntArray(SLOTS)
    private val slowCount = IntArray(SLOTS)
    private val frozenCount = IntArray(SLOTS)
    private val longWorkCount = IntArray(SLOTS)
    private val worstMillis = FloatArray(SLOTS)
    private var busyNanos = 0L

    /**
     * Records one frame. [totalNanos] is the frame from its intended vsync to the end of its GPU
     * work where the platform reports that (API 31), its CPU time otherwise; [screen] the screen
     * it was drawn for, or null before the app reported one.
     */
    fun add(
        startNanos: Long,
        uiNanos: Long,
        totalNanos: Long,
        intervalNanos: Long,
        missedDeadline: Boolean,
        screen: String?,
    ) {
        val total = totalNanos / NANOS_PER_MILLI
        val ui = uiNanos / NANOS_PER_MILLI
        val interval = intervalNanos / NANOS_PER_MILLI
        synchronized(lock) {
            val index = screens.indexOf(screen)
            if (written > 0) busyNanos += busyOf(slot(written - 1), nextStart = startNanos)

            val at = slot(written)
            starts[at] = startNanos
            totalMillis[at] = total
            uiMillis[at] = ui
            intervalMillis[at] = interval
            janky[at] = missedDeadline
            written++
            if (interval < shortestIntervalMillis) shortestIntervalMillis = interval

            uiHistogram.add(ui)
            uiShareHistogram.add(ui / interval)
            count(index, total, ui, missedDeadline)
            count(ALL, total, ui, missedDeadline)
        }
    }

    fun clear() = synchronized(lock) {
        written = 0
        shortestIntervalMillis = Float.MAX_VALUE
        busyNanos = 0
        screens.clear()
        histograms.forEach(FrameHistogram::clear)
        uiHistogram.clear()
        uiShareHistogram.clear()
        jankyCount.fill(0)
        slowCount.fill(0)
        frozenCount.fill(0)
        longWorkCount.fill(0)
        worstMillis.fill(0f)
    }

    /**
     * The live numbers: frames per second over the last second of drawing, and the janky share of
     * everything recorded. A second without enough drawing in it reads as idle rather than as a
     * low frame rate: an app that has nothing to draw is right not to draw.
     */
    fun readLive(nowNanos: Long, into: LiveStats) = synchronized(lock) {
        val all = histograms[ALL].total
        into.jankyPercent = if (all == 0) 0f else jankyCount[ALL] * PERCENT / all
        into.refreshHz = if (written == 0L) 0 else Math.round(MILLIS_PER_SECOND / intervalMillis[slot(written - 1)])

        val since = nowNanos - LIVE_WINDOW_NANOS
        var frames = 0
        var busy = 0L
        var next = Long.MIN_VALUE
        var newest = Long.MIN_VALUE
        var frame = written - 1
        val oldest = maxOf(0L, written - capacity)
        while (frame >= oldest) {
            val at = slot(frame)
            if (starts[at] < since) break
            if (newest == Long.MIN_VALUE) newest = starts[at]
            busy += busyOf(at, next)
            next = starts[at]
            frames++
            frame--
        }
        into.framesPerSecond = when {
            frames < MIN_LIVE_FRAMES || busy < MIN_LIVE_BUSY_NANOS || nowNanos - newest > IDLE_AFTER_NANOS ->
                LiveStats.IDLE
            else -> Math.round(frames * NANOS_PER_SECOND / busy)
        }
    }

    /** Whether a frame started less than [withinNanos] before [nowNanos]. */
    fun drewWithin(nowNanos: Long, withinNanos: Long): Boolean = synchronized(lock) {
        written > 0 && nowNanos - starts[slot(written - 1)] < withinNanos
    }

    /** Copies the last frames into [into], as many as it holds, oldest first. */
    fun copyRecent(into: FrameSeries) = synchronized(lock) {
        val n = min(min(written, capacity.toLong()), into.capacity.toLong()).toInt()
        for (i in 0 until n) {
            val at = slot(written - n + i)
            into.totalMillis[i] = totalMillis[at]
            into.uiMillis[i] = uiMillis[at]
            into.intervalMillis[i] = intervalMillis[at]
            into.janky[i] = janky[at]
        }
        into.count = n
    }

    /**
     * Frames per second of drawing in each of the last [into]'s size seconds before [nowNanos],
     * oldest first; [FpsSeries.NONE] for a second with nothing drawn, or older than the ring.
     */
    fun copyFramesPerSecond(nowNanos: Long, into: FpsSeries) = synchronized(lock) {
        into.frames.fill(0)
        into.busyNanos.fill(0L)
        val seconds = into.seconds
        val windowStart = nowNanos - seconds * NANOS_PER_SECOND_LONG
        var next = Long.MIN_VALUE
        var frame = written - 1
        val oldest = maxOf(0L, written - capacity)
        while (frame >= oldest) {
            val at = slot(frame)
            val start = starts[at]
            if (start < windowStart) break
            val second = ((start - windowStart) / NANOS_PER_SECOND_LONG).toInt().coerceIn(0, seconds - 1)
            into.frames[second]++
            into.busyNanos[second] += busyOf(at, next)
            next = start
            frame--
        }
        for (second in 0 until seconds) {
            into.perSecond[second] = if (into.frames[second] == 0 || into.busyNanos[second] == 0L) {
                FpsSeries.NONE
            } else {
                into.frames[second] * NANOS_PER_SECOND / into.busyNanos[second]
            }
        }
    }

    /** Copies the whole recording's main-thread-time histogram into [into], for the histogram graph. */
    fun copyMainThreadHistogram(into: FrameHistogram) = synchronized(lock) { uiHistogram.copyInto(into) }

    /** Copies the running totals into [into], so a reader can take the difference between two. */
    fun copyTotals(into: FrameTotals) = synchronized(lock) {
        into.frames = histograms[ALL].total
        into.janky = jankyCount[ALL]
        into.slow = slowCount[ALL]
        into.frozen = frozenCount[ALL]
        into.longWork = longWorkCount[ALL]
        into.busyNanos = busyNanos
        histograms[ALL].copyInto(into.histogram)
        uiHistogram.copyInto(into.uiHistogram)
    }

    /**
     * Everything recorded, summed up. Allocates, so it is never called per frame: twice a second
     * while the Perf tab is open, once per screen the log reports leaving, and once per export.
     */
    fun summary(): FrameSummary = synchronized(lock) {
        val perScreen = ArrayList<ScreenFrames>(screens.size)
        for (index in 0 until screens.size) {
            if (histograms[index].total == 0) continue
            perScreen += ScreenFrames(
                name = screens.nameAt(index),
                isOther = index == ScreenTable.OTHER,
                stats = statsOf(index),
            )
        }
        val all = histograms[ALL].total
        FrameSummary(
            overall = statsOf(ALL),
            uiP50Millis = uiHistogram.percentile(P50),
            uiP90Millis = uiHistogram.percentile(P90),
            uiP99Millis = uiHistogram.percentile(P99),
            uiP90BudgetShare = uiShareHistogram.percentile(P90),
            uiP99BudgetShare = uiShareHistogram.percentile(P99),
            framesPerSecond = if (busyNanos == 0L) 0f else (all - 1).coerceAtLeast(0) * NANOS_PER_SECOND / busyNanos,
            refreshHz = if (written == 0L) 0f else MILLIS_PER_SECOND / shortestIntervalMillis,
            screens = perScreen,
        )
    }

    private fun count(index: Int, total: Float, ui: Float, missedDeadline: Boolean) {
        histograms[index].add(total)
        if (missedDeadline) jankyCount[index]++
        if (total > SLOW_MILLIS) slowCount[index]++
        if (total > FROZEN_MILLIS) frozenCount[index]++
        if (ui > LONG_WORK_MILLIS) longWorkCount[index]++
        if (total > worstMillis[index]) worstMillis[index] = total
    }

    private fun statsOf(index: Int): FrameStats {
        val histogram = histograms[index]
        return FrameStats(
            frames = histogram.total,
            janky = jankyCount[index],
            slow = slowCount[index],
            frozen = frozenCount[index],
            longWork = longWorkCount[index],
            p50Millis = histogram.percentile(P50),
            p90Millis = histogram.percentile(P90),
            p99Millis = histogram.percentile(P99),
            worstMillis = worstMillis[index],
        )
    }

    /**
     * The time a frame kept the display busy: until the next frame started, but never longer than
     * its own duration rounded up to whole refresh intervals, so a pause after it counts as idle.
     */
    private fun busyOf(at: Int, nextStart: Long): Long {
        val interval = intervalMillis[at] * NANOS_PER_MILLI
        val rounded = (ceil(totalMillis[at] / intervalMillis[at]).coerceAtLeast(1f) * interval).toLong()
        return if (nextStart == Long.MIN_VALUE) rounded else min(nextStart - starts[at], rounded)
    }

    private fun slot(frame: Long): Int = (frame % capacity).toInt()

    companion object {
        /** About 68 seconds of uninterrupted drawing at 120 Hz, in about 180 KB. */
        const val DEFAULT_CAPACITY = 8192

        /** Android vitals' thresholds. */
        const val SLOW_MILLIS = 16f
        const val FROZEN_MILLIS = 700f

        /** The web's long-task threshold, which a frame's UI-thread work past it is. */
        const val LONG_WORK_MILLIS = 50f

        private const val SLOTS = ScreenTable.CAPACITY + 1
        private const val ALL = ScreenTable.CAPACITY
        private const val P50 = 0.5f
        private const val P90 = 0.9f
        private const val P99 = 0.99f
        private const val PERCENT = 100f
        private const val NANOS_PER_MILLI = 1_000_000f
        private const val NANOS_PER_SECOND = 1_000_000_000f
        private const val NANOS_PER_SECOND_LONG = 1_000_000_000L
        private const val MILLIS_PER_SECOND = 1000f
        private const val LIVE_WINDOW_NANOS = 1_000_000_000L
        private const val IDLE_AFTER_NANOS = 500_000_000L
        private const val MIN_LIVE_FRAMES = 3
        private const val MIN_LIVE_BUSY_NANOS = 100_000_000L
    }
}
