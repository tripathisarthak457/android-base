package __PKG__.feature.__NAME__.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import __PKG__.core.common.LoadState
import __PKG__.core.common.isRefreshing
import __PKG__.core.common.result.AppError
import __PKG__.core.designsystem.component.container.AppCard
import __PKG__.core.designsystem.component.container.AppScaffold
import __PKG__.core.designsystem.component.feedback.AppEmptyState
import __PKG__.core.designsystem.component.feedback.AppErrorState
import __PKG__.core.designsystem.component.feedback.AppSkeletonLine
import __PKG__.core.designsystem.component.navigation.AppLargeTitle
import __PKG__.core.designsystem.component.refresh.AppPullToRefresh
import __PKG__.core.designsystem.component.text.AppText
import __PKG__.core.designsystem.foundation.chromeContentPadding
import __PKG__.core.designsystem.theme.AppTheme
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
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = { AppLargeTitle(title = stringResource(R.string.__NAME___title)) },
    ) {
        when (val loadState = state.loadState) {
            // Idle is the first frame, before the route's Appeared has started the load.
            LoadState.Idle, LoadState.Loading -> LoadingList()

            is LoadState.Error -> AppErrorState(
                message = loadState.error.text(),
                isOffline = loadState.error == AppError.Offline,
                onRetry = { onEvent(__P__ListEvent.Retry) },
            )

            LoadState.Empty -> AppEmptyState(
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("primary_list"),
                    contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
                ) {
                    items(items = state.items, key = __P__Item::id) { item ->
                        __P__Row(
                            item = item,
                            onClick = { onEvent(__P__ListEvent.ItemClicked(item.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun __P__Row(item: __P__Item, onClick: () -> Unit) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        AppText(
            text = item.title,
            style = AppTheme.typography.titleMedium,
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
 * [__P__Row]'s card around one bone at the title's line height, so the row that replaces
 * it is as tall at any font scale.
 */
@Composable
private fun __P__RowPlaceholder() {
    val lineHeight = AppTheme.typography.titleMedium.lineHeight
    AppCard(modifier = Modifier.fillMaxWidth()) {
        AppSkeletonLine(
            height = with(LocalDensity.current) { lineHeight.toDp() },
            widthFraction = 0.6f,
        )
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
                    __P__Item(id = "$index", title = "__TITLE__ ${index + 1}")
                },
            ),
            onEvent = {},
        )
    __THEMED_END__
}
