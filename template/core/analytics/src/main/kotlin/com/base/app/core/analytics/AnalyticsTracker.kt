package com.base.app.core.analytics

/**
 * One event worth recording.
 *
 * A typed class rather than a bare name and a map, so the analytics call sites in feature code
 * read as domain language and the vendor's parameter-name rules — length limits, reserved
 * prefixes, allowed characters — are enforced in one place instead of being discovered when a
 * dashboard silently stops receiving one event.
 */
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

/**
 * Where analytics go.
 *
 * An interface with a no-op default, for three reasons that all show up eventually: swapping
 * vendors becomes one binding rather than a search through every feature; tests do not fire real
 * events; and a build with analytics disabled needs no conditional code at the call sites, only a
 * different binding.
 *
 * [setUserId] takes a nullable so that sign-out has an unambiguous call. Leaving a stale user id
 * attached after sign-out attributes the next person's session to the previous one, which is both
 * wrong data and, on a shared device, a privacy problem.
 */
interface AnalyticsTracker {

    fun track(event: AnalyticsEvent)

    fun setUserId(userId: String?)

    fun setUserProperty(name: String, value: String?)

    /** Honours the user's opt-out. Called from settings, and respected by every implementation. */
    fun setEnabled(enabled: Boolean)
}

/**
 * Records nothing.
 *
 * The binding used in debug builds and in tests, and the one that keeps the rest of the app
 * unaware of whether analytics exist at all.
 */
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
    override fun setUserId(userId: String?) = Unit
    override fun setUserProperty(name: String, value: String?) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
}

/**
 * Where crashes and non-fatal errors go.
 *
 * Separate from [AnalyticsTracker] because the two have genuinely different lifecycles: crash
 * reporting stays on when a user opts out of product analytics, and it is initialised earlier —
 * before the DI graph is fully built, so that a crash during startup is still captured.
 */
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
