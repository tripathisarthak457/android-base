package com.example.finalafter.crnoui.feature.auth.signin

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crnoui.core.navigation.AppNavigator
import com.example.finalafter.crnoui.core.ui.MviScreen
import com.example.finalafter.crnoui.core.ui.PlaceholderScreen
import com.example.finalafter.crnoui.feature.auth.R
import com.example.finalafter.crnoui.feature.auth.api.PasswordResetKey
import com.example.finalafter.crnoui.feature.auth.api.SignUpKey

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
    ) { _, _ ->
        PlaceholderScreen(title = R.string.auth_sign_in)
    }
}
