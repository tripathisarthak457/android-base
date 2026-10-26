package com.example.cmp.feature.auth.signin

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.ui.MviScreen
import com.example.cmp.feature.auth.api.PasswordResetKey
import com.example.cmp.feature.auth.api.SignUpKey

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
