package com.base.app.feature.onboarding.intro

import androidx.compose.runtime.Composable
import com.base.app.core.ui.MviScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun OnboardingRoute(viewModel: OnboardingViewModel = koinViewModel()) {
    MviScreen(viewModel = viewModel) { state, onEvent ->
        OnboardingScreen(state = state, onEvent = onEvent)
    }
}
