package io.example.fitcnetdb.log

import io.example.fitcnetdb.core.common.log.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidLoggerTest {

    private val reported = mutableListOf<String>()
    private val logger = AndroidLogger(verbose = false) { message -> reported += message }

    @Test
    fun `an error is reported with its tag, and with its throwable when there is one`() {
        logger.log(LogLevel.Error, "Sync", "upload failed", throwable = null)
        logger.log(LogLevel.Error, "Sync", "upload failed", IllegalStateException("disk full"))

        assertEquals(
            listOf(
                "Sync: upload failed",
                "Sync: upload failed: java.lang.IllegalStateException: disk full",
            ),
            reported,
        )
    }

    @Test
    fun `nothing below an error is reported, verbose or not`() {
        val verbose = AndroidLogger(verbose = true) { message -> reported += message }

        for (level in listOf(LogLevel.Debug, LogLevel.Info, LogLevel.Warn)) {
            logger.log(level, "Sync", "retrying", throwable = null)
            verbose.log(level, "Sync", "retrying", throwable = null)
        }

        assertEquals(emptyList<String>(), reported)
    }

    @Test
    fun `without a reporter an error is only logged`() {
        AndroidLogger(verbose = false).log(LogLevel.Error, "Sync", "upload failed", throwable = null)

        assertEquals(emptyList<String>(), reported)
    }
}
