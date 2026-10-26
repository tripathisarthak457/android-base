package com.example.finalafter.crui2.feature.auth.signin

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crui2.core.navigation.AppNavigator
import com.example.finalafter.crui2.core.ui.MviScreen
import com.example.finalafter.crui2.feature.auth.api.PasswordResetKey
import com.example.finalafter.crui2.feature.auth.api.SignUpKey

@Composable
internal fun SignInRoute(
    navigator: AppNavigator,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                // The new session moves the app to the signed-in shell; navigating as well would open
                // the next screen twice, once in each host.
                SignInEffect.SignedIn -> Unit
                SignInEffect.OpenSignUp -> navigator.navigate(SignUpKey)
                SignInEffect.OpenPasswordReset -> navigator.navigate(PasswordResetKey)
            }
        },
    ) { state, onEvent ->
        SignInScreen(state = state, email = viewModel.email, password = viewModel.password, onEvent = onEvent)
    }
}
