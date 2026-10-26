package com.example.cmp.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.notification.permission.NotificationAccess
import com.example.cmp.core.notification.permission.rememberNotificationPermission
import com.example.cmp.core.ui.MviScreen

@Composable
internal fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    // Asked once, at the end, when the person knows what the app is for; whatever they answer,
    // onboarding finishes. Skip does not ask.
    val notifications = rememberNotificationPermission { viewModel.onEvent(OnboardingEvent.Completed) }
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
                if (event == OnboardingEvent.Completed && notifications.access == NotificationAccess.NotGranted) {
                    notifications.request()
                    return@OnboardingScreen
                }
                onEvent(event)
            },
        )
    }
}
