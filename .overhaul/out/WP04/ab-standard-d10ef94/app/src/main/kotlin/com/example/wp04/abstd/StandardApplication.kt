package com.example.wp04.abstd

import android.app.Application
import com.example.wp04.abstd.core.common.log.AppLogger
import com.example.wp04.abstd.core.common.log.Log
import com.example.wp04.abstd.core.common.log.LogLevel
import dagger.hilt.android.HiltAndroidApp
// Shared by several optional blocks below. Repeating it inside each would duplicate it when two
// are on, which is what the `a|b` marker form avoids.
import javax.inject.Inject
import com.example.wp04.abstd.core.common.coroutines.ApplicationScope
import kotlinx.coroutines.launch
import com.example.wp04.abstd.core.network.QueuedRequestReplayer

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class StandardApplication : Application() {
    @Inject
    lateinit var queuedRequestReplayer: QueuedRequestReplayer

    @Inject
    lateinit var appScope: ApplicationScope

    override fun onCreate() {
        super.onCreate()

        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG)

        appScope.launch { queuedRequestReplayer.replayWhenOnline() }
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
