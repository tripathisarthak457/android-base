package com.example.standard.log

import com.example.standard.core.common.log.AppLogger
import com.example.standard.core.common.log.LogLevel

/**
 * Logcat behind [com.example.standard.core.common.log.Log]. Library modules have no BuildConfig of their
 * own, so [verbose] is how the one module that has one tells them what kind of build this is: below
 * errors, release builds stay quiet.
 */
internal class AndroidLogger(
    private val verbose: Boolean,
    private val onError: ((message: String) -> Unit)? = null,
) : AppLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.Debug -> if (verbose) android.util.Log.d(tag, message, throwable)
            LogLevel.Info -> if (verbose) android.util.Log.i(tag, message, throwable)
            LogLevel.Warn -> if (verbose) android.util.Log.w(tag, message, throwable)
            LogLevel.Error -> {
                android.util.Log.e(tag, message, throwable)
                onError?.invoke(if (throwable == null) "$tag: $message" else "$tag: $message: $throwable")
            }
        }
    }
}
