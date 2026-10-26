package com.example.cmp.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.ui.MviScreen
import com.example.cmp.core.ui.PlaceholderScreen

@Composable
internal fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                // The completed flag starts the app's back stack afresh, which AppRoot keys on who
                // the app is for; navigating as well would push onto the stack it replaces.
                OnboardingEffect.Finished -> Unit
            }
        },
    ) { _, _ ->
        PlaceholderScreen(title = R.string.onboarding_welcome)
    }
}
