package com.example.finalafter.crnoui3.core.flags

import com.example.finalafter.crnoui3.core.common.session.SessionScopedStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Values set by hand in the inspector's Flags tab, over whatever the source says. Only builds that
 * show the inspector can reach it. Kept in memory and cleared at sign-out, so an override never
 * outlives the session it was set for.
 */
@Singleton
class FlagOverrides @Inject constructor() : SessionScopedStore {

    private val values = MutableStateFlow<Map<String, Any>>(emptyMap())

    /** The overrides by flag key. */
    val changes: StateFlow<Map<String, Any>> = values.asStateFlow()

    @Suppress("UNCHECKED_CAST") // Only set() writes, and it takes the flag's own type.
    operator fun <T> get(flag: Flag<T>): T? = values.value[flag.key] as T?

    fun <T : Any> set(flag: Flag<T>, value: T) = values.update { it + (flag.key to value) }

    fun reset(flag: Flag<*>) = values.update { it - flag.key }

    override suspend fun clear() = values.update { emptyMap() }
}
