package com.example.hud.standard.core.common.log

import kotlin.concurrent.Volatile

/** Where log lines go. The app installs the platform backend at startup; until then [Log] is silent. */
fun interface AppLogger {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}

enum class LogLevel { Debug, Info, Warn, Error }

/** Logging, behind one seam: `Log.w("Dropped a stale page", throwable = e)`. */
object Log {

    private const val DEFAULT_TAG = "HUDStandard"

    @Volatile
    var logger: AppLogger? = null

    fun d(message: String, tag: String = DEFAULT_TAG) {
        logger?.log(LogLevel.Debug, tag, message, null)
    }

    fun i(message: String, tag: String = DEFAULT_TAG) {
        logger?.log(LogLevel.Info, tag, message, null)
    }

    fun w(message: String, throwable: Throwable? = null, tag: String = DEFAULT_TAG) {
        logger?.log(LogLevel.Warn, tag, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = DEFAULT_TAG) {
        logger?.log(LogLevel.Error, tag, message, throwable)
    }
}
