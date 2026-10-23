package com.base.app.catalog.media.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.base.app.catalog.LocalCatalogEnvironment
import com.base.app.catalog.ProvideEnvironment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode
import com.base.app.core.image.PrefetchImages
import com.base.app.core.video.AppVideo
import com.base.app.core.video.AppVideoDefaults
import com.base.app.core.video.VideoFeedState
import com.base.app.core.video.rememberVideoFeedState

/**
 * A short-video feed, as a reels app has it: one clip a page, the settled one playing muted, a swipe
 * to the next. It is the scene the video gates measure (`--es section Reels`): the pages share a few
 * players, each clip draws on a SurfaceView, the neighbours are ready before a swipe lands, and the
 * posters of the pages ahead are fetched to disk.
 */
@Composable
internal fun ReelsScreen(onBack: (() -> Unit)?) {
    // Video looks the same in either theme, so the scene and the words over it are drawn dark.
    ProvideEnvironment(LocalCatalogEnvironment.current.copy(themeMode = ThemeMode.Dark)) { Reels(onBack) }
}

@Composable
private fun Reels(onBack: (() -> Unit)?) {
    val pager = rememberPagerState { ReelPages.size }
    val feed = rememberVideoFeedState(ReelClips, current = { pager.settledPage })
    // Once, in the frame that first composes the scene: its time to first frame counts from here.
    SideEffect(Unit) { VideoPerfLog.opened() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        VerticalPager(
            state = pager,
            modifier = Modifier
                .fillMaxSize()
                .testTag("media_reels"),
            beyondViewportPageCount = AppVideoDefaults.BeyondPages,
            flingBehavior = AppScrollDefaults.pagerFlingBehavior(pager),
            key = { "clip-$it" },
        ) { page -> ReelPage(feed, page) }
        Controls(feed, onBack)
    }
    PrefetchImages(pager) { page -> ReelPages.getOrNull(page)?.clip?.posterUrl }
}

@Composable
private fun ReelPage(feed: VideoFeedState, page: Int) {
    val sample = ReelPages[page]
    val scrim = AppTheme.colors.scrim
    val fade = remember(scrim) { Brush.verticalGradient(listOf(Color.Transparent, scrim)) }
    Box(Modifier.fillMaxSize()) {
        AppVideo(feed, index = page, contentDescription = sample.title, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(fade)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(AppTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
        ) {
            AppText(text = sample.author, style = AppTheme.typography.headline, color = AppTheme.colors.contentPrimary)
            AppText(
                text = sample.title,
                style = AppTheme.typography.subheadline,
                color = AppTheme.colors.contentPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            AppText(text = Credit, style = AppTheme.typography.caption1, color = AppTheme.colors.contentSecondary)
        }
    }
}

/** Back and sound over the feed; the sound follows the person from clip to clip. */
@Composable
private fun Controls(feed: VideoFeedState, onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            AppIconButton(icon = AppIcons.ArrowLeft, contentDescription = "Back", onClick = onBack, variant = ButtonVariant.Tinted)
        }
        Spacer(Modifier.weight(1f))
        AppIconButton(
            icon = if (feed.muted) AppIcons.VolumeOff else AppIcons.VolumeOn,
            contentDescription = if (feed.muted) "Turn sound on" else "Turn sound off",
            onClick = feed::toggleSound,
            variant = ButtonVariant.Tinted,
        )
    }
}

private val ReelClips = ReelPages.map(SampleClip::clip)
private const val Credit = "Public domain (CC0)"
