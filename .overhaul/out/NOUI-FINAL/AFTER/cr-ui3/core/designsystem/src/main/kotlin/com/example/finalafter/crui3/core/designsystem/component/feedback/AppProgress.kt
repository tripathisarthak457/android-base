package com.example.finalafter.crui3.core.designsystem.component.feedback

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui3.core.designsystem.R
import com.example.finalafter.crui3.core.designsystem.foundation.layout.LocalTabVisible
import com.example.finalafter.crui3.core.designsystem.preview.AppComponentPreviews
import com.example.finalafter.crui3.core.designsystem.theme.AppTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * iOS's activity indicator: eight spokes, the brightest stepping round. It steps rather than spins,
 * so it redraws eight times a revolution, not every frame, and it stops when its screen is hidden.
 * Its motion is the message, so it keeps turning with motion reduced, at half the pace.
 */
@Composable
fun AppActivityIndicator(
    modifier: Modifier = Modifier,
    size: Dp = AppProgressDefaults.IndicatorSize,
    color: Color = AppTheme.colors.contentSecondary,
) {
    var head by remember { mutableIntStateOf(0) }
    val visibility = LocalTabVisible.current
    val step = if (AppTheme.reduceMotion) StepMillis * 2 else StepMillis
    // The holder's value is read in the effect, so hiding the screen stops the steps without a recomposition.
    LaunchedEffect(visibility, step) {
        snapshotFlow { visibility.isVisible }.collectLatest { visible ->
            while (visible) {
                delay(step)
                head = (head + 1) % ActivitySpokes
            }
        }
    }
    val description = stringResource(R.string.designsystem_loading)
    // Its own layer, so each step redraws the indicator and not everything around it.
    Canvas(modifier.size(size).graphicsLayer().semantics { contentDescription = description }) {
        drawActivitySpokes(
            center = Offset(this.size.width / 2f, this.size.height / 2f),
            radius = this.size.minDimension / 2f,
            color = color,
            head = head,
        )
    }
}

/**
 * The activity indicator's spokes, shared by [AppActivityIndicator] and pull to refresh so the
 * spinner a pull reveals is the one that then turns. With [head] null the spokes are still, the
 * first [revealed] of them shown at the brightest spoke's alpha; otherwise they fade behind [head].
 * The geometry is iOS's: spokes from a third of the radius to its edge, a fifth of it wide.
 */
internal fun DrawScope.drawActivitySpokes(
    center: Offset,
    radius: Float,
    color: Color,
    head: Int?,
    revealed: Int = ActivitySpokes,
    alpha: Float = 1f,
) {
    val width = radius / 5f
    for (i in 0 until revealed.coerceAtMost(ActivitySpokes)) {
        val spokeAlpha = if (head == null) SpokeAlphas[0] else SpokeAlphas[(head - i).mod(ActivitySpokes)]
        rotate(degrees = i * 360f / ActivitySpokes, pivot = center) {
            drawLine(
                color = color,
                start = Offset(center.x, center.y - radius / 3f - width / 2f),
                end = Offset(center.x, center.y - radius + width / 2f),
                strokeWidth = width,
                cap = StrokeCap.Round,
                alpha = spokeAlpha * alpha,
            )
        }
    }
}

internal const val ActivitySpokes = 8

/** Newest to oldest, as iOS fades the trail behind the brightest spoke. */
private val SpokeAlphas = floatArrayOf(147f, 122f, 97f, 72f, 47f, 47f, 47f, 47f).map { it / 255f }.toFloatArray()

/**
 * A determinate bar. The fill glides to each new value of [progress] on the layout spring. The
 * value is read as a lambda, so a download moving the bar on every frame redraws it without
 * recomposing anything; a screen reader hears it in steps of five percent.
 */
@Composable
fun AppLinearProgress(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.accent,
    trackColor: Color = AppTheme.colors.fill,
) {
    val springs = AppTheme.springs
    val current by rememberUpdatedState(progress)
    val shown = remember { Animatable(Snapshot.withoutReadObservation { current().coerceIn(0f, 1f) }) }
    LaunchedEffect(shown, springs) {
        snapshotFlow { current().coerceIn(0f, 1f) }.collectLatest { shown.animateTo(it, springs.layout.unit) }
    }
    val announced by remember { derivedStateOf { (current().coerceIn(0f, 1f) * AnnouncedSteps).roundToInt() / AnnouncedSteps } }
    Canvas(modifier.fillMaxWidth().height(AppProgressDefaults.LinearHeight).progressSemantics(announced)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(trackColor, cornerRadius = radius)
        val width = size.width * shown.value
        if (width > 0f) {
            val start = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
            drawRoundRect(color, topLeft = Offset(start, 0f), size = Size(width, size.height), cornerRadius = radius)
        }
    }
}

private const val AnnouncedSteps = 20f
private const val StepMillis = 125L

/** The progress indicators' sizes. */
object AppProgressDefaults {
    val IndicatorSize = 20.dp
    val LinearHeight = 4.dp
}

@AppComponentPreviews
@Composable
private fun AppProgressPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
            AppActivityIndicator()
            AppActivityIndicator(size = AppTheme.sizes.iconLarge, color = AppTheme.colors.accent)
        }
        AppLinearProgress(progress = { 0f })
        AppLinearProgress(progress = { 0.4f })
        AppLinearProgress(progress = { 1f }, color = AppTheme.colors.success.base)
    }
}
