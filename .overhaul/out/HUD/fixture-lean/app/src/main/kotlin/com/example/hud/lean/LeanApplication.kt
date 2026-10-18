package com.example.hud.lean

import android.app.Application
import com.example.hud.lean.core.common.log.AppLogger
import com.example.hud.lean.core.common.log.Log
import com.example.hud.lean.core.common.log.LogLevel
import dagger.hilt.android.HiltAndroidApp

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class LeanApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG)
    }
}

/**
 * Logcat behind [Log]. Library modules have no BuildConfig of their own, so [verbose] is how the one
 * module that has one tells them what kind of build this is: below errors, release builds stay quiet.
 */
private class AndroidLogger(
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
