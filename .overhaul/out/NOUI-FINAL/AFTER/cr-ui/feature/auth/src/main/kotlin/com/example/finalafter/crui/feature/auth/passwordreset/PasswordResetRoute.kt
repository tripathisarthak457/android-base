package com.example.finalafter.crui.feature.auth.passwordreset

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crui.core.navigation.AppNavigator
import com.example.finalafter.crui.core.ui.MviScreen

@Composable
internal fun PasswordResetRoute(
    navigator: AppNavigator,
    viewModel: PasswordResetViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                PasswordResetEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        PasswordResetScreen(state = state, email = viewModel.email, onEvent = onEvent)
    }
}
