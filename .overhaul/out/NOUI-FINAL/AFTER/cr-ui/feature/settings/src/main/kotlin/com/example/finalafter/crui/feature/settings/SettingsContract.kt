package com.example.finalafter.crui.feature.settings

import androidx.compose.runtime.Immutable
import com.example.finalafter.crui.core.datastore.settings.AppSettings
import com.example.finalafter.crui.core.mvi.UiEffect
import com.example.finalafter.crui.core.mvi.UiEvent
import com.example.finalafter.crui.core.mvi.UiState

@Immutable
internal data class SettingsState(
    val appVersion: String,
    val settings: AppSettings = AppSettings(),
    /** Null while the app follows the phone's language. */
    val languageTag: String? = null,
) : UiState

internal sealed interface SettingsEvent : UiEvent {
    /** [mode] is one of the `AppSettings.THEME_*` values. */
    data class ThemeSelected(val mode: String) : SettingsEvent
    data class AnalyticsToggled(val enabled: Boolean) : SettingsEvent
    data class HapticsToggled(val enabled: Boolean) : SettingsEvent

    data class AppLockToggled(val enabled: Boolean) : SettingsEvent

    data object SignOutConfirmed : SettingsEvent

    /** The language lives in the platform rather than the store, so the route reads it. */
    data class LanguageRead(val tag: String?) : SettingsEvent
    data class LanguagePicked(val tag: String?) : SettingsEvent

    data object LicensesClicked : SettingsEvent
}

internal sealed interface SettingsEffect : UiEffect {
    data class ApplyLanguage(val tag: String?) : SettingsEffect

    data object OpenLicenses : SettingsEffect
}
