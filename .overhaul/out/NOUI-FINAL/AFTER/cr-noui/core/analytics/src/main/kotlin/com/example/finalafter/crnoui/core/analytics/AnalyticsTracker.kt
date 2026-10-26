package com.example.finalafter.crnoui.core.analytics

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

    /**
     * Honours the person's choice in settings. The application applies it at start-up and on every
     * change, so nothing is collected after they turn it off.
     */
    fun setEnabled(enabled: Boolean)
}

/** Records nothing: the binding until a vendor is chosen, and in tests. */
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
    override fun setUserId(userId: String?) = Unit
    override fun setUserProperty(name: String, value: String?) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
}

/**
 * Where crashes and non-fatal errors go. Every error the app logs becomes a breadcrumb through
 * [log]; [recordException] is for the unexpected ones only, such as an exception nothing caught.
 * Recording every handled failure buries the real problems under thousands of expected ones.
 */
interface CrashReporter {

    fun recordException(throwable: Throwable, message: String? = null)

    fun log(message: String)

    /** Honours the person's choice in settings, like [AnalyticsTracker.setEnabled]. */
    fun setEnabled(enabled: Boolean)

    fun setUserId(userId: String?)

    fun setCustomKey(key: String, value: String)
}

/** Reports nothing: the binding until a vendor is chosen, and in tests. */
class NoOpCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable, message: String?) = Unit
    override fun log(message: String) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
    override fun setUserId(userId: String?) = Unit
    override fun setCustomKey(key: String, value: String) = Unit
}
