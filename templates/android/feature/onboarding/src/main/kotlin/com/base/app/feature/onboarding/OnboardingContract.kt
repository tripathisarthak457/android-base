package com.base.app.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
// <opt:ui_components>
import androidx.compose.ui.graphics.vector.ImageVector
import com.base.app.core.designsystem.icon.AppIcons
// </opt:ui_components>
import com.base.app.core.mvi.UiEffect
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState

/**
 * One onboarding page. Replace the placeholder copy and icons with your own. Resource ids rather
 * than strings, because the list of pages is a top-level `val` and a composable cannot be called
 * from one.
 */
@Immutable
// <opt:ui_components>
internal data class OnboardingPage(
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    val icon: ImageVector,
)
// </opt:ui_components>
// <opt-else:ui_components>
//~ internal data class OnboardingPage(@param:StringRes val title: Int, @param:StringRes val body: Int)
// </opt-else:ui_components>

@Immutable
internal data class OnboardingState(val pages: List<OnboardingPage> = DefaultPages) : UiState

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
        // <opt:ui_components>
        icon = AppIcons.Home,
        // </opt:ui_components>
    ),
    OnboardingPage(
        title = R.string.onboarding_stay_in_the_loop,
        body = R.string.onboarding_body_benefit,
        // <opt:ui_components>
        icon = AppIcons.Bell,
        // </opt:ui_components>
    ),
    OnboardingPage(
        title = R.string.onboarding_your_data_is_yours,
        body = R.string.onboarding_body_reassurance,
        // <opt:ui_components>
        icon = AppIcons.Lock,
        // </opt:ui_components>
    ),
)
