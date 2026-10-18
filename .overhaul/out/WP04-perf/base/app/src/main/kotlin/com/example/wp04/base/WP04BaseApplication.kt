package com.example.wp04.base

import android.app.Application
import com.example.wp04.base.core.common.util.AppLogger
import dagger.hilt.android.HiltAndroidApp
// Shared by several optional blocks below. Repeating it inside each would duplicate it when two
// are on, which is what the `a|b` marker form avoids.
import javax.inject.Inject
import com.example.wp04.base.core.analytics.CrashReporter
import com.example.wp04.base.core.coroutines.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.wp04.base.core.notification.AppNotifications
import com.example.wp04.base.core.notification.PushTokenRegistrar
import com.example.wp04.base.core.network.QueuedRequestReplayer
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class WP04BaseApplication : Application(), Configuration.Provider {
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
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        // Library modules have no BuildConfig of their own — see AndroidLibraryConventionPlugin —
        // so this is where the one module that has one tells them what kind of build this is.
        AppLogger.debugEnabled = BuildConfig.DEBUG

        // Every error the app already logs becomes a breadcrumb on the next crash, without a
        // second call at each site.
        AppLogger.reporter = { message, throwable ->
            if (throwable != null) crashReporter.recordException(throwable, message) else crashReporter.log(message)
        }

        // Channels must exist before a notification arrives, or the user cannot mute a category in
        // advance.
        notifications.createChannels()
        appScope.launch { pushRegistrar.registerOnLaunch() }

        appScope.launch { queuedRequestReplayer.replayWhenOnline() }
    }
}
