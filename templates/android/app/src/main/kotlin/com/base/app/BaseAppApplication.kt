package com.base.app

import android.app.Application
import com.base.app.core.common.log.AppLogger
import com.base.app.core.common.log.Log
import com.base.app.core.common.log.LogLevel
import dagger.hilt.android.HiltAndroidApp
// <opt:analytics|push|flags|offlinequeue|websocket>
// Shared by several optional blocks below. Repeating it inside each would duplicate it when two
// are on, which is what the `a|b` marker form avoids.
import javax.inject.Inject
// </opt:analytics|push|flags|offlinequeue|websocket>
// <opt:analytics|push|flags>
import com.base.app.core.common.coroutines.ApplicationScope
import kotlinx.coroutines.launch
// </opt:analytics|push|flags>
// <opt:analytics>
import com.base.app.core.analytics.AnalyticsTracker
import com.base.app.core.analytics.CrashReporter
import com.base.app.core.datastore.settings.AppSettingsStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
// </opt:analytics>
// <opt:push>
import com.base.app.core.notification.channel.NotificationChannels
import com.base.app.core.notification.push.PushRegistration
// </opt:push>
// <opt:flags>
import com.base.app.core.flags.FeatureFlags
// </opt:flags>
// <opt:offlinequeue>
import androidx.work.Configuration
import com.base.app.core.network.queue.OfflineQueueWorkerFactory
// </opt:offlinequeue>
// <opt:websocket>
import com.base.app.core.network.websocket.RealtimeLifecycle
// </opt:websocket>

/** The composition root. Deliberately thin. */
@HiltAndroidApp
// <opt:offlinequeue>
class BaseAppApplication : Application(), Configuration.Provider {
// </opt:offlinequeue>
// <opt:!offlinequeue>class BaseAppApplication : Application() {

    // <opt:offlinequeue>
    @Inject
    lateinit var workerFactory: OfflineQueueWorkerFactory

    // Read by WorkManager the first time something asks for it, not at start-up: the manifest
    // removes its App Startup initializer, so a launch that queues nothing never opens its database.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
    // </opt:offlinequeue>

    // <opt:analytics|push|flags>
    @Inject
    lateinit var appScope: ApplicationScope
    // </opt:analytics|push|flags>

    // <opt:analytics>
    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var analytics: AnalyticsTracker

    @Inject
    lateinit var settings: AppSettingsStore
    // </opt:analytics>

    // <opt:push>
    @Inject
    lateinit var notificationChannels: NotificationChannels

    @Inject
    lateinit var pushRegistration: PushRegistration
    // </opt:push>

    // <opt:flags>
    @Inject
    lateinit var featureFlags: FeatureFlags
    // </opt:flags>

    // <opt:websocket>
    @Inject
    lateinit var realtime: RealtimeLifecycle
    // </opt:websocket>

    override fun onCreate() {
        super.onCreate()

        // <opt:analytics>
        // Every error the app logs becomes a breadcrumb on the next crash report, without a second
        // call at each site. Only what nothing handled is recorded as an error of its own.
        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG) { message -> crashReporter.log(message) }

        // The choice in settings reaches both collectors before anything is sent, and follows every
        // change after.
        appScope.launch {
            settings.settings.map { it.analyticsEnabled }.distinctUntilChanged().collect { enabled ->
                analytics.setEnabled(enabled)
                crashReporter.setEnabled(enabled)
            }
        }
        // </opt:analytics>
        // <opt:!analytics>        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG)

        // <opt:push>
        // Channels exist before the first notification arrives, so each can be muted in advance.
        appScope.launch { notificationChannels.create() }
        pushRegistration.start()
        // </opt:push>

        // <opt:flags>
        appScope.launch { featureFlags.refresh() }
        // </opt:flags>

        // <opt:websocket>
        realtime.start()
        // </opt:websocket>
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
