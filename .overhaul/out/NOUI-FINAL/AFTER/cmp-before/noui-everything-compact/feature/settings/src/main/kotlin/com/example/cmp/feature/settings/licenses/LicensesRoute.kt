package com.example.cmp.feature.settings.licenses

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.ui.MviScreen
import com.example.cmp.core.ui.PlaceholderScreen
import com.example.cmp.feature.settings.R

@Composable
internal fun LicensesRoute(
    navigator: AppNavigator,
    viewModel: LicensesViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                LicensesEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { _, _ ->
        PlaceholderScreen(title = R.string.settings_open_source_licences)
    }
}
