package com.base.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.base.app.core.common.coroutines.AppDispatchers
import com.base.app.core.common.coroutines.ApplicationScope
import kotlinx.coroutines.plus
import okio.Path.Companion.toPath
import org.koin.core.scope.Scope

/**
 * Opens the preferences file [name] in the app's private storage. One instance per file for the
 * life of the process: two open on one file corrupt it. A file that is corrupt anyway is replaced
 * by an empty one rather than crashing every launch after it.
 */
internal fun Scope.preferencesFile(
    name: String,
    scope: ApplicationScope,
    dispatchers: AppDispatchers,
): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    scope = scope + dispatchers.io,
    produceFile = { "${preferencesDirectory()}/$name.preferences_pb".toPath() },
)

/** The directory preferences files live in: app-private, backed up, never shown to the person. */
internal expect fun Scope.preferencesDirectory(): String
