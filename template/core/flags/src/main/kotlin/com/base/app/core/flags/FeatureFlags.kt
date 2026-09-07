package com.base.app.core.flags

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where a flag's value comes from.
 *
 * An interface with a local default, for the same reason [com.base.app.core.flags.Flag] exists:
 * the call sites in feature code should read the same whether the answer arrives from a server,
 * from a debug override, or from the declaration itself. Swapping the source is one binding.
 *
 * [refresh] is separate from reading on purpose. A read must never block or suspend — it happens
 * during composition — so the value is whatever the last successful refresh left behind, and the
 * refresh happens on a schedule the app chooses. A flags API that fetches on read is an API that
 * either blocks a frame or returns the default at the exact moment it matters.
 */
interface FeatureFlags {

    operator fun <T> get(flag: Flag<T>): T

    /**
     * Fetches the newest values. Returns whether anything changed.
     *
     * Safe to call often; implementations rate-limit. Call it on start rather than per screen —
     * a value that changes underneath a running screen is a bug report about a UI that "flickered
     * between two versions".
     */
    suspend fun refresh(): Boolean = false
}

/**
 * The default: every flag reads as declared, plus whatever a debug build has overridden.
 *
 * The override map is what makes flags testable by hand. Without it, checking the other side of a
 * flag means editing the remote config and waiting for a fetch, so in practice nobody checks it
 * and the disabled path ships untested.
 */
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
