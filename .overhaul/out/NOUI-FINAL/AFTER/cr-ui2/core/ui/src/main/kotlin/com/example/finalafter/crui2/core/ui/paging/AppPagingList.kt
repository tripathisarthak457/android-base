package com.example.finalafter.crui2.core.ui.paging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.finalafter.crui2.core.designsystem.animation.appAnimateItem
import com.example.finalafter.crui2.core.designsystem.component.button.AppButton
import com.example.finalafter.crui2.core.designsystem.component.button.ButtonSize
import com.example.finalafter.crui2.core.designsystem.component.button.ButtonVariant
import com.example.finalafter.crui2.core.designsystem.component.feedback.AppActivityIndicator
import com.example.finalafter.crui2.core.designsystem.component.refresh.AppPullToRefresh
import com.example.finalafter.crui2.core.designsystem.component.text.AppText
import com.example.finalafter.crui2.core.designsystem.foundation.layout.chromeContentPadding
import com.example.finalafter.crui2.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme

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
 * Hoist [state] to drive a large title from the list: `scrolled = { state.scrolledFromTop() }`. A
 * [loading] list keeps a state of its own, never [state], for the reason `AppRedacted` gives. The
 * list starts under the screen's top bar and ends above its bottom chrome unless [contentPadding]
 * says otherwise; [modifier] applies to whichever state is showing.
 */
@Composable
fun <T : Any> AppPagingList(
    items: LazyPagingItems<T>,
    key: (T) -> Any,
    loadMoreFailedLabel: String,
    retryLabel: String,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
    loading: @Composable () -> Unit = {},
    failed: @Composable (error: Throwable, retry: () -> Unit) -> Unit = { _, _ -> },
    empty: @Composable () -> Unit = {},
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    val refresh = items.loadState.refresh
    val nothingYet = items.itemCount == 0

    when {
        nothingYet && refresh is LoadState.Loading -> StateBox(modifier) { loading() }
        nothingYet && refresh is LoadState.Error -> StateBox(modifier) { failed(refresh.error) { items.retry() } }
        nothingYet && refresh is LoadState.NotLoading && items.loadState.append.endOfPaginationReached -> {
            StateBox(modifier) { empty() }
        }
        else -> AppPullToRefresh(
            isRefreshing = refresh is LoadState.Loading && !nothingYet,
            onRefresh = items::refresh,
            modifier = modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
                flingBehavior = AppScrollDefaults.flingBehavior(),
            ) {
                items(
                    count = items.itemCount,
                    key = items.itemKey(key),
                    contentType = items.itemContentType { ROW },
                ) { index ->
                    val item = items[index] ?: return@items
                    Box(modifier = appAnimateItem()) { itemContent(item) }
                }

                item(key = APPEND_FOOTER, contentType = APPEND_FOOTER) {
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
private fun StateBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), propagateMinConstraints = true) { content() }
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
            AppActivityIndicator(size = AppTheme.sizes.iconLarge)
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
                style = AppTheme.typography.subheadline,
                color = AppTheme.colors.contentSecondary,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = retryLabel,
                onClick = onRetry,
                variant = ButtonVariant.Tinted,
                size = ButtonSize.Small,
            )
        }
    }
}

private const val ROW = "row"
private const val APPEND_FOOTER = "append-footer"
