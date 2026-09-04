package com.base.app.feature.sample.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.base.app.core.common.mvi.LoadState
import com.base.app.core.common.mvi.hasContent
import com.base.app.core.common.mvi.isRefreshing
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppBanner
import com.base.app.core.designsystem.component.feedback.AppEmptyState
import com.base.app.core.designsystem.component.feedback.AppErrorState
import com.base.app.core.designsystem.component.feedback.AppSkeletonListItem
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.input.AppSearchField
import com.base.app.core.designsystem.component.navigation.AppLargeTitle
import com.base.app.core.designsystem.component.refresh.AppPullToRefresh
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.ui.asString
import com.base.app.data.sample.SampleItem

/**
 * The list screen.
 *
 * Stateless by construction: it takes a state and emits events, and holds nothing of its own. That
 * is what makes it previewable in every state below without a ViewModel, a network call, or a
 * device — and previews that need none of those are previews people actually keep working.
 */
@Composable
fun SampleListScreen(
    state: SampleListState,
    onEvent: (SampleListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        topBar = {
            Column {
                AppLargeTitle(title = "Samples", subtitle = "Pulled from a live API")
                AppSearchField(
                    value = state.query,
                    onValueChange = { onEvent(SampleListEvent.QueryChanged(it)) },
                    placeholder = "Search titles",
                    modifier = Modifier.padding(
                        horizontal = AppTheme.spacing.gutter,
                        vertical = AppTheme.spacing.sm,
                    ),
                )
            }
        },
    ) {
        when {
            state.loadState is LoadState.Loading -> LoadingList()

            state.loadState is LoadState.Error -> AppErrorState(
                message = state.loadState.message.asString(),
                isOffline = state.loadState.isOffline,
                onRetry = { onEvent(SampleListEvent.Retry) },
            )

            state.loadState is LoadState.Empty -> AppEmptyState(
                title = "Nothing here yet",
                message = "When there is something to show, it will appear on this screen.",
                icon = AppIcons.ListView,
                actionLabel = "Reload",
                onAction = { onEvent(SampleListEvent.Retry) },
            )

            state.loadState.hasContent -> Content(state, onEvent)
        }
    }
}

@Composable
private fun Content(
    state: SampleListState,
    onEvent: (SampleListEvent) -> Unit,
) {
    AppPullToRefresh(
        isRefreshing = state.loadState.isRefreshing,
        onRefresh = { onEvent(SampleListEvent.Refresh) },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.isFromCache) {
                AppBanner(
                    text = "Showing saved data. Pull down to refresh.",
                    tone = AppTone.Info,
                    icon = AppIcons.WifiOff,
                )
            }

            val visible = state.visibleItems
            if (visible.isEmpty()) {
                AppEmptyState(
                    title = "No matches",
                    message = "Nothing here matches \"${state.query}\".",
                    icon = AppIcons.Search,
                )
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AppTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
            ) {
                // Keyed on the item's own id, not the index. Without a stable key, inserting a row
                // at the top makes Compose re-map every item to a different slot, which loses
                // scroll position and restarts every animation in the list.
                items(items = visible, key = SampleItem::id) { item ->
                    SampleRow(item = item, onClick = { onEvent(SampleListEvent.ItemClicked(item.id)) })
                }
            }
        }
    }
}

@Composable
private fun SampleRow(
    item: SampleItem,
    onClick: () -> Unit,
) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        AppText(
            text = item.title,
            style = AppTheme.typography.titleLarge,
            color = AppTheme.colors.contentPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        AppText(
            text = item.preview,
            modifier = Modifier.padding(top = AppTheme.spacing.xs),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentTertiary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The loading state is shaped like the content it stands in for — see
 * [com.base.app.core.designsystem.component.feedback.AppSkeleton].
 */
@Composable
private fun LoadingList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
    ) {
        items(SKELETON_ROWS) {
            AppCard { AppSkeletonListItem(showLeading = false) }
        }
    }
}

private const val SKELETON_ROWS = 6

@Preview(name = "Content", showBackground = true)
@Composable
private fun SampleListContentPreview() {
    AppTheme {
        SampleListScreen(
            state = SampleListState(
                loadState = LoadState.Success,
                items = List(4) {
                    SampleItem(
                        id = it,
                        title = "A sample row number ${it + 1}",
                        body = "Some supporting copy that gets truncated when it runs long enough.",
                    )
                },
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun SampleListLoadingPreview() {
    AppTheme {
        SampleListScreen(state = SampleListState(loadState = LoadState.Loading), onEvent = {})
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun SampleListEmptyPreview() {
    AppTheme {
        SampleListScreen(state = SampleListState(loadState = LoadState.Empty), onEvent = {})
    }
}
