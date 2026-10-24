package com.base.app.feature.onboarding.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.adaptive.AppTwoPane
import com.base.app.core.designsystem.foundation.adaptive.centeredMaxWidth
import com.base.app.core.designsystem.foundation.adaptive.shouldUseTwoPanes
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.feature.onboarding.resources.Res
import com.base.app.feature.onboarding.resources.onboarding_get_started
import com.base.app.feature.onboarding.resources.onboarding_next
import com.base.app.feature.onboarding.resources.onboarding_skip
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .safeDrawingPadding()
            .padding(horizontal = AppTheme.spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Skip stays on every page, the last included, where it does what "Get started" does:
        // hiding it at the end would move the buttons as the person pages.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppButton(
                text = stringResource(Res.string.onboarding_skip),
                onClick = { onEvent(OnboardingEvent.Skipped) },
                variant = ButtonVariant.Plain,
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            PageContent(page = state.pages[index], sideBySide = sideBySide, modifier = Modifier.fillMaxSize())
        }

        PageDots(count = state.pages.size, current = pagerState.currentPage)

        AppButton(
            text = stringResource(if (onLastPage) Res.string.onboarding_get_started else Res.string.onboarding_next),
            onClick = {
                if (onLastPage) {
                    onEvent(OnboardingEvent.Completed)
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .centeredMaxWidth(AppTheme.layout.formMaxWidth)
                .padding(vertical = AppTheme.spacing.lg),
        )
    }
}

/**
 * One page: the picture above the words on a phone, beside them when the window is wide or short,
 * since a phone held sideways has no height to stack them in.
 */
@Composable
private fun PageContent(page: OnboardingPage, sideBySide: Boolean, modifier: Modifier = Modifier) {
    val textAlign = if (sideBySide) TextAlign.Start else TextAlign.Center
    Box(modifier = modifier.padding(horizontal = AppTheme.spacing.lg), contentAlignment = Alignment.Center) {
        AppTwoPane(
            sideBySide = sideBySide,
            modifier = Modifier.widthIn(max = AppTheme.layout.contentMaxWidth),
            first = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Illustration(page) }
            },
            second = { PageText(page, textAlign) },
        )
    }
}

/** The page's picture: an icon on an accent disc, until a project puts its own artwork here. */
@Composable
private fun Illustration(page: OnboardingPage) {
    Box(
        modifier = Modifier
            .size(IllustrationSize)
            .background(AppTheme.colors.accentSubtle, AppTheme.shapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(page.icon, contentDescription = null, tint = AppTheme.colors.accent, size = AppTheme.sizes.iconLarge)
    }
}

@Composable
private fun PageText(page: OnboardingPage, textAlign: TextAlign, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.widthIn(max = AppTheme.layout.readableMaxWidth),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppText(
            text = stringResource(page.title),
            modifier = Modifier.fillMaxWidth(),
            style = AppTheme.typography.title1,
            color = AppTheme.colors.contentPrimary,
            textAlign = textAlign,
        )
        AppText(
            text = stringResource(page.body),
            modifier = Modifier.fillMaxWidth(),
            style = AppTheme.typography.body,
            color = AppTheme.colors.contentSecondary,
            textAlign = textAlign,
        )
    }
}

/** Where the person is in the pages. Decorative: the pager announces its own position. */
@Composable
private fun PageDots(count: Int, current: Int) {
    Row(
        modifier = Modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(AppTheme.spacing.sm)
                    .background(
                        color = if (index == current) AppTheme.colors.accent else AppTheme.colors.fill,
                        shape = AppTheme.shapes.pill,
                    ),
            )
        }
    }
}

private val IllustrationSize = 96.dp

@Preview
@Composable
private fun OnboardingPreview() {
    AppTheme {
        OnboardingScreen(state = OnboardingState(), onEvent = {})
    }
}
