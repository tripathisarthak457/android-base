package com.example.finalafter.crui2.feature.settings.licenses

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crui2.core.navigation.AppNavigator
import com.example.finalafter.crui2.core.ui.MviScreen

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
    ) { state, onEvent ->
        LicensesScreen(state = state, onEvent = onEvent)
    }
}
