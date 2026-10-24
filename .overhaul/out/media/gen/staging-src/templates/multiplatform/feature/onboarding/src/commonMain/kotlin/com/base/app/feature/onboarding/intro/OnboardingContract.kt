package com.base.app.feature.onboarding.intro

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState
import com.base.app.feature.onboarding.resources.Res
import com.base.app.feature.onboarding.resources.onboarding_body_benefit
import com.base.app.feature.onboarding.resources.onboarding_body_reassurance
import com.base.app.feature.onboarding.resources.onboarding_body_what_it_does
import com.base.app.feature.onboarding.resources.onboarding_stay_in_the_loop
import com.base.app.feature.onboarding.resources.onboarding_welcome
import com.base.app.feature.onboarding.resources.onboarding_your_data_is_yours
import org.jetbrains.compose.resources.StringResource

/** One onboarding page. Replace the placeholder copy and icons with your own. */
@Immutable
internal data class OnboardingPage(
    val title: StringResource,
    val body: StringResource,
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

/** Placeholder copy. Replace it; the structure is the part worth keeping. */
private val DefaultPages = listOf(
    OnboardingPage(Res.string.onboarding_welcome, Res.string.onboarding_body_what_it_does, AppIcons.Home),
    OnboardingPage(Res.string.onboarding_stay_in_the_loop, Res.string.onboarding_body_benefit, AppIcons.Bell),
    OnboardingPage(Res.string.onboarding_your_data_is_yours, Res.string.onboarding_body_reassurance, AppIcons.Lock),
)
