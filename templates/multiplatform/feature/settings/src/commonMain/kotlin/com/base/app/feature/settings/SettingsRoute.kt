package com.base.app.feature.settings

import androidx.compose.runtime.Composable
import com.base.app.core.ui.MviScreen
import org.koin.compose.viewmodel.koinViewModel
// <opt:language>
import com.base.app.feature.settings.language.rememberLanguageSettings
// </opt:language>

@Composable
internal fun SettingsRoute(viewModel: SettingsViewModel = koinViewModel()) {
    // <opt:language>
    val openLanguageSettings = rememberLanguageSettings()
    // </opt:language>
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                // <opt:language>
                SettingsEffect.OpenLanguageSettings -> openLanguageSettings?.invoke()
                // </opt:language>
            }
        },
    ) { state, onEvent ->
        SettingsScreen(
            state = state,
            onEvent = onEvent,
            // <opt:language>
            languageSettingsAvailable = openLanguageSettings != null,
            // </opt:language>
        )
    }
}
