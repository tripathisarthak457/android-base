package com.base.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import com.base.app.core.datastore.AppSettingsStore
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.list.AppPager
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.ui.MviScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One onboarding page. Replace the placeholder copy and icons with your own. */
@Immutable
/**
 * Resource ids rather than strings, because the list of pages is a top-level `val` and a composable
 * cannot be called from one.
 */
data class OnboardingPage(
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    val icon: ImageVector,
)

@Immutable
data class OnboardingState(
    val pages: List<OnboardingPage> = DefaultPages,
) : UiState

sealed interface OnboardingEvent : UiEvent {
    data object Completed : OnboardingEvent
    data object Skipped : OnboardingEvent
}

sealed interface OnboardingEffect : UiEffect {
    data object Finished : OnboardingEffect
}

/**
 * Onboarding, which is finished exactly once. Both "Get started" and "Skip" write the same flag and
 * emit the same effect.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsStore: AppSettingsStore,
) : MviViewModel<OnboardingState, OnboardingEvent, OnboardingEffect>(OnboardingState()) {

    override suspend fun handleEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.Completed, OnboardingEvent.Skipped -> {
                settingsStore.setOnboardingCompleted(true)
                emitEffect(OnboardingEffect.Finished)
            }
        }
    }
}

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                OnboardingEffect.Finished -> onFinished()
            }
        },
    ) { state, onEvent ->
        OnboardingScreen(state = state, onEvent = onEvent)
    }
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { state.pages.size })
    val scope = rememberCoroutineScope()
    val onLastPage = pagerState.currentPage == state.pages.lastIndex

    AppScaffold(modifier = modifier) {
        // No top or bottom bar here, so this screen applies the system-bar insets itself.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(AppTheme.spacing.gutter),
        ) {
            // Skip stays visible on every page including the last, where it is the same action as
            // "Get started". Hiding it at the end makes the button jump around as the user pages.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                AppButton(
                    text = stringResource(R.string.onboarding_skip),
                    onClick = { onEvent(OnboardingEvent.Skipped) },
                    variant = ButtonVariant.Ghost,
                )
            }

            AppPager(
                pageCount = state.pages.size,
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { index ->
                val page = state.pages[index]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppTheme.spacing.lg)
                        .padding(bottom = AppTheme.spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        AppTheme.spacing.lg,
                        Alignment.CenterVertically,
                    ),
                ) {
                    AppSurface(
                        modifier = Modifier.size(96.dp),
                        shape = AppTheme.shapes.pill,
                        color = AppTheme.colors.accentSubtle,
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AppIcon(
                                page.icon,
                                contentDescription = null,
                                tint = AppTheme.colors.accent,
                                size = 40.dp,
                            )
                        }
                    }
                    AppText(
                        text = stringResource(page.title),
                        style = AppTheme.typography.displaySmall,
                        color = AppTheme.colors.contentPrimary,
                        textAlign = TextAlign.Center,
                    )
                    AppText(
                        text = stringResource(page.body),
                        style = AppTheme.typography.bodyLarge,
                        color = AppTheme.colors.contentTertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            AppButton(
                text = if (onLastPage) "Get started" else "Next",
                onClick = {
                    if (onLastPage) {
                        onEvent(OnboardingEvent.Completed)
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                fillWidth = true,
                modifier = Modifier.padding(vertical = AppTheme.spacing.lg),
            )
        }
    }
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

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    AppTheme {
        OnboardingScreen(state = OnboardingState(), onEvent = {})
    }
}
