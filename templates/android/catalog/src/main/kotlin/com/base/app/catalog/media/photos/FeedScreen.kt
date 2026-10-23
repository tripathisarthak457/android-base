package com.base.app.catalog.media.photos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.base.app.catalog.catalogContentPadding
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppAvatar
import com.base.app.core.designsystem.component.navigation.AppTopBar
import com.base.app.core.designsystem.component.navigation.scrolledFromTop
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.image.AppNetworkImage
import com.base.app.core.image.PrefetchImages
import kotlin.math.roundToInt

/**
 * A social feed of real photos loaded over the network: a row of stories, then full-width pictures at
 * their own aspect ratios. The image gates measure it (`--es section Feed`), so it is built the way an
 * app's feed should be: every item keyed and typed, every picture sized from the data before it
 * arrives, its strings built before the list draws, and the photos ahead fetched to disk as it moves.
 */
@Composable
internal fun PhotoFeedScreen(onBack: (() -> Unit)?) {
    val list = rememberLazyListState()
    AppScaffold(
        topBar = { AppTopBar(title = "Photo feed", onBack = onBack, scrolled = { list.scrolledFromTop() }) },
    ) {
        LazyColumn(
            state = list,
            modifier = Modifier
                .fillMaxSize()
                .testTag("media_feed"),
            contentPadding = catalogContentPadding(PaddingValues(vertical = AppTheme.spacing.md)),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
            flingBehavior = AppScrollDefaults.flingBehavior(),
        ) {
            item(key = "stories", contentType = "stories") { Stories() }
            items(FeedPosts, key = { "post-${it.id}" }, contentType = { "post" }) { post -> FeedPost(post) }
            item(key = "credits", contentType = "credits") { Credits(FeedCredits) }
        }
    }
    // The first item is the stories row, so post n is item n + 1.
    PrefetchImages(list) { index -> FeedPosts.getOrNull(index - 1)?.photoUrl }
}

@Composable
private fun Stories() {
    // A story is the size of a list row, larger than a post's avatar, which keeps AppAvatar's own size.
    val storySize = AppTheme.sizes.rowHeight
    LazyRow(
        contentPadding = PaddingValues(horizontal = AppTheme.spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        flingBehavior = AppScrollDefaults.flingBehavior(),
    ) {
        items(StoryItems, key = { "story-${it.id}" }, contentType = { "story" }) { story ->
            Column(
                modifier = Modifier.width(storySize),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                AppAvatar(name = story.author, size = storySize) {
                    AppNetworkImage(url = story.photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
                AppText(
                    text = story.firstName,
                    style = AppTheme.typography.caption1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FeedPost(post: Post) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Row(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppAvatar(name = post.author) {
                AppNetworkImage(url = post.avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            AppText(
                text = post.author,
                modifier = Modifier.weight(1f),
                style = AppTheme.typography.headline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AppText(text = post.age, style = AppTheme.typography.caption1, color = AppTheme.colors.contentTertiary)
        }
        // The ratio comes from the data, so the row has its final height before the photo arrives.
        AppNetworkImage(
            url = post.photoUrl,
            contentDescription = post.caption,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(post.ratio),
        )
        Row(modifier = Modifier.padding(horizontal = AppTheme.spacing.sm)) {
            AppIconButton(icon = AppIcons.Heart, contentDescription = "Like", onClick = {})
            AppIconButton(icon = AppIcons.Message, contentDescription = "Comment", onClick = {})
            AppIconButton(icon = AppIcons.Share, contentDescription = "Share", onClick = {})
            Spacer(Modifier.weight(1f))
            AppIconButton(icon = AppIcons.Bookmark, contentDescription = "Save", onClick = {})
        }
        Column(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
        ) {
            AppText(text = post.likes, style = AppTheme.typography.subheadline)
            AppText(
                text = post.caption,
                style = AppTheme.typography.subheadline,
                color = AppTheme.colors.contentSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun Credits(text: String) {
    AppText(
        text = text,
        modifier = Modifier.padding(horizontal = AppTheme.spacing.gutter),
        style = AppTheme.typography.footnote,
        color = AppTheme.colors.contentTertiary,
    )
}

/** One post, every string formatted and every URL built before the list draws it. */
@Immutable
private class Post(
    val id: Int,
    val author: String,
    val age: String,
    val likes: String,
    val caption: String,
    val ratio: Float,
    val photoUrl: String,
    val avatarUrl: String,
)

@Immutable
private class Story(val id: Int, val author: String, val firstName: String, val photoUrl: String)

/**
 * A photo's own shape, held between 4:5 and 1.91:1 as photo feeds hold it, so one tall picture
 * cannot fill three screens. Requested 1080 px wide, the size a feed API serves; JPEG and WebP alternate.
 */
private val FeedPosts: List<Post> = SamplePhotos.take(FEED_LENGTH).mapIndexed { index, photo ->
    val ratio = (photo.width.toFloat() / photo.height).coerceIn(MIN_RATIO, MAX_RATIO)
    val height = (FEED_WIDTH / ratio).roundToInt()
    Post(
        id = photo.id,
        author = photo.author,
        age = "${index % 23 + 1} h",
        likes = "${(photo.id * 37 + index * 11) % 4000 + 12} likes",
        caption = "Photo by ${photo.author} on Unsplash",
        ratio = ratio,
        photoUrl = "https://picsum.photos/id/${photo.id}/$FEED_WIDTH/$height.${if (photo.id % 2 == 0) "jpg" else "webp"}",
        avatarUrl = "https://picsum.photos/id/${photo.id}/$AVATAR_PIXELS/$AVATAR_PIXELS.jpg",
    )
}

private val StoryItems: List<Story> = SamplePhotos.takeLast(STORY_COUNT).map { photo ->
    Story(photo.id, photo.author, photo.author.substringBefore(' '), "https://picsum.photos/id/${photo.id}/200/200.jpg")
}

private const val FeedCredits = "Photos from Unsplash, through Lorem Picsum"
private const val FEED_LENGTH = 120
private const val STORY_COUNT = 12
private const val FEED_WIDTH = 1080
private const val AVATAR_PIXELS = 96
private const val MIN_RATIO = 0.8f
private const val MAX_RATIO = 1.91f
