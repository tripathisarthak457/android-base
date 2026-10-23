package io.example.fitclocal.feature.abcdefghijklmno.list

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
import io.example.fitclocal.core.common.LoadState
import io.example.fitclocal.core.common.isRefreshing
import io.example.fitclocal.core.common.result.AppError
import io.example.fitclocal.core.designsystem.R as DesignSystemR
import io.example.fitclocal.core.designsystem.component.container.AppCard
import io.example.fitclocal.core.designsystem.component.container.AppScaffold
import io.example.fitclocal.core.designsystem.component.feedback.AppSkeleton
import io.example.fitclocal.core.designsystem.component.feedback.AppStateView
import io.example.fitclocal.core.designsystem.component.feedback.StateKind
import io.example.fitclocal.core.designsystem.component.navigation.AppLargeTitleTopBar
import io.example.fitclocal.core.designsystem.component.navigation.scrolledFromTop
import io.example.fitclocal.core.designsystem.component.refresh.AppPullToRefresh
import io.example.fitclocal.core.designsystem.component.text.AppText
import io.example.fitclocal.core.designsystem.foundation.layout.ReportScrollEdge
import io.example.fitclocal.core.designsystem.foundation.layout.chromeContentPadding
import io.example.fitclocal.core.designsystem.foundation.scroll.AppScrollDefaults
import io.example.fitclocal.core.designsystem.preview.AppScreenPreviews
import io.example.fitclocal.core.designsystem.theme.AppTheme
import io.example.fitclocal.core.navigation.ScrollToTopOnReselect
import io.example.fitclocal.core.ui.text
import io.example.fitclocal.data.abcdefghijklmno.api.model.AbcdefghijklmnoItem
import io.example.fitclocal.feature.abcdefghijklmno.R

@Composable
internal fun AbcdefghijklmnoListScreen(
    state: AbcdefghijklmnoListState,
    onEvent: (AbcdefghijklmnoListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppLargeTitleTopBar(
                title = stringResource(R.string.abcdefghijklmno_title),
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
                onAction = { onEvent(AbcdefghijklmnoListEvent.Retry) },
            )

            LoadState.Empty -> AppStateView(
                title = stringResource(R.string.abcdefghijklmno_empty_title),
                message = stringResource(R.string.abcdefghijklmno_empty_message),
                actionLabel = stringResource(R.string.abcdefghijklmno_reload),
                onAction = { onEvent(AbcdefghijklmnoListEvent.Retry) },
            )

            LoadState.Success, LoadState.Refreshing -> AppPullToRefresh(
                isRefreshing = loadState.isRefreshing,
                onRefresh = { onEvent(AbcdefghijklmnoListEvent.Refresh) },
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
                        key = AbcdefghijklmnoItem::id,
                        contentType = { ROW },
                    ) { item ->
                        AbcdefghijklmnoRow(
                            item = item,
                            onClick = { onEvent(AbcdefghijklmnoListEvent.ItemClicked(item.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AbcdefghijklmnoRow(item: AbcdefghijklmnoItem, onClick: () -> Unit) {
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
        repeat(SKELETON_ROWS) { AbcdefghijklmnoRowPlaceholder() }
    }
}

/**
 * The row's card with a bone where the title goes. A transparent line in the title's style
 * sizes the bone, so the row that replaces it is exactly as tall at any font scale.
 */
@Composable
private fun AbcdefghijklmnoRowPlaceholder() {
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
private fun AbcdefghijklmnoListScreenPreview() {
    AbcdefghijklmnoListScreen(
        state = AbcdefghijklmnoListState(
            loadState = LoadState.Success,
            items = List(4) { index ->
                AbcdefghijklmnoItem(id = "$index", title = "Abcdefghijklmno ${index + 1}")
            },
        ),
        onEvent = {},
    )
}
