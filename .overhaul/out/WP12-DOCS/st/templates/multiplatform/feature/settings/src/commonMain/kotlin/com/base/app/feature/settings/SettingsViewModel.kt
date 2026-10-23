package com.base.app.feature.settings

import com.base.app.core.datastore.settings.AppSettingsStore
import com.base.app.core.mvi.MviViewModel
import com.base.app.feature.settings.api.SettingsAppInfo

/** Settings, reading and writing the store the rest of the app already follows. */
internal class SettingsViewModel(
    private val settingsStore: AppSettingsStore,
    appInfo: SettingsAppInfo,
) : MviViewModel<SettingsState, SettingsEvent, SettingsEffect>(SettingsState(appVersion = appInfo.versionName)) {

    init {
        observe(settingsStore.settings) { copy(themeMode = it.themeMode) }
    }

    override fun handleEvent(event: SettingsEvent) {
        when (event) {
            // An absolute value, so a newer tap cancelling an older write loses nothing.
            is SettingsEvent.ThemeSelected -> launchLatest(THEME) { settingsStore.setThemeMode(event.mode) }
            // <opt:language>
            SettingsEvent.LanguageClicked -> emit(SettingsEffect.OpenLanguageSettings)
            // </opt:language>
        }
    }

    private companion object {
        const val THEME = "theme"
    }
}
