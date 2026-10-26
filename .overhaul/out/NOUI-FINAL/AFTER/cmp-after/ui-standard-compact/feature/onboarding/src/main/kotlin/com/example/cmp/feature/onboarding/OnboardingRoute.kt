package com.example.cmp.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.ui.MviScreen

@Composable
internal fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                // The completed flag moves the app on to sign-in or the tabs; navigating as well
                // would open that screen twice, once in each host.
                OnboardingEffect.Finished -> Unit
            }
        },
    ) { state, onEvent ->
        OnboardingScreen(
            state = state,
            onEvent = { event ->
                onEvent(event)
            },
        )
    }
}
