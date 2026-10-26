package com.base.app.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
// <opt:push&ui_components>
import com.base.app.core.notification.permission.NotificationAccess
import com.base.app.core.notification.permission.rememberNotificationPermission
// </opt:push&ui_components>
import com.base.app.core.ui.MviScreen
// <opt:!ui_components>import com.base.app.core.ui.PlaceholderScreen

@Composable
internal fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    // <opt:push&ui_components>
    // Asked once, at the end, when the person knows what the app is for; whatever they answer,
    // onboarding finishes. Skip does not ask.
    val notifications =
        rememberNotificationPermission { viewModel.onEvent(OnboardingEvent.Completed) }
    // </opt:push&ui_components>
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                // <opt:ui_components>
                // The completed flag moves the app on to sign-in or the tabs; navigating as well
                // would open that screen twice, once in each host.
                // </opt:ui_components>
                // <opt-else:ui_components>
                //~ // The completed flag starts the app's back stack afresh, which AppRoot keys on who
                //~ // the app is for; navigating as well would push onto the stack it replaces.
                // </opt-else:ui_components>
                OnboardingEffect.Finished -> Unit
            }
        },
    // <opt:ui_components>
    ) { state, onEvent ->
        OnboardingScreen(
            state = state,
            onEvent = { event ->
                // <opt:push>
                if (event == OnboardingEvent.Completed &&
                    notifications.access == NotificationAccess.NotGranted
                ) {
                    notifications.request()
                    return@OnboardingScreen
                }
                // </opt:push>
                onEvent(event)
            },
        )
    }
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ ) { _, _ ->
    //~     PlaceholderScreen(title = R.string.onboarding_welcome)
    //~ }
    // </opt-else:ui_components>
}
