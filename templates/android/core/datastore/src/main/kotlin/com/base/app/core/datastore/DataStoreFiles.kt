package com.base.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.base.app.core.common.coroutines.AppDispatchers
import com.base.app.core.common.coroutines.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.plus

/**
 * Makes one preferences file per concern, such as `search` or `profile`. A file per concern rather
 * than one shared file, because every write rewrites and syncs the whole file: a recent search would
 * otherwise wait behind a token refresh. Call [preferences] once per file, from a `@Singleton`
 * provider: two instances of one file corrupt it.
 */
@Singleton
class DataStoreFiles @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
) {
    fun preferences(fileName: String): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        // A corrupt file would otherwise throw on every read until the person clears the app's data.
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope + dispatchers.io,
        produceFile = { context.preferencesDataStoreFile(fileName) },
    )
}
