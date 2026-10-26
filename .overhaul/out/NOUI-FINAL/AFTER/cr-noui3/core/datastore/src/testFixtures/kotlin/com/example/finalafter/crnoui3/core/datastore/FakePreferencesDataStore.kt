package com.example.finalafter.crnoui3.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Preferences in memory, with no file and no scope to cancel, counting how often they are read and
 * written so a test can hold a class to the disk work it promises.
 */
class FakePreferencesDataStore : DataStore<Preferences> {

    private val state = MutableStateFlow(emptyPreferences())

    /** Collections of [data] started so far. */
    var reads = 0
        private set

    /** Edits applied so far. */
    var writes = 0
        private set

    override val data: Flow<Preferences> = flow {
        reads++
        emitAll(state)
    }

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences = transform(state.value).also {
        writes++
        state.value = it
    }
}
