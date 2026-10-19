package com.example.hud.everything.core.perfhud.startup

import android.app.ApplicationStartInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartupTimesTest {

    @Test
    fun `a start is timed from its launch to its first frame and to fully drawn`() {
        val record = startRecordOf(
            ApplicationStartInfo.START_TYPE_COLD,
            mapOf(
                ApplicationStartInfo.START_TIMESTAMP_LAUNCH to 1_000 * MILLI,
                ApplicationStartInfo.START_TIMESTAMP_FIRST_FRAME to 1_350 * MILLI,
                ApplicationStartInfo.START_TIMESTAMP_FULLY_DRAWN to 1_600 * MILLI,
            ),
        )

        assertEquals(StartRecord(StartType.Cold, initialDisplayMillis = 350f, fullDisplayMillis = 600f), record)
    }

    @Test
    fun `each start type maps to its own kind`() {
        val types = listOf(
            ApplicationStartInfo.START_TYPE_COLD,
            ApplicationStartInfo.START_TYPE_WARM,
            ApplicationStartInfo.START_TYPE_HOT,
        )

        val kinds = types.map { startRecordOf(it, drawn)?.type }

        assertEquals(listOf(StartType.Cold, StartType.Warm, StartType.Hot), kinds)
    }

    @Test
    fun `a start of an unknown type is left out`() {
        assertNull(startRecordOf(ApplicationStartInfo.START_TYPE_UNSET, drawn))
    }

    @Test
    fun `a start without a first frame is left out`() {
        val record = startRecordOf(
            ApplicationStartInfo.START_TYPE_COLD,
            mapOf(ApplicationStartInfo.START_TIMESTAMP_LAUNCH to 1_000 * MILLI),
        )

        assertNull(record)
    }

    @Test
    fun `a start the app did not report fully drawn has no full-display time`() {
        val record = startRecordOf(ApplicationStartInfo.START_TYPE_WARM, drawn)

        assertNull(record?.fullDisplayMillis)
        assertEquals(200f, record?.initialDisplayMillis)
    }

    private companion object {
        const val MILLI = 1_000_000L

        val drawn = mapOf(
            ApplicationStartInfo.START_TIMESTAMP_LAUNCH to 1_000 * MILLI,
            ApplicationStartInfo.START_TIMESTAMP_FIRST_FRAME to 1_200 * MILLI,
        )
    }
}
