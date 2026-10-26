package com.example.cmp.feature.feed

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.compose.LazyPagingItems
import com.example.cmp.core.common.result.AppError
import com.example.cmp.core.designsystem.R as DesignSystemR
import com.example.cmp.core.designsystem.component.button.AppButton
import com.example.cmp.core.designsystem.component.button.ButtonSize
import com.example.cmp.core.designsystem.component.button.ButtonVariant
import com.example.cmp.core.designsystem.component.container.AppCard
import com.example.cmp.core.designsystem.component.container.AppScaffold
import com.example.cmp.core.designsystem.component.feedback.AppAvatar
import com.example.cmp.core.designsystem.component.feedback.AppRedacted
import com.example.cmp.core.designsystem.component.feedback.AppStateView
import com.example.cmp.core.designsystem.component.feedback.StateKind
import com.example.cmp.core.designsystem.component.navigation.AppLargeTitleTopBar
import com.example.cmp.core.designsystem.component.navigation.scrolledFromTop
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.foundation.layout.chromeContentPadding
import com.example.cmp.core.designsystem.icon.AppIcons
import com.example.cmp.core.designsystem.theme.AppTheme
import com.example.cmp.core.mvi.UiText
import com.example.cmp.core.ui.paging.AppPagingList
import com.example.cmp.core.ui.text
import com.example.cmp.data.feed.api.FeedLoadException
import com.example.cmp.data.feed.api.model.FeedPost

@Composable
internal fun FeedScreen(
    state: FeedState,
    posts: LazyPagingItems<FeedPost>,
    onEvent: (FeedEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    AppScaffold(
        modifier = modifier,
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        topBar = {
            AppLargeTitleTopBar(
                title = stringResource(R.string.feed_title),
                scrolled = { listState.scrolledFromTop() },
                subtitle = stringResource(R.string.feed_subtitle),
            )
        },
    ) {
        AppPagingList(
            items = posts,
            key = FeedPost::id,
            state = listState,
            loadMoreFailedLabel = stringResource(R.string.feed_load_more_failed),
            retryLabel = stringResource(R.string.feed_retry),
            contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
            loading = { LoadingFeed() },
            failed = { throwable, retry ->
                val error = (throwable as? FeedLoadException)?.error
                AppStateView(
                    message = error?.text(UiText.of(R.string.feed_load_failed))
                        ?: stringResource(R.string.feed_load_failed),
                    kind = if (error == AppError.Offline) StateKind.Offline else StateKind.Error,
                    onAction = retry,
                    actionLabel = stringResource(R.string.feed_retry),
                )
            },
            empty = {
                AppStateView(
                    title = stringResource(R.string.feed_empty_title),
                    message = stringResource(R.string.feed_empty_message),
                    icon = AppIcons.ListView,
                )
            },
        ) { post ->
            FeedCard(
                post = post,
                expanded = post.id in state.expanded,
                onToggle = { onEvent(FeedEvent.ToggleExpanded(post.id)) },
            )
        }
    }
}

@Composable
private fun FeedCard(
    post: FeedPost,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val author = stringResource(R.string.feed_author, post.authorId)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.animateContentSize(AppTheme.springs.layout.size),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppAvatar(name = author)
                AppText(
                    text = author,
                    style = AppTheme.typography.footnote,
                    color = AppTheme.colors.contentSecondary,
                )
            }
            AppText(
                text = post.title,
                style = AppTheme.typography.headline,
                color = AppTheme.colors.contentPrimary,
            )
            AppText(
                text = post.body,
                style = AppTheme.typography.callout,
                color = AppTheme.colors.contentSecondary,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
            AppButton(
                text = stringResource(if (expanded) R.string.feed_show_less else R.string.feed_show_more),
                onClick = onToggle,
                variant = ButtonVariant.Plain,
                size = ButtonSize.Small,
            )
        }
    }
}

/**
 * Real cards, redacted, in a list with the feed's padding, so the first page lands in place. It
 * keeps a list state of its own, as `AppRedacted` asks.
 */
@Composable
private fun LoadingFeed() {
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
            AppRedacted(active = true) { FeedCard(post = FeedPlaceholder, expanded = false, onToggle = {}) }
        }
    }
}

/** Enough for the tallest window; a lazy list composes only the cards that show. */
private const val PlaceholderRows = 8

// Never shown (see AppRedacted). A one-line title and a body clamped to two lines, as most posts show.
private val FeedPlaceholder = FeedPost(
    -1,
    1,
    "Placeholder post title",
    "The body of a post is clamped to two lines until it is expanded, and this one runs past them.",
    "The body of a post is clamped to two lines until it is expanded, and this one runs past them.",
)
