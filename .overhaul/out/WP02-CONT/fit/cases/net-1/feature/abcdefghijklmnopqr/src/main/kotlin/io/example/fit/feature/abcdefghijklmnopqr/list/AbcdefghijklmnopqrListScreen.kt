package io.example.fit.feature.abcdefghijklmnopqr.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.isRefreshing
import io.example.fit.core.common.result.AppError
import io.example.fit.core.designsystem.R as DesignSystemR
import io.example.fit.core.designsystem.component.container.AppCard
import io.example.fit.core.designsystem.component.container.AppScaffold
import io.example.fit.core.designsystem.component.feedback.AppSkeleton
import io.example.fit.core.designsystem.component.feedback.AppStateView
import io.example.fit.core.designsystem.component.feedback.StateKind
import io.example.fit.core.designsystem.component.navigation.AppLargeTitleTopBar
import io.example.fit.core.designsystem.component.navigation.scrolledFromTop
import io.example.fit.core.designsystem.component.refresh.AppPullToRefresh
import io.example.fit.core.designsystem.component.text.AppText
import io.example.fit.core.designsystem.foundation.layout.ReportScrollEdge
import io.example.fit.core.designsystem.foundation.layout.chromeContentPadding
import io.example.fit.core.designsystem.foundation.scroll.AppScrollDefaults
import io.example.fit.core.designsystem.preview.AppScreenPreviews
import io.example.fit.core.designsystem.theme.AppTheme
import io.example.fit.core.navigation.ScrollToTopOnReselect
import io.example.fit.core.ui.text
import io.example.fit.data.abcdefghijklmnopqr.api.model.AbcdefghijklmnopqrItem
import io.example.fit.feature.abcdefghijklmnopqr.R

@Composable
internal fun AbcdefghijklmnopqrListScreen(
    state: AbcdefghijklmnopqrListState,
    onEvent: (AbcdefghijklmnopqrListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppLargeTitleTopBar(
                title = stringResource(R.string.abcdefghijklmnopqr_title),
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
                onAction = { onEvent(AbcdefghijklmnopqrListEvent.Retry) },
            )

            LoadState.Empty -> AppStateView(
                title = stringResource(R.string.abcdefghijklmnopqr_empty_title),
                message = stringResource(R.string.abcdefghijklmnopqr_empty_message),
                actionLabel = stringResource(R.string.abcdefghijklmnopqr_reload),
                onAction = { onEvent(AbcdefghijklmnopqrListEvent.Retry) },
            )

            LoadState.Success, LoadState.Refreshing -> AppPullToRefresh(
                isRefreshing = loadState.isRefreshing,
                onRefresh = { onEvent(AbcdefghijklmnopqrListEvent.Refresh) },
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
                    items(
                        items = state.items,
                        key = AbcdefghijklmnopqrItem::id,
                        contentType = { ROW },
                    ) { item ->
                        AbcdefghijklmnopqrRow(
                            item = item,
                            onClick = { onEvent(AbcdefghijklmnopqrListEvent.ItemClicked(item.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AbcdefghijklmnopqrRow(item: AbcdefghijklmnopqrItem, onClick: () -> Unit) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        AppText(
            text = item.title,
            style = AppTheme.typography.headline,
            color = AppTheme.colors.contentPrimary,
        )
    }
}

/**
 * Padded and spaced like the content list, so the rows replace the placeholders in place. A few
 * fixed rows that never scroll need none of a lazy list's machinery: they are measured at their
 * own height and cut at the screen's edge, as the list's rows are, and read as one node.
 */
@Composable
private fun LoadingList() {
    val loading = stringResource(DesignSystemR.string.designsystem_loading)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .wrapContentHeight(align = Alignment.Top, unbounded = true)
            .padding(chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)))
            .clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
    ) {
        repeat(SKELETON_ROWS) { AbcdefghijklmnopqrRowPlaceholder() }
    }
}

/**
 * The row's card with a bone where the title goes. A transparent line in the title's style
 * sizes the bone, so the row that replaces it is exactly as tall at any font scale.
 */
@Composable
private fun AbcdefghijklmnopqrRowPlaceholder() {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth(0.6f)) {
            AppText(text = " ", style = AppTheme.typography.headline, color = Color.Transparent)
            AppSkeleton(modifier = Modifier.matchParentSize(), shape = AppTheme.shapes.xs)
        }
    }
}

private const val SKELETON_ROWS = 6

/**
 * The list's one kind of row. Typed now, a header or a footer added later is not handed a row's
 * composition to reuse.
 */
private const val ROW = "row"

@AppScreenPreviews
@Composable
private fun AbcdefghijklmnopqrListScreenPreview() {
    AbcdefghijklmnopqrListScreen(
        state = AbcdefghijklmnopqrListState(
            loadState = LoadState.Success,
            items = List(4) { index ->
                AbcdefghijklmnopqrItem(id = "$index", title = "Abcdefghijklmnopqr ${index + 1}")
            },
        ),
        onEvent = {},
    )
}
