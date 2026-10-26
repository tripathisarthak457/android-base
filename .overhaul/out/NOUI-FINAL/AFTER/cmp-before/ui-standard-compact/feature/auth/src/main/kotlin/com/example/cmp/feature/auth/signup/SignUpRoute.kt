package com.example.cmp.feature.auth.signup

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.ui.MviScreen

@Composable
internal fun SignUpRoute(
    navigator: AppNavigator,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                // As with sign-in, the new session itself moves the app on.
                SignUpEffect.SignedUp -> Unit
                SignUpEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        SignUpScreen(
            state = state,
            name = viewModel.name,
            email = viewModel.email,
            password = viewModel.password,
            confirm = viewModel.confirm,
            onEvent = onEvent,
        )
    }
}
