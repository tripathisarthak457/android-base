package com.example.wp04.dlplay

import android.app.Application
import com.example.wp04.dlplay.core.common.log.AppLogger
import com.example.wp04.dlplay.core.common.log.Log
import com.example.wp04.dlplay.core.common.log.LogLevel
import dagger.hilt.android.HiltAndroidApp
// Shared by several optional blocks below. Repeating it inside each would duplicate it when two
// are on, which is what the `a|b` marker form avoids.
import javax.inject.Inject
import com.example.wp04.dlplay.core.analytics.CrashReporter
import com.example.wp04.dlplay.core.common.coroutines.ApplicationScope
import kotlinx.coroutines.launch
import com.example.wp04.dlplay.core.notification.AppNotifications
import com.example.wp04.dlplay.core.notification.PushTokenRegistrar
import com.example.wp04.dlplay.core.network.QueuedRequestReplayer
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class EverythingApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var notifications: AppNotifications

    @Inject
    lateinit var pushRegistrar: PushTokenRegistrar

    @Inject
    lateinit var queuedRequestReplayer: QueuedRequestReplayer

    @Inject
    lateinit var appScope: ApplicationScope

    override fun onCreate() {
        super.onCreate()

        // Every error the app already logs becomes a breadcrumb on the next crash, without a
        // second call at each site.
        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG) { message, throwable ->
            if (throwable != null) crashReporter.recordException(throwable, message) else crashReporter.log(message)
        }

        // Channels must exist before a notification arrives, or the user cannot mute a category in
        // advance.
        notifications.createChannels()
        appScope.launch { pushRegistrar.registerOnLaunch() }

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
