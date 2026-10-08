package com.base.app.feature.feed

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
import com.base.app.core.common.userMessage
import com.base.app.core.common.util.UiText
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppAvatar
import com.base.app.core.designsystem.component.feedback.AppEmptyState
import com.base.app.core.designsystem.component.feedback.AppErrorState
import com.base.app.core.designsystem.component.feedback.AppSkeletonListItem
import com.base.app.core.designsystem.component.navigation.AppLargeTitle
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.ui.MviScreen
import com.base.app.core.ui.asString
import com.base.app.core.ui.paging.AppPagingList
import com.base.app.data.feed.FeedLoadException
import com.base.app.data.feed.FeedPost

@Composable
fun FeedRoute(viewModel: FeedViewModel = hiltViewModel()) {
    val posts = viewModel.posts.collectAsLazyPagingItems()
    MviScreen(viewModel = viewModel, onEffect = {}) { state, onEvent ->
        FeedScreen(state = state, posts = posts, onEvent = onEvent)
    }
}

@Composable
fun FeedScreen(
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
            loading = { LoadingFeed() },
            failed = { error, retry ->
                AppErrorState(
                    message = (error as? FeedLoadException)?.failure
                        ?.userMessage(UiText.of(R.string.feed_load_failed))
                        ?.asString()
                        ?: stringResource(R.string.feed_load_failed),
                    isOffline = (error as? FeedLoadException)?.failure?.isOffline == true,
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
        contentPadding = PaddingValues(AppTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
        userScrollEnabled = false,
    ) {
        items(SKELETON_ROWS) {
            AppCard { AppSkeletonListItem() }
        }
    }
}

private const val SKELETON_ROWS = 5
