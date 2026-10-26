package com.example.finalafter.crui2

import android.app.Application
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.finalafter.crui2.core.analytics.AnalyticsTracker
import com.example.finalafter.crui2.core.analytics.CrashReporter
import com.example.finalafter.crui2.core.common.coroutines.ApplicationScope
import com.example.finalafter.crui2.core.common.log.AppLogger
import com.example.finalafter.crui2.core.common.log.Log
import com.example.finalafter.crui2.core.common.log.LogLevel
import com.example.finalafter.crui2.core.datastore.settings.AppSettingsStore
import com.example.finalafter.crui2.core.flags.FeatureFlags
import com.example.finalafter.crui2.core.network.queue.OfflineQueueWorkerFactory
import com.example.finalafter.crui2.core.network.websocket.RealtimeLifecycle
import com.example.finalafter.crui2.core.notification.channel.NotificationChannels
import com.example.finalafter.crui2.core.notification.push.PushRegistration
import dagger.hilt.android.HiltAndroidApp
// Shared by several optional blocks below. Repeating it inside each would duplicate it when two
// are on, which is what the `a|b` marker form avoids.
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class FinalAfterApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: OfflineQueueWorkerFactory

    // Read by WorkManager the first time something asks for it, not at start-up: the manifest
    // removes its App Startup initializer, so a launch that queues nothing never opens its database.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    @Inject
    lateinit var appScope: ApplicationScope

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var analytics: AnalyticsTracker

    @Inject
    lateinit var settings: AppSettingsStore

    @Inject
    lateinit var notificationChannels: NotificationChannels

    @Inject
    lateinit var pushRegistration: PushRegistration

    @Inject
    lateinit var featureFlags: FeatureFlags

    @Inject
    lateinit var realtime: RealtimeLifecycle

    // Lazy: injecting the client itself would build it during start-up, before any image needs it.
    @Inject
    lateinit var okHttp: dagger.Lazy<OkHttpClient>

    override fun onCreate() {
        super.onCreate()

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

        // Channels exist before the first notification arrives, so each can be muted in advance.
        appScope.launch { notificationChannels.create() }
        pushRegistration.start()

        appScope.launch { featureFlags.refresh() }

        realtime.start()

        // Images go through the app's own client, so they share its connections and its certificate
        // pins; Coil's default would build a client of its own that has neither. Its HTTP cache is
        // left out because Coil keeps the images in a disk cache of its own.
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = { okHttp.get().newBuilder().cache(null).build() },
                        ),
                    )
                }
                .build()
        }
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
