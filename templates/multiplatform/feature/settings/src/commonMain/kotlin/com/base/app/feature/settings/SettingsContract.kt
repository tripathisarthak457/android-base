package com.base.app.feature.settings

import androidx.compose.runtime.Immutable
import com.base.app.core.datastore.settings.AppSettings
import com.base.app.core.mvi.UiEffect
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState

@Immutable
internal data class SettingsState(
    val themeMode: String = AppSettings.THEME_SYSTEM,
    val appVersion: String = "",
) : UiState

internal sealed interface SettingsEvent : UiEvent {
    data class ThemeSelected(val mode: String) : SettingsEvent
    // <opt:language>
    data object LanguageClicked : SettingsEvent
    // </opt:language>
}

internal sealed interface SettingsEffect : UiEffect {
    // <opt:language>
    data object OpenLanguageSettings : SettingsEffect
    // </opt:language>
}
