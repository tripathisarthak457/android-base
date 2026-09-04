package com.base.app.core.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase behind the vendor-agnostic seam.
 *
 * Parameters are coerced to the handful of types Firebase actually accepts. Anything else it
 * drops silently, at runtime, on the user's device — so a `LocalDate` passed as a parameter
 * produces an event that arrives with a field missing and no error anywhere to explain it.
 * Converting here means the event always carries what the call site meant.
 */
@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        firebaseAnalytics.logEvent(event.name) {
            event.parameters.forEach { (key, value) ->
                when (value) {
                    null -> Unit
                    is Long -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Double -> param(key, value)
                    is Float -> param(key, value.toDouble())
                    else -> param(key, value.toString())
                }
            }
        }
    }

    override fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
    }

    override fun setUserProperty(name: String, value: String?) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    override fun setEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
}
