package com.base.app

import android.app.Application
import com.base.app.core.common.log.AppLogger
import com.base.app.core.common.log.Log
import com.base.app.core.common.log.LogLevel
import dagger.hilt.android.HiltAndroidApp
// <opt:analytics|push|workmanager|room>
// Shared by several optional blocks below. Repeating it inside each would duplicate it when two
// are on, which is what the `a|b` marker form avoids.
import javax.inject.Inject
// </opt:analytics|push|workmanager|room>
// <opt:analytics>
import com.base.app.core.analytics.CrashReporter
// </opt:analytics>
// <opt:push|room>
import com.base.app.core.common.coroutines.ApplicationScope
import kotlinx.coroutines.launch
// </opt:push|room>
// <opt:push>
import com.base.app.core.notification.AppNotifications
import com.base.app.core.notification.PushTokenRegistrar
// </opt:push>
// <opt:room>
import com.base.app.core.network.QueuedRequestReplayer
// </opt:room>
// <opt:workmanager>
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
// </opt:workmanager>

/** The composition root. Deliberately thin. */
// <opt:workmanager>
@HiltAndroidApp
class BaseAppApplication : Application(), Configuration.Provider {
// </opt:workmanager>
// <opt:!workmanager>@HiltAndroidApp
// <opt:!workmanager>class BaseAppApplication : Application() {

    // <opt:workmanager>
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    // </opt:workmanager>

    // <opt:analytics>
    @Inject
    lateinit var crashReporter: CrashReporter
    // </opt:analytics>

    // <opt:push>
    @Inject
    lateinit var notifications: AppNotifications

    @Inject
    lateinit var pushRegistrar: PushTokenRegistrar
    // </opt:push>

    // <opt:room>
    @Inject
    lateinit var queuedRequestReplayer: QueuedRequestReplayer
    // </opt:room>

    // <opt:push|room>
    @Inject
    lateinit var appScope: ApplicationScope
    // </opt:push|room>

    override fun onCreate() {
        super.onCreate()

        // <opt:analytics>
        // Every error the app already logs becomes a breadcrumb on the next crash, without a
        // second call at each site.
        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG) { message, throwable ->
            if (throwable != null) crashReporter.recordException(throwable, message) else crashReporter.log(message)
        }
        // </opt:analytics>
        // <opt:!analytics>        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG)

        // <opt:push>
        // Channels must exist before a notification arrives, or the user cannot mute a category in
        // advance.
        notifications.createChannels()
        appScope.launch { pushRegistrar.registerOnLaunch() }
        // </opt:push>

        // <opt:room>
        appScope.launch { queuedRequestReplayer.replayWhenOnline() }
        // </opt:room>
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
