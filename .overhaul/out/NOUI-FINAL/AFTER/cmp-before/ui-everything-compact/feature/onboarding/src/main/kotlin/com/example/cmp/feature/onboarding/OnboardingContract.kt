package com.example.cmp.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cmp.core.designsystem.icon.AppIcons
import com.example.cmp.core.mvi.UiEffect
import com.example.cmp.core.mvi.UiEvent
import com.example.cmp.core.mvi.UiState

/**
 * One onboarding page. Replace the placeholder copy and icons with your own. Resource ids rather
 * than strings, because the list of pages is a top-level `val` and a composable cannot be called
 * from one.
 */
@Immutable
internal data class OnboardingPage(
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    val icon: ImageVector,
)

@Immutable
internal data class OnboardingState(
    val pages: List<OnboardingPage> = DefaultPages,
) : UiState

internal sealed interface OnboardingEvent : UiEvent {
    data object Completed : OnboardingEvent
    data object Skipped : OnboardingEvent
}

internal sealed interface OnboardingEffect : UiEffect {
    data object Finished : OnboardingEffect
}

/** Placeholder copy. Replace it; the structure is the part worth keeping. */
private val DefaultPages = listOf(
    OnboardingPage(
        title = R.string.onboarding_welcome,
        body = R.string.onboarding_body_what_it_does,
        icon = AppIcons.Home,
    ),
    OnboardingPage(
        title = R.string.onboarding_stay_in_the_loop,
        body = R.string.onboarding_body_benefit,
        icon = AppIcons.Bell,
    ),
    OnboardingPage(
        title = R.string.onboarding_your_data_is_yours,
        body = R.string.onboarding_body_reassurance,
        icon = AppIcons.Lock,
    ),
)
