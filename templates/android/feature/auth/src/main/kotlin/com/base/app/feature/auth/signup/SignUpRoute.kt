package com.base.app.feature.auth.signup

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
// <opt:!ui_components>import com.base.app.core.ui.PlaceholderScreen
// <opt:!ui_components>import com.base.app.feature.auth.R

@Composable
internal fun SignUpRoute(navigator: AppNavigator, viewModel: SignUpViewModel = hiltViewModel()) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                // As with sign-in, the new session itself moves the app on.
                SignUpEffect.SignedUp -> Unit

                SignUpEffect.NavigateBack -> navigator.navigateUp()
            }
        },
        // <opt:ui_components>
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
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ ) { _, _ ->
    //~     PlaceholderScreen(title = R.string.auth_create_account)
    //~ }
    // </opt-else:ui_components>
}
