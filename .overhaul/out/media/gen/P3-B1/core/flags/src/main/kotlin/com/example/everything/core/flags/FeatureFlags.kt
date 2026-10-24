package com.example.everything.core.flags

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Where a flag's value comes from. Overrides set in the inspector win over every source. */
interface FeatureFlags {

    /** The value now. Cheap: read it where it is needed rather than caching it. */
    operator fun <T> get(flag: Flag<T>): T

    /** The value now and every change after it, for a screen that should follow a refresh. */
    fun <T> observe(flag: Flag<T>): Flow<T>

    /** Fetches the newest values from the source. Returns whether anything changed. */
    suspend fun refresh(): Boolean
}

/** Every flag as declared, plus the inspector's overrides: the source until a vendor is bound. */
@Singleton
class LocalFeatureFlags @Inject constructor(
    private val overrides: FlagOverrides,
) : FeatureFlags {

    override fun <T> get(flag: Flag<T>): T = overrides[flag] ?: flag.default

    override fun <T> observe(flag: Flag<T>): Flow<T> = overrides.changes.map { get(flag) }.distinctUntilChanged()

    override suspend fun refresh(): Boolean = false
}
