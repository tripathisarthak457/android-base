package com.example.none.core.datastore.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.none.core.common.coroutines.ApplicationScope
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppSettingsStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `the settings are in memory once read, for callers that cannot wait`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "settings.preferences_pb") }
        val store = AppSettingsStore(dataStore, ApplicationScope(backgroundScope))
        assertNull(store.current)

        store.setThemeMode(AppSettings.THEME_DARK)
        store.settings.first { it.themeMode == AppSettings.THEME_DARK }
        runCurrent()

        assertEquals(AppSettings.THEME_DARK, store.current?.themeMode)
    }
}
