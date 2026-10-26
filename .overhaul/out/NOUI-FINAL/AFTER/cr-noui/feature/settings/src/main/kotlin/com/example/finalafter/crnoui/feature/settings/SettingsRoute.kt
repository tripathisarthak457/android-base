package com.example.finalafter.crnoui.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crnoui.core.navigation.AppNavigator
import com.example.finalafter.crnoui.core.ui.MviScreen
import com.example.finalafter.crnoui.core.ui.PlaceholderScreen
import com.example.finalafter.crnoui.core.ui.locale.AppLocales
import com.example.finalafter.crnoui.feature.settings.api.LicensesKey

@Composable
internal fun SettingsRoute(
    navigator: AppNavigator,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.onEvent(SettingsEvent.LanguageRead(AppLocales.current(context))) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is SettingsEffect.ApplyLanguage -> AppLocales.set(context, effect.tag)
                SettingsEffect.OpenLicenses -> navigator.navigate(LicensesKey)
            }
        },
    ) { _, _ ->
        PlaceholderScreen(title = R.string.settings_title)
    }
}
