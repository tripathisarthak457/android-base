package com.base.app.feature.settings

import androidx.compose.runtime.Immutable
import com.base.app.core.datastore.settings.AppSettings
import com.base.app.core.mvi.UiEffect
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState

@Immutable
internal data class SettingsState(
    val appVersion: String,
    val settings: AppSettings = AppSettings(),
    // <opt:language>
    /** Null while the app follows the phone's language. */
    val languageTag: String? = null,
    // </opt:language>
) : UiState

internal sealed interface SettingsEvent : UiEvent {
    /** [mode] is one of the `AppSettings.THEME_*` values. */
    data class ThemeSelected(val mode: String) : SettingsEvent
    data class AnalyticsToggled(val enabled: Boolean) : SettingsEvent
    data class HapticsToggled(val enabled: Boolean) : SettingsEvent

    // <opt:applock>
    data class AppLockToggled(val enabled: Boolean) : SettingsEvent
    // </opt:applock>

    data object SignOutConfirmed : SettingsEvent

    // <opt:language>
    /** The language lives in the platform rather than the store, so the route reads it. */
    data class LanguageRead(val tag: String?) : SettingsEvent
    data class LanguagePicked(val tag: String?) : SettingsEvent
    // </opt:language>

    // <opt:licenses>
    data object LicensesClicked : SettingsEvent
    // </opt:licenses>
}

internal sealed interface SettingsEffect : UiEffect {
    // <opt:language>
    data class ApplyLanguage(val tag: String?) : SettingsEffect
    // </opt:language>

    // <opt:licenses>
    data object OpenLicenses : SettingsEffect
    // </opt:licenses>
}
