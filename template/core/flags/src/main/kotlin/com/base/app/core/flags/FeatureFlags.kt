package com.base.app.core.flags

import javax.inject.Inject
import javax.inject.Singleton

/** Where a flag's value comes from. */
interface FeatureFlags {

    operator fun <T> get(flag: Flag<T>): T

    /** Fetches the newest values. Returns whether anything changed. */
    suspend fun refresh(): Boolean = false
}

/** The default: every flag reads as declared, plus whatever a debug build has overridden. */
@Singleton
class LocalFeatureFlags @Inject constructor() : FeatureFlags {

    private val overrides = mutableMapOf<String, Any>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(flag: Flag<T>): T = (overrides[flag.key] as? T) ?: flag.default

    /** Debug builds only. Nothing in release should be able to reach this. */
    fun override(flag: Flag<*>, value: Any) {
        overrides[flag.key] = value
    }

    fun clearOverrides() = overrides.clear()
}
