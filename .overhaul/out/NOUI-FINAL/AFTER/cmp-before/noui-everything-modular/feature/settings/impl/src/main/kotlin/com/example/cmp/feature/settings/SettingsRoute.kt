package com.example.cmp.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.ui.MviScreen
import com.example.cmp.core.ui.PlaceholderScreen
import com.example.cmp.core.ui.locale.AppLocales
import com.example.cmp.feature.settings.api.LicensesKey

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
