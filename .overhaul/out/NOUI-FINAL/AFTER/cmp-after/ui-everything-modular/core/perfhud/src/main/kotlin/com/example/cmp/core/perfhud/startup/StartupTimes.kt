package com.example.cmp.core.perfhud.startup

import android.app.ActivityManager
import android.app.ApplicationStartInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.serialization.Serializable

/**
 * The app's recent starts as the system timed them, from Android 15's `ApplicationStartInfo`:
 * from the launch to the first frame (time to initial display) and to `reportFullyDrawn` (time to
 * full display). Earlier releases keep no such record, and an estimate taken from inside the app
 * would leave out the part of a cold start before the app's code runs, so they get none.
 *
 * [read] makes a binder call: off the main thread.
 */
internal class StartupTimes @Inject constructor(@ApplicationContext private val context: Context) {

    fun read(): StartupSummary {
        if (Build.VERSION.SDK_INT <
            Build.VERSION_CODES.VANILLA_ICE_CREAM
        ) {
            return StartupSummary.Unsupported
        }
        val starts = context.getSystemService(ActivityManager::class.java)
            .getHistoricalProcessStartReasons(MAX_STARTS)
            .mapNotNull(::recordOf)
        return StartupSummary(
            supported = true,
            cold = starts.firstOrNull { it.type == StartType.Cold },
            warm = starts.firstOrNull { it.type == StartType.Warm },
            hot = starts.firstOrNull { it.type == StartType.Hot },
        )
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun recordOf(info: ApplicationStartInfo): StartRecord? =
        startRecordOf(info.startType, info.startupTimestamps)

    private companion object {
        /** The system keeps the most recent starts first; the latest of each kind is what is shown. */
        const val MAX_STARTS = 20
    }
}

/**
 * One start, from an `ApplicationStartInfo`'s start type and its timestamps by `START_TIMESTAMP_*`
 * key. Null for a type this does not show, and for a start with no launch or first-frame time: one
 * still under way, or one that never drew.
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal fun startRecordOf(startType: Int, timestamps: Map<Int, Long>): StartRecord? {
    val type = when (startType) {
        ApplicationStartInfo.START_TYPE_COLD -> StartType.Cold
        ApplicationStartInfo.START_TYPE_WARM -> StartType.Warm
        ApplicationStartInfo.START_TYPE_HOT -> StartType.Hot
        else -> return null
    }
    val launch = timestamps[ApplicationStartInfo.START_TIMESTAMP_LAUNCH] ?: return null
    val firstFrame = timestamps[ApplicationStartInfo.START_TIMESTAMP_FIRST_FRAME] ?: return null
    val fullyDrawn = timestamps[ApplicationStartInfo.START_TIMESTAMP_FULLY_DRAWN]
    return StartRecord(
        type = type,
        initialDisplayMillis = (firstFrame - launch) / NANOS_PER_MILLI,
        fullDisplayMillis = fullyDrawn?.let { (it - launch) / NANOS_PER_MILLI },
    )
}

private const val NANOS_PER_MILLI = 1_000_000f

@Serializable
internal enum class StartType { Cold, Warm, Hot }

/** One start. [fullDisplayMillis] is null when the app did not call `reportFullyDrawn`. */
@Serializable
internal data class StartRecord(
    val type: StartType,
    val initialDisplayMillis: Float,
    val fullDisplayMillis: Float?,
)

/** The latest start of each kind; null for a kind the system has no record of. */
@Serializable
internal data class StartupSummary(
    val supported: Boolean,
    val cold: StartRecord?,
    val warm: StartRecord?,
    val hot: StartRecord?,
) {
    companion object {
        val Unsupported = StartupSummary(supported = false, cold = null, warm = null, hot = null)
    }
}
