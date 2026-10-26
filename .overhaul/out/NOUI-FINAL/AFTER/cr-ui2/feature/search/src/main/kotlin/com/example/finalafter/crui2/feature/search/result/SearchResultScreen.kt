package com.example.finalafter.crui2.feature.search.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.finalafter.crui2.core.common.LoadState
import com.example.finalafter.crui2.core.common.result.AppError
import com.example.finalafter.crui2.core.designsystem.R as DesignSystemR
import com.example.finalafter.crui2.core.designsystem.component.container.AppScaffold
import com.example.finalafter.crui2.core.designsystem.component.feedback.AppRedacted
import com.example.finalafter.crui2.core.designsystem.component.feedback.AppStateView
import com.example.finalafter.crui2.core.designsystem.component.feedback.StateKind
import com.example.finalafter.crui2.core.designsystem.component.navigation.AppTopBar
import com.example.finalafter.crui2.core.designsystem.component.text.AppText
import com.example.finalafter.crui2.core.designsystem.foundation.layout.ReportScrollEdge
import com.example.finalafter.crui2.core.designsystem.foundation.layout.chromeContentPadding
import com.example.finalafter.crui2.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.finalafter.crui2.core.designsystem.preview.AppScreenPreviews
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import com.example.finalafter.crui2.core.ui.text
import com.example.finalafter.crui2.data.search.api.model.SearchResult
import com.example.finalafter.crui2.feature.search.R

/** One result: its title and its snippet, or why it could not be loaded, with a way back to the list. */
@Composable
internal fun SearchResultScreen(
    state: SearchResultState,
    onEvent: (SearchResultEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.search_title),
                onBack = { onEvent(SearchResultEvent.BackClicked) },
            )
        },
    ) {
        val scroll = rememberScrollState()
        ReportScrollEdge(scroll)
        when (val loadState = state.loadState) {
            is LoadState.Error -> AppStateView(
                message = loadState.error.text(),
                kind = if (loadState.error == AppError.Offline) StateKind.Offline else StateKind.Error,
                onAction = { onEvent(SearchResultEvent.Retry) },
                actionLabel = stringResource(R.string.search_retry),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll, flingBehavior = AppScrollDefaults.flingBehavior())
                    .padding(chromeContentPadding(PaddingValues(AppTheme.spacing.gutter))),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                val result = state.result
                // The same text before and after the result arrives, so it lands where its
                // placeholder was and nothing around it moves.
                AppRedacted(
                    active = result == null,
                    contentDescription = stringResource(DesignSystemR.string.designsystem_loading),
                ) {
                    val shown = result ?: ResultPlaceholder
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                        AppText(
                            text = shown.title,
                            style = AppTheme.typography.headline,
                            color = AppTheme.colors.contentPrimary,
                        )
                        AppText(
                            text = shown.snippet,
                            style = AppTheme.typography.body,
                            color = AppTheme.colors.contentSecondary,
                        )
                    }
                }
            }
        }
    }
}

// Never shown (see AppRedacted). A one-line title and a snippet of about three lines, as results have.
private val ResultPlaceholder = SearchResult(
    -1,
    "Placeholder result title",
    "A result's snippet runs to about three lines on a phone, and this placeholder is about as long, " +
        "so its bones take close to the room the real snippet will.",
)

@AppScreenPreviews
@Composable
private fun SearchResultLoadingPreview() {
    SearchResultScreen(state = SearchResultState(loadState = LoadState.Loading), onEvent = {})
}
