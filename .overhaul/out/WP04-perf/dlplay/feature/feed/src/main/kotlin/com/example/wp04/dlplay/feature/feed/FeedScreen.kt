package com.example.wp04.dlplay.feature.feed

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.wp04.dlplay.core.common.result.AppError
import com.example.wp04.dlplay.core.designsystem.component.button.AppButton
import com.example.wp04.dlplay.core.designsystem.component.button.ButtonSize
import com.example.wp04.dlplay.core.designsystem.component.button.ButtonVariant
import com.example.wp04.dlplay.core.designsystem.component.container.AppCard
import com.example.wp04.dlplay.core.designsystem.component.container.AppScaffold
import com.example.wp04.dlplay.core.designsystem.component.feedback.AppAvatar
import com.example.wp04.dlplay.core.designsystem.component.feedback.AppEmptyState
import com.example.wp04.dlplay.core.designsystem.component.feedback.AppErrorState
import com.example.wp04.dlplay.core.designsystem.component.feedback.AppSkeletonListItem
import com.example.wp04.dlplay.core.designsystem.component.navigation.AppLargeTitle
import com.example.wp04.dlplay.core.designsystem.component.text.AppText
import com.example.wp04.dlplay.core.designsystem.foundation.layout.chromeContentPadding
import com.example.wp04.dlplay.core.designsystem.icon.AppIcons
import com.example.wp04.dlplay.core.designsystem.theme.AppTheme
import com.example.wp04.dlplay.core.mvi.UiText
import com.example.wp04.dlplay.core.ui.MviScreen
import com.example.wp04.dlplay.core.ui.paging.AppPagingList
import com.example.wp04.dlplay.core.ui.text
import com.example.wp04.dlplay.data.feed.api.FeedLoadException
import com.example.wp04.dlplay.data.feed.api.FeedPost

@Composable
internal fun FeedRoute(viewModel: FeedViewModel = hiltViewModel()) {
    val posts = viewModel.posts.collectAsLazyPagingItems()
    MviScreen(viewModel = viewModel, onEffect = {}) { state, onEvent ->
        FeedScreen(state = state, posts = posts, onEvent = onEvent)
    }
}

@Composable
internal fun FeedScreen(
    state: FeedState,
    posts: LazyPagingItems<FeedPost>,
    onEvent: (FeedEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        topBar = {
            AppLargeTitle(
                title = stringResource(R.string.feed_title),
                subtitle = stringResource(R.string.feed_subtitle),
            )
        },
    ) {
        AppPagingList(
            items = posts,
            key = FeedPost::id,
            loadMoreFailedLabel = stringResource(R.string.feed_load_more_failed),
            retryLabel = stringResource(R.string.feed_retry),
            contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
            loading = { LoadingFeed() },
            failed = { throwable, retry ->
                val error = (throwable as? FeedLoadException)?.error
                AppErrorState(
                    message = error?.text(UiText.of(R.string.feed_load_failed))
                        ?: stringResource(R.string.feed_load_failed),
                    isOffline = error == AppError.Offline,
                    onRetry = retry,
                    retryLabel = stringResource(R.string.feed_retry),
                )
            },
            empty = {
                AppEmptyState(
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
            modifier = Modifier.animateContentSize(AppTheme.motion.sheet()),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppAvatar(name = author)
                AppText(
                    text = author,
                    style = AppTheme.typography.titleSmall,
                    color = AppTheme.colors.contentSecondary,
                )
            }
            AppText(
                text = post.title,
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.contentPrimary,
            )
            AppText(
                text = post.body,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.contentSecondary,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
            AppButton(
                text = stringResource(if (expanded) R.string.feed_show_less else R.string.feed_show_more),
                onClick = onToggle,
                variant = ButtonVariant.Tertiary,
                size = ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun LoadingFeed() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
        userScrollEnabled = false,
    ) {
        items(SKELETON_ROWS) {
            AppCard { AppSkeletonListItem() }
        }
    }
}

private const val SKELETON_ROWS = 5
