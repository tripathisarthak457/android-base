package com.example.finalafter.crui2.feature.sample.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import com.example.finalafter.crui2.core.common.LoadState
import com.example.finalafter.crui2.core.common.hasContent
import com.example.finalafter.crui2.core.common.isRefreshing
import com.example.finalafter.crui2.core.common.result.AppError
import com.example.finalafter.crui2.core.designsystem.R as DesignSystemR
import com.example.finalafter.crui2.core.designsystem.animation.appAnimateItem
import com.example.finalafter.crui2.core.designsystem.component.container.AppScaffold
import com.example.finalafter.crui2.core.designsystem.component.feedback.AppRedacted
import com.example.finalafter.crui2.core.designsystem.component.feedback.AppStateView
import com.example.finalafter.crui2.core.designsystem.component.feedback.StateKind
import com.example.finalafter.crui2.core.designsystem.component.input.AppSearchField
import com.example.finalafter.crui2.core.designsystem.component.navigation.AppLargeTitleTopBar
import com.example.finalafter.crui2.core.designsystem.component.navigation.scrolledFromTop
import com.example.finalafter.crui2.core.designsystem.component.refresh.AppPullToRefresh
import com.example.finalafter.crui2.core.designsystem.foundation.layout.ReportScrollEdge
import com.example.finalafter.crui2.core.designsystem.foundation.layout.chromeContentPadding
import com.example.finalafter.crui2.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.finalafter.crui2.core.designsystem.icon.AppIcons
import com.example.finalafter.crui2.core.designsystem.preview.AppScreenPreviews
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import com.example.finalafter.crui2.core.navigation.ScrollToTopOnReselect
import com.example.finalafter.crui2.core.ui.text
import com.example.finalafter.crui2.data.sample.api.model.SampleItem
import com.example.finalafter.crui2.feature.sample.R
import com.example.finalafter.crui2.feature.sample.component.SampleRow

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
    val listState = rememberLazyListState()
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppLargeTitleTopBar(
                title = stringResource(R.string.sample_title),
                scrolled = { listState.scrolledFromTop() },
                subtitle = stringResource(R.string.sample_pulled_from_a_live_api),
                accessory = {
                    AppSearchField(state = query, placeholder = stringResource(R.string.sample_search_titles))
                },
            )
        },
    ) {
        when {
            // Idle is the first frame, before the screen's first observation has started the load.
            state.loadState == LoadState.Idle || state.loadState == LoadState.Loading -> LoadingList()

            state.loadState is LoadState.Error -> AppStateView(
                message = state.loadState.error.text(),
                kind = if (state.loadState.error == AppError.Offline) StateKind.Offline else StateKind.Error,
                onAction = { onEvent(SampleListEvent.Retry) },
            )

            state.loadState is LoadState.Empty -> AppStateView(
                title = stringResource(R.string.sample_nothing_here_yet),
                message = stringResource(R.string.sample_empty_explanation),
                icon = AppIcons.ListView,
                actionLabel = stringResource(R.string.sample_reload),
                onAction = { onEvent(SampleListEvent.Retry) },
            )

            state.loadState.hasContent -> Content(state, query, listState, onEvent)
        }
    }
}

@Composable
private fun Content(
    state: SampleListState,
    query: TextFieldState,
    listState: LazyListState,
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
                AppStateView(
                    title = stringResource(R.string.sample_no_matches),
                    message = stringResource(R.string.sample_no_matches_for_query, query.text.trim()),
                    icon = AppIcons.Search,
                )
                return@Column
            }

            ScrollToTopOnReselect(listState)
            ReportScrollEdge(listState)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("primary_list"),
                state = listState,
                contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
                flingBehavior = AppScrollDefaults.flingBehavior(),
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
 * The loading state: real rows, redacted, in a list with the loaded list's padding. The first row
 * starts below the large title exactly where the first loaded row will, and the swap to content is
 * one frame with nothing moving or fading. It keeps a list state of its own, as `AppRedacted` asks.
 */
@Composable
private fun LoadingList() {
    val loading = stringResource(DesignSystemR.string.designsystem_loading)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .clearAndSetSemantics { contentDescription = loading },
        contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
        userScrollEnabled = false,
    ) {
        items(PlaceholderRows, key = { it }) {
            AppRedacted(active = true) { SampleRow(item = SampleRowPlaceholder, onClick = {}) }
        }
    }
}

/** Enough for the tallest window; a lazy list composes only the rows that show. */
private const val PlaceholderRows = 12

// Never shown (see AppRedacted). A one-line title and a two-line preview, as most samples show.
private val SampleRowPlaceholder = SampleItem(
    -1,
    "Placeholder sample title",
    "The preview of a sample runs to about two lines on a phone, which is what this one is for.",
)

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

@AppScreenPreviews
@Composable
private fun SampleListLoadingPreview() {
    SampleListScreen(
        state = SampleListState(loadState = LoadState.Loading),
        query = rememberTextFieldState(),
        onEvent = {},
    )
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
