package com.base.app.core.perfhud.frame

import kotlinx.serialization.Serializable

/**
 * The pill's numbers, filled in place by [FrameLog.readLive] so a refresh allocates nothing.
 * Confined to the thread that reads it.
 */
internal class LiveStats {
    /** [IDLE] when the app drew too little in the last second to call it a frame rate. */
    var framesPerSecond: Int = IDLE
    var refreshHz: Int = 0
    var jankyPercent: Float = 0f

    companion object {
        const val IDLE = -1
    }
}

/** The last frames, for the frame-time graph. Filled by [FrameLog.copyRecent]. */
internal class FrameSeries(val capacity: Int) {
    val totalMillis = FloatArray(capacity)
    val uiMillis = FloatArray(capacity)
    val intervalMillis = FloatArray(capacity)
    val janky = BooleanArray(capacity)
    var count: Int = 0
}

/** Frames per second of drawing, second by second. Filled by [FrameLog.copyFramesPerSecond]. */
internal class FpsSeries(val seconds: Int) {
    val perSecond = FloatArray(seconds)
    internal val frames = IntArray(seconds)
    internal val busyNanos = LongArray(seconds)

    companion object {
        /** A second in which nothing was drawn. */
        const val NONE = -1f
    }
}

/** The running totals of a recording, filled by [FrameLog.copyTotals]. */
internal class FrameTotals {
    var frames: Int = 0
    var janky: Int = 0
    var slow: Int = 0
    var frozen: Int = 0
    var longWork: Int = 0
    var busyNanos: Long = 0L
    val histogram = FrameHistogram()
}

/**
 * What a set of frames came to. Times are each frame's full duration, from its vsync to the end of
 * its GPU work, in milliseconds. The percentages are derived, and kept as properties so the
 * exported report carries them.
 */
@Serializable
internal data class FrameStats(
    val frames: Int,
    val janky: Int,
    val slow: Int,
    val frozen: Int,
    val longWork: Int,
    val p50Millis: Float,
    val p90Millis: Float,
    val p99Millis: Float,
    val worstMillis: Float,
    val jankyPercent: Float = percentOf(janky, frames),
    val slowPercent: Float = percentOf(slow, frames),
    val frozenPercent: Float = percentOf(frozen, frames),
)

private fun percentOf(part: Int, whole: Int): Float = if (whole == 0) 0f else part * 100f / whole

/**
 * One screen's frames. [name] is the name the app reported; null with [isOther] false for frames
 * drawn before the first report, and null with [isOther] true for the screens past the table's
 * capacity.
 */
@Serializable
internal data class ScreenFrames(val name: String?, val isOther: Boolean, val stats: FrameStats)

@Serializable
internal data class FrameSummary(
    val overall: FrameStats,
    val uiP50Millis: Float,
    val uiP90Millis: Float,
    val uiP99Millis: Float,
    /** While the app was drawing; idle time does not lower it. */
    val framesPerSecond: Float,
    val refreshHz: Float,
    val screens: List<ScreenFrames>,
)
