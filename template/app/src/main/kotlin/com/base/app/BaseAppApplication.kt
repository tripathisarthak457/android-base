package com.base.app

import android.app.Application
import com.base.app.core.common.util.AppLogger
import dagger.hilt.android.HiltAndroidApp
// <opt:analytics|push|workmanager>
// Needed by whichever of the three blocks below survives, and by none of them alone — which is
// what the `a|b` marker form is for. Repeating it inside each would duplicate it when two are on.
import javax.inject.Inject
// </opt:analytics|push|workmanager>
// <opt:analytics>
import com.base.app.core.analytics.CrashReporter
// </opt:analytics>
// <opt:push>
import com.base.app.core.notification.AppNotifications
// </opt:push>
// <opt:workmanager>
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
// </opt:workmanager>

/**
 * The composition root.
 *
 * Deliberately thin. Everything here is either "wire two modules together" or "the one thing that
 * genuinely has to happen before any screen exists" — anything more and the application class
 * becomes where work goes when nobody can think of a better home, and cold-start time goes with
 * it.
 */
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
    // </opt:push>

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
        // Channels have to exist before the user can find them in system settings. Creating them
        // lazily means someone must receive a notification from a category before they are able
        // to mute it.
        notifications.createChannels()
        // </opt:push>
    }
}
