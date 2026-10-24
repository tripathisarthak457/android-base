package com.base.app.core.datastore.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.base.app.core.common.coroutines.ApplicationScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The person's preferences: things they chose, which survive a sign-out. */
data class AppSettings(
    val themeMode: String = THEME_SYSTEM,
    val onboardingCompleted: Boolean = false,
) {
    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }
}

/** Read once at start-up and kept in memory, so the first frame never waits on disk. */
class AppSettingsStore(
    private val dataStore: DataStore<Preferences>,
    scope: ApplicationScope,
) {
    private val latest: StateFlow<AppSettings?> = dataStore.data
        .map { preferences ->
            AppSettings(
                themeMode = preferences[THEME_MODE] ?: AppSettings.THEME_SYSTEM,
                onboardingCompleted = preferences[ONBOARDING_DONE] ?: false,
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val settings: Flow<AppSettings> = latest.filterNotNull()

    /** The settings as last read, without suspending. Null only until the first read at start-up lands. */
    val current: AppSettings? get() = latest.value

    suspend fun setThemeMode(mode: String) = put(THEME_MODE, mode)

    suspend fun setOnboardingCompleted(completed: Boolean) = put(ONBOARDING_DONE, completed)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
    }
}
