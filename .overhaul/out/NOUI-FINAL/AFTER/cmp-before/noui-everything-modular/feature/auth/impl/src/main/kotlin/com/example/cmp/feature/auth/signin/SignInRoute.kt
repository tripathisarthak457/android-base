package com.example.cmp.feature.auth.signin

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.ui.MviScreen
import com.example.cmp.core.ui.PlaceholderScreen
import com.example.cmp.feature.auth.R
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
                // The new session starts the app's back stack afresh, which AppRoot keys on who the
                // app is for; navigating as well would push onto the stack it replaces.
                SignInEffect.SignedIn -> Unit
                SignInEffect.OpenSignUp -> navigator.navigate(SignUpKey)
                SignInEffect.OpenPasswordReset -> navigator.navigate(PasswordResetKey)
            }
        },
    ) { _, _ ->
        PlaceholderScreen(title = R.string.auth_sign_in)
    }
}
