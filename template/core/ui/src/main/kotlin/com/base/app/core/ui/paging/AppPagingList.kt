package com.base.app.core.ui.paging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.base.app.core.designsystem.animation.appAnimateItem
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.feedback.AppCircularProgress
import com.base.app.core.designsystem.component.refresh.AppPullToRefresh
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A paged list with every state Paging can be in already drawn.
 *
 * - First load: [loading], usually skeleton rows shaped like the real ones.
 * - First load failed: [failed], with a retry that restarts from the beginning.
 * - Nothing came back: [empty].
 * - Loading the next page: a spinner under the last row.
 * - The next page failed: a short line and a retry under the last row, keeping every row already
 *   loaded. Replacing a long list with a full-screen error because page seven failed is the
 *   most irritating thing a feed can do.
 *
 * Pull to refresh reloads from where the user is, not from the top — see the paging source's
 * `getRefreshKey`.
 */
@Composable
fun <T : Any> AppPagingList(
    items: LazyPagingItems<T>,
    key: (T) -> Any,
    loadMoreFailedLabel: String,
    retryLabel: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.gutter),
    loading: @Composable () -> Unit = {},
    failed: @Composable (error: Throwable, retry: () -> Unit) -> Unit = { _, _ -> },
    empty: @Composable () -> Unit = {},
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    val refresh = items.loadState.refresh
    val nothingYet = items.itemCount == 0

    when {
        nothingYet && refresh is LoadState.Loading -> loading()
        nothingYet && refresh is LoadState.Error -> failed(refresh.error) { items.retry() }
        nothingYet && refresh is LoadState.NotLoading && items.loadState.append.endOfPaginationReached -> empty()
        else -> AppPullToRefresh(
            isRefreshing = refresh is LoadState.Loading && !nothingYet,
            onRefresh = items::refresh,
            modifier = modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = rememberLazyListState(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
            ) {
                items(count = items.itemCount, key = items.itemKey(key)) { index ->
                    val item = items[index] ?: return@items
                    Box(modifier = appAnimateItem()) { itemContent(item) }
                }

                item(key = "append-footer") {
                    val append = items.loadState.append
                    AppendFooter(
                        loading = append is LoadState.Loading,
                        failed = append is LoadState.Error,
                        failedLabel = loadMoreFailedLabel,
                        retryLabel = retryLabel,
                        onRetry = items::retry,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppendFooter(
    loading: Boolean,
    failed: Boolean,
    failedLabel: String,
    retryLabel: String,
    onRetry: () -> Unit,
) {
    when {
        loading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppTheme.spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            AppCircularProgress(size = AppTheme.sizes.iconLarge)
        }

        failed -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = failedLabel,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.contentSecondary,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = retryLabel,
                onClick = onRetry,
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
            )
        }
    }
}
