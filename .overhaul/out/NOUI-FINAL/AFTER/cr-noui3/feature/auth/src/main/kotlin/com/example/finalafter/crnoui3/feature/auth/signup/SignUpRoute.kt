package com.example.finalafter.crnoui3.feature.auth.signup

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crnoui3.core.navigation.AppNavigator
import com.example.finalafter.crnoui3.core.ui.MviScreen
import com.example.finalafter.crnoui3.core.ui.PlaceholderScreen
import com.example.finalafter.crnoui3.feature.auth.R

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
    ) { _, _ ->
        PlaceholderScreen(title = R.string.auth_create_account)
    }
}
