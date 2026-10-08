package com.base.app.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/** Crashlytics behind the vendor-agnostic seam. [log] writes a breadcrumb rather than a report. */
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
