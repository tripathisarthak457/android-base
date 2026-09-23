package com.base.app.core.analytics

/** One event worth recording. */
data class AnalyticsEvent(
    val name: String,
    val parameters: Map<String, Any?> = emptyMap(),
) {
    init {
        require(name.isNotBlank()) { "An analytics event needs a name." }
    }

    companion object {
        /** Screen views are the one event every product wants and every vendor names differently. */
        fun screenView(screenName: String) =
            AnalyticsEvent(name = "screen_view", parameters = mapOf("screen_name" to screenName))
    }
}

/** Where analytics go. */
interface AnalyticsTracker {

    fun track(event: AnalyticsEvent)

    fun setUserId(userId: String?)

    fun setUserProperty(name: String, value: String?)

    /** Honours the user's opt-out. Called from settings, and respected by every implementation. */
    fun setEnabled(enabled: Boolean)
}

/**
 * Records nothing. The binding used in debug builds and in tests, and the one that keeps the rest
 * of the app unaware of whether analytics exist at all.
 */
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
    override fun setUserId(userId: String?) = Unit
    override fun setUserProperty(name: String, value: String?) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
}

/** Where crashes and non-fatal errors go. */
interface CrashReporter {

    fun recordException(throwable: Throwable, message: String? = null)

    fun log(message: String)

    fun setUserId(userId: String?)

    fun setCustomKey(key: String, value: String)
}

class NoOpCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable, message: String?) = Unit
    override fun log(message: String) = Unit
    override fun setUserId(userId: String?) = Unit
    override fun setCustomKey(key: String, value: String) = Unit
}
