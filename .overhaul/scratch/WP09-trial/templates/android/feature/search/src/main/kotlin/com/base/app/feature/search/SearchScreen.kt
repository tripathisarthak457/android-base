package com.base.app.feature.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.common.mvi.LoadState
import com.base.app.core.designsystem.animation.appAnimateItem
import com.base.app.core.designsystem.animation.rememberAppTransitions
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppEmptyState
import com.base.app.core.designsystem.component.feedback.AppErrorState
import com.base.app.core.designsystem.component.feedback.AppLinearProgress
import com.base.app.core.designsystem.component.feedback.AppSkeletonListItem
import com.base.app.core.designsystem.component.input.AppSearchField
import com.base.app.core.designsystem.component.navigation.AppLargeTitle
import com.base.app.core.designsystem.component.selection.AppChip
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.ui.MviScreen
import com.base.app.core.ui.asString
import com.base.app.data.search.SearchResult

@Composable
fun SearchRoute(
    onOpenResult: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is SearchEffect.OpenResult -> onOpenResult(effect.id)
            }
        },
    ) { state, onEvent ->
        SearchScreen(state = state, onEvent = onEvent)
    }
}

@Composable
fun SearchScreen(
    state: SearchState,
    onEvent: (SearchEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            Column {
                AppLargeTitle(title = stringResource(R.string.search_title))
                AppSearchField(
                    value = state.query,
                    onValueChange = { onEvent(SearchEvent.QueryChanged(it)) },
                    placeholder = stringResource(R.string.search_placeholder),
                    onSearch = { onEvent(SearchEvent.Submit) },
                    modifier = Modifier.padding(
                        horizontal = AppTheme.spacing.gutter,
                        vertical = AppTheme.spacing.sm,
                    ),
                )
                // A thin line so results stay readable while loading; its space is always reserved.
                Box(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                    if (state.loadState is LoadState.Refreshing) {
                        AppLinearProgress(modifier = Modifier.fillMaxWidth(), height = 2.dp)
                    }
                }
            }
        },
    ) {
        val transitions = rememberAppTransitions()
        AnimatedContent(
            targetState = state.contentKind(),
            transitionSpec = { transitions.fadeIn togetherWith transitions.fadeOut },
            label = "searchContent",
        ) { kind ->
            when (kind) {
                ContentKind.Start -> StartContent(state.recent, onEvent)
                ContentKind.Loading -> LoadingResults()
                ContentKind.Error -> (state.loadState as? LoadState.Error)?.let {
                    AppErrorState(
                        message = it.message.asString(),
                        isOffline = it.isOffline,
                        onRetry = { onEvent(SearchEvent.Retry) },
                        retryLabel = stringResource(R.string.search_retry),
                    )
                }

                ContentKind.Empty -> AppEmptyState(
                    title = stringResource(R.string.search_no_results_title),
                    message = stringResource(R.string.search_no_results_message, state.query),
                    icon = AppIcons.Search,
                )

                ContentKind.Results -> Results(state.results, onEvent)
            }
        }
    }
}

private enum class ContentKind { Start, Loading, Error, Empty, Results }

private fun SearchState.contentKind(): ContentKind = when {
    query.isBlank() -> ContentKind.Start
    loadState is LoadState.Loading -> ContentKind.Loading
    loadState is LoadState.Error -> ContentKind.Error
    loadState is LoadState.Empty -> ContentKind.Empty
    else -> ContentKind.Results
}

@Composable
private fun StartContent(recent: List<String>, onEvent: (SearchEvent) -> Unit) {
    if (recent.isEmpty()) {
        AppEmptyState(
            title = stringResource(R.string.search_start_title),
            message = stringResource(R.string.search_start_message),
            icon = AppIcons.Search,
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppTheme.spacing.gutter, vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = stringResource(R.string.search_recent),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.contentSecondary,
            )
            AppButton(
                text = stringResource(R.string.search_clear_recent),
                onClick = { onEvent(SearchEvent.ClearRecent) },
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Small,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            recent.forEach { query ->
                AppChip(
                    label = query,
                    onClick = { onEvent(SearchEvent.RecentClicked(query)) },
                    leadingIcon = AppIcons.Clock,
                    onRemove = { onEvent(SearchEvent.RecentRemoved(query)) },
                )
            }
        }
    }
}

@Composable
private fun Results(results: List<SearchResult>, onEvent: (SearchEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
    ) {
        items(results, key = SearchResult::id) { result ->
            AppCard(
                onClick = { onEvent(SearchEvent.ResultClicked(result)) },
                modifier = appAnimateItem(Modifier.fillMaxWidth()),
            ) {
                AppText(
                    text = result.title,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.contentPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                AppText(
                    text = result.snippet,
                    modifier = Modifier.padding(top = AppTheme.spacing.xs),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.contentTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LoadingResults() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
        userScrollEnabled = false,
    ) {
        items(SKELETON_ROWS) { AppCard { AppSkeletonListItem(showLeading = false) } }
    }
}

private const val SKELETON_ROWS = 4
