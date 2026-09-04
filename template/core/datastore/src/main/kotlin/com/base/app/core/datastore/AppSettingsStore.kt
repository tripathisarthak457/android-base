package com.base.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.base.app.core.datastore.di.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The user's preferences: things they chose, which must survive a sign-out.
 *
 * Exposed as flows rather than suspend reads because the theme in particular is consumed by the
 * composition at the very root of the app — it has to *react*, not be fetched once at startup and
 * then be wrong until the next launch.
 */
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

@Singleton
class AppSettingsStore @Inject constructor(
    @SettingsDataStore private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AppSettings> = dataStore.data
        .map { preferences ->
            AppSettings(
                themeMode = preferences[THEME_MODE] ?: AppSettings.THEME_SYSTEM,
                dynamicColorEnabled = preferences[DYNAMIC_COLOR] ?: false,
                onboardingCompleted = preferences[ONBOARDING_DONE] ?: false,
                analyticsEnabled = preferences[ANALYTICS_ENABLED] ?: true,
                hapticsEnabled = preferences[HAPTICS_ENABLED] ?: true,
            )
        }
        .distinctUntilChanged()

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
