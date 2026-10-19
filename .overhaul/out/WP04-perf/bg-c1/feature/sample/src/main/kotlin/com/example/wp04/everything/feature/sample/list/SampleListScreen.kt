package com.example.wp04.everything.feature.sample.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.wp04.everything.core.common.LoadState
import com.example.wp04.everything.core.common.hasContent
import com.example.wp04.everything.core.common.isRefreshing
import com.example.wp04.everything.core.common.result.AppError
import com.example.wp04.everything.core.designsystem.animation.appAnimateItem
import com.example.wp04.everything.core.designsystem.component.container.AppCard
import com.example.wp04.everything.core.designsystem.component.container.AppScaffold
import com.example.wp04.everything.core.designsystem.component.feedback.AppEmptyState
import com.example.wp04.everything.core.designsystem.component.feedback.AppErrorState
import com.example.wp04.everything.core.designsystem.component.feedback.AppSkeletonListItem
import com.example.wp04.everything.core.designsystem.component.input.AppSearchField
import com.example.wp04.everything.core.designsystem.component.navigation.AppLargeTitle
import com.example.wp04.everything.core.designsystem.component.refresh.AppPullToRefresh
import com.example.wp04.everything.core.designsystem.foundation.layout.ReportScrollEdge
import com.example.wp04.everything.core.designsystem.foundation.layout.chromeContentPadding
import com.example.wp04.everything.core.designsystem.icon.AppIcons
import com.example.wp04.everything.core.designsystem.theme.AppTheme
import com.example.wp04.everything.core.navigation.ScrollToTopOnReselect
import com.example.wp04.everything.core.ui.text
import com.example.wp04.everything.data.sample.api.SampleItem
import com.example.wp04.everything.feature.sample.R
import com.example.wp04.everything.feature.sample.component.SampleRow

/**
 * The list screen. Stateless by construction: it takes a state, the ViewModel's search field, and
 * emits events. It holds nothing of its own.
 */
@Composable
internal fun SampleListScreen(
    state: SampleListState,
    query: TextFieldState,
    onEvent: (SampleListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            Column {
                AppLargeTitle(
                    title = stringResource(R.string.sample_title),
                    subtitle = stringResource(R.string.sample_pulled_from_a_live_api),
                )
                // The design-system field takes a String: the text is read from the ViewModel's
                // field and written straight back, so nothing is copied into the state.
                AppSearchField(
                    value = query.text.toString(),
                    onValueChange = query::setTextAndPlaceCursorAtEnd,
                    placeholder = stringResource(R.string.sample_search_titles),
                    modifier = Modifier.padding(
                        horizontal = AppTheme.spacing.gutter,
                        vertical = AppTheme.spacing.sm,
                    ),
                )
            }
        },
    ) {
        when {
            // Idle is the first frame, before the route's Appeared has started the load.
            state.loadState == LoadState.Idle || state.loadState == LoadState.Loading -> LoadingList()

            state.loadState is LoadState.Error -> AppErrorState(
                message = state.loadState.error.text(),
                isOffline = state.loadState.error == AppError.Offline,
                onRetry = { onEvent(SampleListEvent.Retry) },
            )

            state.loadState is LoadState.Empty -> AppEmptyState(
                title = stringResource(R.string.sample_nothing_here_yet),
                message = stringResource(R.string.sample_empty_explanation),
                icon = AppIcons.ListView,
                actionLabel = stringResource(R.string.sample_reload),
                onAction = { onEvent(SampleListEvent.Retry) },
            )

            state.loadState.hasContent -> Content(state, query, onEvent)
        }
    }
}

@Composable
private fun Content(
    state: SampleListState,
    query: TextFieldState,
    onEvent: (SampleListEvent) -> Unit,
) {
    AppPullToRefresh(
        isRefreshing = state.loadState.isRefreshing,
        onRefresh = { onEvent(SampleListEvent.Refresh) },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val visible = state.visibleItems
            if (visible.isEmpty()) {
                AppEmptyState(
                    title = stringResource(R.string.sample_no_matches),
                    message = stringResource(R.string.sample_no_matches_for_query, query.text.trim()),
                    icon = AppIcons.Search,
                )
                return@Column
            }

            val listState = rememberLazyListState()
            ScrollToTopOnReselect(listState)
            ReportScrollEdge(listState)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("primary_list"),
                state = listState,
                contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
            ) {
                // Keyed on the id so inserts keep scroll position and animations.
                items(items = visible, key = SampleItem::id) { item ->
                    SampleRow(
                        item = item,
                        onClick = { onEvent(SampleListEvent.ItemClicked(item.id)) },
                        modifier = appAnimateItem(),
                    )
                }
            }
        }
    }
}

/**
 * The loading state is shaped like the content it stands in for — see
 * [com.example.wp04.everything.core.designsystem.component.feedback.AppSkeleton].
 */
@Composable
private fun LoadingList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
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
        val items = List(4) {
            SampleItem(
                id = it,
                title = "A sample row number ${it + 1}",
                body = "Some supporting copy that gets truncated when it runs long enough.",
            )
        }
        SampleListScreen(
            state = SampleListState(loadState = LoadState.Success, items = items, visibleItems = items),
            query = rememberTextFieldState(),
            onEvent = {},
        )
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun SampleListLoadingPreview() {
    AppTheme {
        SampleListScreen(
            state = SampleListState(loadState = LoadState.Loading),
            query = rememberTextFieldState(),
            onEvent = {},
        )
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun SampleListEmptyPreview() {
    AppTheme {
        SampleListScreen(
            state = SampleListState(loadState = LoadState.Empty),
            query = rememberTextFieldState(),
            onEvent = {},
        )
    }
}
