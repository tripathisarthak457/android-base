package com.base.app.feature.settings

import androidx.compose.runtime.Composable
// <opt:language>
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
// </opt:language>
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
// <opt:!ui_components>import com.base.app.core.ui.PlaceholderScreen
// <opt:language>
import com.base.app.core.ui.locale.AppLocales
// </opt:language>
// <opt:licenses>
import com.base.app.feature.settings.api.LicensesKey
// </opt:licenses>

@Composable
internal fun SettingsRoute(
    navigator: AppNavigator,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    // <opt:language>
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.onEvent(SettingsEvent.LanguageRead(AppLocales.current(context)))
    }
    // </opt:language>

    MviScreen(
        viewModel = viewModel,
        // <opt:language|licenses>
        onEffect = { effect ->
            when (effect) {
                // <opt:language>
                is SettingsEffect.ApplyLanguage -> AppLocales.set(context, effect.tag)
                // </opt:language>
                // <opt:licenses>
                SettingsEffect.OpenLicenses -> navigator.navigate(LicensesKey)
                // </opt:licenses>
            }
        },
        // </opt:language|licenses>
    // <opt:ui_components>
    ) { state, onEvent ->
        SettingsScreen(state = state, onEvent = onEvent)
    }
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ ) { _, _ ->
    //~     PlaceholderScreen(title = R.string.settings_title)
    //~ }
    // </opt-else:ui_components>
}
