package com.example.finalafter.crnoui3.feature.auth.passwordreset

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crnoui3.core.navigation.AppNavigator
import com.example.finalafter.crnoui3.core.ui.MviScreen
import com.example.finalafter.crnoui3.core.ui.PlaceholderScreen
import com.example.finalafter.crnoui3.feature.auth.R

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
    ) { _, _ ->
        PlaceholderScreen(title = R.string.auth_reset_password)
    }
}
