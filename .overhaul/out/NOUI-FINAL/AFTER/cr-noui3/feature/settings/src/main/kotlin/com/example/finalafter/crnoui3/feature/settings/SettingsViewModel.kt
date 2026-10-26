package com.example.finalafter.crnoui3.feature.settings

import com.example.finalafter.crnoui3.core.common.session.SessionController
import com.example.finalafter.crnoui3.core.datastore.settings.AppSettingsStore
import com.example.finalafter.crnoui3.core.mvi.MviViewModel
import com.example.finalafter.crnoui3.feature.settings.api.SettingsAppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Settings, reading and writing the store the rest of the app already uses. */
@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val settingsStore: AppSettingsStore,
    private val sessionController: SessionController,
    appInfo: SettingsAppInfo,
) : MviViewModel<SettingsState, SettingsEvent, SettingsEffect>(SettingsState(appVersion = appInfo.versionName)) {

    init {
        observe(settingsStore.settings) { copy(settings = it) }
    }

    // Each write sets an absolute value, so a newer tap cancelling an older write loses nothing.
    override fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeSelected -> launchLatest(THEME) { settingsStore.setThemeMode(event.mode) }

            is SettingsEvent.AnalyticsToggled -> launchLatest(ANALYTICS) { settingsStore.setAnalyticsEnabled(event.enabled) }

            is SettingsEvent.HapticsToggled -> launchLatest(HAPTICS) { settingsStore.setHapticsEnabled(event.enabled) }

            is SettingsEvent.AppLockToggled -> launchLatest(APP_LOCK) { settingsStore.setAppLockEnabled(event.enabled) }

            SettingsEvent.SignOutConfirmed -> sessionController.signOut()

            is SettingsEvent.LanguageRead -> setState { copy(languageTag = event.tag) }

            is SettingsEvent.LanguagePicked -> {
                setState { copy(languageTag = event.tag) }
                emit(SettingsEffect.ApplyLanguage(event.tag))
            }

            SettingsEvent.LicensesClicked -> emit(SettingsEffect.OpenLicenses)
        }
    }

    private companion object {
        const val THEME = "theme"
        const val ANALYTICS = "analytics"
        const val HAPTICS = "haptics"
        const val APP_LOCK = "appLock"
    }
}
