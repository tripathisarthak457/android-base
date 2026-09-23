package com.base.app

import android.app.Application
import com.base.app.core.common.util.AppLogger
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
import com.base.app.core.coroutines.ApplicationScope
import kotlinx.coroutines.CoroutineScope
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
    @ApplicationScope
    lateinit var appScope: CoroutineScope
    // </opt:push|room>

    override fun onCreate() {
        super.onCreate()

        // Library modules have no BuildConfig of their own — see AndroidLibraryConventionPlugin —
        // so this is where the one module that has one tells them what kind of build this is.
        AppLogger.debugEnabled = BuildConfig.DEBUG

        // <opt:analytics>
        // Every error the app already logs becomes a breadcrumb on the next crash, without a
        // second call at each site.
        AppLogger.reporter = { message, throwable ->
            if (throwable != null) crashReporter.recordException(throwable, message) else crashReporter.log(message)
        }
        // </opt:analytics>

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
