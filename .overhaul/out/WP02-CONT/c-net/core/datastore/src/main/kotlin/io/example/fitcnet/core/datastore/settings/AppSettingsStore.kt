package io.example.fitcnet.core.datastore.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.example.fitcnet.core.common.coroutines.ApplicationScope
import io.example.fitcnet.core.datastore.di.SettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The person's preferences: things they chose, which survive a sign-out. */
data class AppSettings(
    val themeMode: String = THEME_SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val analyticsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
) {
    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }
}

/** Read once at start-up and kept in memory, so the first frame and the app lock never wait on disk. */
@Singleton
class AppSettingsStore @Inject constructor(
    @SettingsDataStore private val dataStore: DataStore<Preferences>,
    scope: ApplicationScope,
) {
    private val latest: StateFlow<AppSettings?> = dataStore.data
        .map { preferences ->
            AppSettings(
                themeMode = preferences[THEME_MODE] ?: AppSettings.THEME_SYSTEM,
                dynamicColorEnabled = preferences[DYNAMIC_COLOR] ?: false,
                onboardingCompleted = preferences[ONBOARDING_DONE] ?: false,
                analyticsEnabled = preferences[ANALYTICS_ENABLED] ?: true,
                hapticsEnabled = preferences[HAPTICS_ENABLED] ?: true,
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val settings: Flow<AppSettings> = latest.filterNotNull()

    /** The settings as last read, without suspending. Null only until the first read at start-up lands. */
    val current: AppSettings? get() = latest.value

    suspend fun setThemeMode(mode: String) = put(THEME_MODE, mode)

    suspend fun setDynamicColorEnabled(enabled: Boolean) = put(DYNAMIC_COLOR, enabled)

    suspend fun setOnboardingCompleted(completed: Boolean) = put(ONBOARDING_DONE, completed)

    suspend fun setAnalyticsEnabled(enabled: Boolean) = put(ANALYTICS_ENABLED, enabled)

    suspend fun setHapticsEnabled(enabled: Boolean) = put(HAPTICS_ENABLED, enabled)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }
}
