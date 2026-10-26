package com.example.finalafter.crnoui3.core.perfhud.recording

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.finalafter.crnoui3.core.datastore.DataStoreFiles
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** The developer's choice to record or not, kept across launches. Main-safe. */
@Singleton
internal class RecordingSettings @Inject constructor(files: DataStoreFiles) {

    private val store = files.preferences(FILE_NAME)

    /** Null until the developer has chosen. */
    suspend fun stored(): Boolean? = store.data.first()[RECORDING]

    suspend fun save(recording: Boolean) {
        store.edit { it[RECORDING] = recording }
    }

    private companion object {
        const val FILE_NAME = "perfhud"
        val RECORDING = booleanPreferencesKey("recording")
    }
}
