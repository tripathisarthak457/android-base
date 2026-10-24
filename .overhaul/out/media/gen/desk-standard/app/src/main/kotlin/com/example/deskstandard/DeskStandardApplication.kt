package com.example.deskstandard

import android.app.Application
import com.example.deskstandard.core.common.coroutines.ApplicationScope
import com.example.deskstandard.core.common.log.AppLogger
import com.example.deskstandard.core.common.log.Log
import com.example.deskstandard.core.common.log.LogLevel
import com.example.deskstandard.core.flags.FeatureFlags
import dagger.hilt.android.HiltAndroidApp
// Shared by several optional blocks below. Repeating it inside each would duplicate it when two
// are on, which is what the `a|b` marker form avoids.
import javax.inject.Inject
import kotlinx.coroutines.launch

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class DeskStandardApplication : Application() {
    @Inject
    lateinit var appScope: ApplicationScope

    @Inject
    lateinit var featureFlags: FeatureFlags

    override fun onCreate() {
        super.onCreate()

        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG)

        appScope.launch { featureFlags.refresh() }
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
