package com.example.cmp.feature.onboarding

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cmp.core.designsystem.component.button.AppButton
import com.example.cmp.core.designsystem.component.button.ButtonVariant
import com.example.cmp.core.designsystem.component.container.AppScaffold
import com.example.cmp.core.designsystem.component.navigation.AppPager
import com.example.cmp.core.designsystem.component.text.AppIcon
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.foundation.adaptive.AppTwoPane
import com.example.cmp.core.designsystem.foundation.adaptive.centeredMaxWidth
import com.example.cmp.core.designsystem.foundation.adaptive.shouldUseTwoPanes
import com.example.cmp.core.designsystem.foundation.content.AppSurface
import com.example.cmp.core.designsystem.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
internal fun OnboardingScreen(
    state: OnboardingState,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { state.pages.size })
    val scope = rememberCoroutineScope()
    val onLastPage = pagerState.currentPage == state.pages.lastIndex
    val sideBySide = shouldUseTwoPanes

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
                    variant = ButtonVariant.Plain,
                )
            }

            AppPager(
                pageCount = state.pages.size,
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { index ->
                OnboardingPageContent(
                    page = state.pages[index],
                    sideBySide = sideBySide,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppTheme.spacing.lg)
                        .padding(bottom = AppTheme.spacing.xl),
                )
            }

            AppButton(
                text = stringResource(if (onLastPage) R.string.onboarding_get_started else R.string.onboarding_next),
                onClick = {
                    if (onLastPage) {
                        onEvent(OnboardingEvent.Completed)
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .centeredMaxWidth(AppTheme.layout.formMaxWidth)
                    .padding(vertical = AppTheme.spacing.lg)
                    .fillMaxWidth(),
            )
        }
    }
}

/**
 * One page: the picture above the words on a phone, beside them when the window is wide or short —
 * a phone held sideways has no height to stack them in.
 */
@Composable
private fun OnboardingPageContent(page: OnboardingPage, sideBySide: Boolean, modifier: Modifier = Modifier) {
    val textAlign = if (sideBySide) TextAlign.Start else TextAlign.Center
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AppTwoPane(
            sideBySide = sideBySide,
            modifier = Modifier.widthIn(max = AppTheme.layout.contentMaxWidth),
            first = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AppSurface(
                        modifier = Modifier.size(if (sideBySide) 160.dp else 96.dp),
                        shape = AppTheme.shapes.pill,
                        color = AppTheme.colors.accentSubtle,
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AppIcon(
                                page.icon,
                                contentDescription = null,
                                tint = AppTheme.colors.accent,
                                size = if (sideBySide) 64.dp else 40.dp,
                            )
                        }
                    }
                }
            },
            second = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (sideBySide) Alignment.Start else Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    AppText(
                        text = stringResource(page.title),
                        style = AppTheme.typography.title2,
                        color = AppTheme.colors.contentPrimary,
                        textAlign = textAlign,
                    )
                    AppText(
                        text = stringResource(page.body),
                        modifier = Modifier.widthIn(max = AppTheme.layout.readableMaxWidth),
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.contentTertiary,
                        textAlign = textAlign,
                    )
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    AppTheme {
        OnboardingScreen(state = OnboardingState(), onEvent = {})
    }
}
