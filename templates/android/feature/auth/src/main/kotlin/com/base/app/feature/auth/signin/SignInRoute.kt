package com.base.app.feature.auth.signin

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
// <opt:!ui_components>import com.base.app.core.ui.PlaceholderScreen
// <opt:!ui_components>import com.base.app.feature.auth.R
import com.base.app.feature.auth.api.PasswordResetKey
import com.base.app.feature.auth.api.SignUpKey

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
    // <opt:ui_components>
    ) { state, onEvent ->
        SignInScreen(state = state, email = viewModel.email, password = viewModel.password, onEvent = onEvent)
    }
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ ) { _, _ ->
    //~     PlaceholderScreen(title = R.string.auth_sign_in)
    //~ }
    // </opt-else:ui_components>
}
