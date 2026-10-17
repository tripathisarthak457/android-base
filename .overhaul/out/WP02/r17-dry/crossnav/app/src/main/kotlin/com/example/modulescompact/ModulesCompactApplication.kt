package com.example.modulescompact

import android.app.Application
import com.example.modulescompact.core.common.log.AppLogger
import com.example.modulescompact.core.common.log.Log
import com.example.modulescompact.core.common.log.LogLevel
import dagger.hilt.android.HiltAndroidApp

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class ModulesCompactApplication : Application() {
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
    private val onError: ((message: String, throwable: Throwable?) -> Unit)? = null,
) : AppLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.Debug -> if (verbose) android.util.Log.d(tag, message, throwable)
            LogLevel.Info -> if (verbose) android.util.Log.i(tag, message, throwable)
            LogLevel.Warn -> if (verbose) android.util.Log.w(tag, message, throwable)
            LogLevel.Error -> {
                android.util.Log.e(tag, message, throwable)
                onError?.invoke("$tag: $message", throwable)
            }
        }
    }
}
