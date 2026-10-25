package __PKG__.feature.__NAME__.list

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
import __PKG__.core.common.LoadState
import __PKG__.core.common.isRefreshing
import __PKG__.core.common.result.AppError
import __PKG__.core.designsystem.component.container.AppCard
import __PKG__.core.designsystem.component.container.AppScaffold
import __PKG__.core.designsystem.component.feedback.AppSkeleton
import __PKG__.core.designsystem.component.feedback.AppStateView
import __PKG__.core.designsystem.component.feedback.StateKind
import __PKG__.core.designsystem.component.navigation.AppLargeTitleTopBar
import __PKG__.core.designsystem.component.navigation.scrolledFromTop
import __PKG__.core.designsystem.component.refresh.AppPullToRefresh
import __PKG__.core.designsystem.component.text.AppText
import __PKG__.core.designsystem.foundation.layout.ReportScrollEdge
import __PKG__.core.designsystem.foundation.layout.chromeContentPadding
import __PKG__.core.designsystem.foundation.scroll.AppScrollDefaults
import __PKG__.core.designsystem.theme.AppTheme
import __PKG__.core.navigation.ScrollToTopOnReselect
import __PKG__.core.ui.text
import __PKG__.data.__NAME__.api.__P__Item
import __PKG__.feature.__NAME__.R
__PREVIEW_IMPORTS__

@Composable
internal fun __P__ListScreen(
    state: __P__ListState,
    onEvent: (__P__ListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppLargeTitleTopBar(
                title = stringResource(R.string.__NAME___title),
                scrolled = { listState.scrolledFromTop() },
            )
        },
    ) {
        when (val loadState = state.loadState) {
            // Idle is the first frame, before the screen's first observation has started the load.
            LoadState.Idle, LoadState.Loading -> LoadingList()

            is LoadState.Error -> AppStateView(
                message = loadState.error.text(),
                kind = if (loadState.error == AppError.Offline) {
                    StateKind.Offline
                } else {
                    StateKind.Error
                },
                onAction = { onEvent(__P__ListEvent.Retry) },
            )

            LoadState.Empty -> AppStateView(
                title = stringResource(R.string.__NAME___empty_title),
                message = stringResource(R.string.__NAME___empty_message),
                actionLabel = stringResource(R.string.__NAME___reload),
                onAction = { onEvent(__P__ListEvent.Retry) },
            )

            LoadState.Success, LoadState.Refreshing -> AppPullToRefresh(
                isRefreshing = loadState.isRefreshing,
                onRefresh = { onEvent(__P__ListEvent.Refresh) },
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
__FIT__
                    items(items = state.items, key = __P__Item::id) { item ->
__ELSE__
                    items(
                        items = state.items,
                        key = __P__Item::id,
                    ) { item ->
__END_FIT__
                        __P__Row(
                            item = item,
__FIT__
                            onClick = { onEvent(__P__ListEvent.ItemClicked(item.id)) },
__ELSE__
                            onClick = {
                                onEvent(__P__ListEvent.ItemClicked(item.id))
                            },
__ELSE__
                            onClick = {
                                onEvent(
                                    __P__ListEvent.ItemClicked(item.id),
                                )
                            },
__END_FIT__
                        )
                    }
                }
            }
        }
    }
}

@Composable
__FIT__
private fun __P__Row(item: __P__Item, onClick: () -> Unit) {
__ELSE__
private fun __P__Row(
    item: __P__Item,
    onClick: () -> Unit,
) {
__END_FIT__
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
        items(SKELETON_ROWS) { __P__RowPlaceholder() }
    }
}

/**
 * The row's card with a bone where the title goes. A transparent line in the title's style
 * sizes the bone, so the row that replaces it is exactly as tall at any font scale.
 */
@Composable
private fun __P__RowPlaceholder() {
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

__PREVIEW__
private fun __P__ListScreenPreview() {
    __THEMED_START__
        __P__ListScreen(
            state = __P__ListState(
                loadState = LoadState.Success,
                items = List(4) { index ->
__FIT__
                    __P__Item(id = "$index", title = "__TITLE__ ${index + 1}")
__ELSE__
                    __P__Item(
                        id = "$index",
                        title = "__TITLE__ ${index + 1}",
                    )
__END_FIT__
                },
            ),
            onEvent = {},
        )
    __THEMED_END__
}
