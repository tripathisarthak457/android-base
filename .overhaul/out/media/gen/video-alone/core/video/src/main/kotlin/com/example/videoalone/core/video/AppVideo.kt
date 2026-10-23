@file:OptIn(UnstableApi::class)

package com.example.videoalone.core.video

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import com.example.videoalone.core.designsystem.component.button.AppButton
import com.example.videoalone.core.designsystem.component.button.ButtonVariant
import com.example.videoalone.core.designsystem.component.text.AppIcon
import com.example.videoalone.core.designsystem.component.text.AppText
import com.example.videoalone.core.designsystem.foundation.interaction.PressFeedback
import com.example.videoalone.core.designsystem.foundation.interaction.pressable
import com.example.videoalone.core.designsystem.icon.AppIcons
import com.example.videoalone.core.designsystem.theme.AppTheme
import com.example.videoalone.core.image.AppNetworkImage

/**
 * The clip at [index] of [state]'s feed, filling its bounds: its poster until the first frame, then
 * the video, drawn on a SurfaceView, so the system composites every video frame and a playing clip
 * costs the app no frames at all. It plays only while it is the feed's settled clip and its screen
 * shows; a tap pauses or plays it. The settled, playing clip keeps the screen on.
 *
 * A SurfaceView is a separate layer: glass chrome cannot blur it and the back gesture's card does not
 * contain it, so a full-screen feed gives the floating bar its solid look.
 */
@Composable
fun AppVideo(
    state: VideoFeedState,
    index: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val clip = state.clips.getOrNull(index) ?: return
    val player = state.playerAt(index)
    val playing = state.plays(index)
    Box(
        modifier = modifier
            .clipToBounds()
            .then(if (playing) Modifier.keepScreenOn() else Modifier)
            .pressable(
                onClick = { state.togglePlayback(index) },
                feedback = PressFeedback.None,
                onClickLabel = stringResource(if (playing) R.string.video_pause else R.string.video_play),
            )
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (player == null) {
            Poster(clip.posterUrl, contentScale)
        } else {
            ContentFrame(
                player = player,
                modifier = Modifier.fillMaxSize(),
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                contentScale = contentScale,
                shutter = { Poster(clip.posterUrl, contentScale) },
            )
        }
        when {
            state.failed(index) -> Failed(onRetry = { state.retry(index) })
            state.waitsForTap(index) -> AppIcon(AppIcons.Play, contentDescription = null, tint = AppTheme.colors.onAccent)
        }
    }
}

/** Defaults for a feed of [AppVideo]s. */
object AppVideoDefaults {
    /**
     * Pages a feed's pager composes either side of the settled one: their players are prepared and
     * their first frame drawn while paused, so a swipe reveals a frame, not a poster.
     */
    const val BeyondPages: Int = VideoDefaults.BEYOND_PAGES
}

/** The clip's own picture until its first frame; the skeleton colour when the clip has none. */
@Composable
private fun Poster(url: String?, contentScale: ContentScale) {
    val modifier = Modifier
        .fillMaxSize()
        .testTag(PosterTag)
    if (url == null) {
        // Never AppNetworkImage without a url: it would draw its failure glyph over a clip that is fine.
        Box(modifier.background(AppTheme.colors.skeleton))
    } else {
        AppNetworkImage(url = url, contentDescription = null, modifier = modifier, contentScale = contentScale)
    }
}

internal const val PosterTag = "video_poster"

/** A clip that cannot play keeps its poster and offers another try; the feed goes on around it. */
@Composable
private fun Failed(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        AppText(text = stringResource(R.string.video_failed), color = AppTheme.colors.onAccent)
        AppButton(text = stringResource(R.string.video_retry), onClick = onRetry, variant = ButtonVariant.Tinted)
    }
}
