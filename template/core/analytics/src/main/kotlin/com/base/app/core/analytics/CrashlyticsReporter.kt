package com.base.app.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crashlytics behind the vendor-agnostic seam.
 *
 * [log] writes a breadcrumb rather than a report. Breadcrumbs are attached to the *next* crash,
 * which is what makes them worth writing at all: a stack trace tells you where the app died, and
 * the breadcrumb trail tells you what the user had been doing for the thirty seconds before it —
 * which is almost always the part that explains it.
 *
 * `AppLogger.reporter` is pointed at this from the application module, so every error the app
 * already logs becomes a breadcrumb without a second call at each site.
 */
@Singleton
class CrashlyticsReporter @Inject constructor(
    private val crashlytics: FirebaseCrashlytics,
) : CrashReporter {

    override fun recordException(throwable: Throwable, message: String?) {
        message?.let(crashlytics::log)
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun setUserId(userId: String?) {
        crashlytics.setUserId(userId.orEmpty())
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
}
