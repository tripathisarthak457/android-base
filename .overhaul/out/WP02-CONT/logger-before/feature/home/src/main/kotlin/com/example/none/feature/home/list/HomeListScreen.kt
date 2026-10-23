package com.example.none.feature.home.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.none.core.common.LoadState
import com.example.none.core.common.isRefreshing
import com.example.none.core.common.result.AppError
import com.example.none.core.designsystem.component.container.AppCard
import com.example.none.core.designsystem.component.container.AppScaffold
import com.example.none.core.designsystem.component.feedback.AppSkeleton
import com.example.none.core.designsystem.component.feedback.AppStateView
import com.example.none.core.designsystem.component.feedback.StateKind
import com.example.none.core.designsystem.component.navigation.AppLargeTitleTopBar
import com.example.none.core.designsystem.component.navigation.scrolledFromTop
import com.example.none.core.designsystem.component.refresh.AppPullToRefresh
import com.example.none.core.designsystem.component.text.AppText
import com.example.none.core.designsystem.foundation.layout.ReportScrollEdge
import com.example.none.core.designsystem.foundation.layout.chromeContentPadding
import com.example.none.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.none.core.designsystem.preview.AppScreenPreviews
import com.example.none.core.designsystem.theme.AppTheme
import com.example.none.core.navigation.ScrollToTopOnReselect
import com.example.none.core.ui.text
import com.example.none.data.home.api.HomeItem
import com.example.none.feature.home.R

@Composable
internal fun HomeListScreen(
    state: HomeListState,
    onEvent: (HomeListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppLargeTitleTopBar(
                title = stringResource(R.string.home_title),
                scrolled = { listState.scrolledFromTop() },
            )
        },
    ) {
        when (val loadState = state.loadState) {
            // Idle is the first frame, before the route's Appeared has started the load.
            LoadState.Idle, LoadState.Loading -> LoadingList()

            is LoadState.Error -> AppStateView(
                message = loadState.error.text(),
                kind = if (loadState.error == AppError.Offline) {
                    StateKind.Offline
                } else {
                    StateKind.Error
                },
                onAction = { onEvent(HomeListEvent.Retry) },
            )

            LoadState.Empty -> AppStateView(
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.home_empty_message),
                actionLabel = stringResource(R.string.home_reload),
                onAction = { onEvent(HomeListEvent.Retry) },
            )

            LoadState.Success, LoadState.Refreshing -> AppPullToRefresh(
                isRefreshing = loadState.isRefreshing,
                onRefresh = { onEvent(HomeListEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
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
                    items(items = state.items, key = HomeItem::id) { item ->
                        HomeRow(
                            item = item,
                            onClick = { onEvent(HomeListEvent.ItemClicked(item.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRow(item: HomeItem, onClick: () -> Unit) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        AppText(
            text = item.title,
            style = AppTheme.typography.headline,
            color = AppTheme.colors.contentPrimary,
        )
    }
}

/** Laid out like the content list, so the rows replace the placeholders in place. */
@Composable
private fun LoadingList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
        userScrollEnabled = false,
    ) {
        items(SKELETON_ROWS) { HomeRowPlaceholder() }
    }
}

/**
 * The row's card with a bone where the title goes. A transparent line in the title's style
 * sizes the bone, so the row that replaces it is exactly as tall at any font scale.
 */
@Composable
private fun HomeRowPlaceholder() {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .clearAndSetSemantics {},
        ) {
            AppText(text = " ", style = AppTheme.typography.headline, color = Color.Transparent)
            AppSkeleton(modifier = Modifier.matchParentSize(), shape = AppTheme.shapes.xs)
        }
    }
}

private const val SKELETON_ROWS = 6

@AppScreenPreviews
@Composable
private fun HomeListScreenPreview() {
    HomeListScreen(
        state = HomeListState(
            loadState = LoadState.Success,
            items = List(4) { index ->
                HomeItem(id = "$index", title = "Home ${index + 1}")
            },
        ),
        onEvent = {},
    )
}
