package com.base.app.core.designsystem.component.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.foundation.LocalTabVisible
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * One sweep shared by every skeleton in the window, so a list of placeholders sweeps in step and
 * costs one frame callback rather than one animation each. It runs only while a skeleton that is on
 * screen needs it: placeholders in a hidden tab stop asking, and with none left the clock stops, so
 * an idle screen draws no frames.
 */
@Stable
internal class ShimmerClock(private val scope: CoroutineScope) {
    var progress by mutableFloatStateOf(0f)
        private set
    private var users = 0
    private var job: Job? = null

    fun attach() {
        users++
        if (job == null) {
            job = scope.launch {
                while (true) {
                    withFrameMillis { time -> progress = (time % PeriodMillis) / PeriodMillis.toFloat() }
                }
            }
        }
    }

    fun detach() {
        users--
        if (users == 0) {
            job?.cancel()
            job = null
        }
    }

    private companion object {
        const val PeriodMillis = 1_400L
    }
}

internal val LocalShimmer = staticCompositionLocalOf<ShimmerClock?> { null }

/**
 * A placeholder in the shape of content that has not arrived: a lighter band sweeps across it. With
 * motion reduced it holds still. Hidden from screen readers; the screen announces loading once.
 */
@Composable
fun AppSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.sm,
) {
    val colors = AppTheme.colors
    val clock = LocalShimmer.current
    val animated = clock != null && LocalTabVisible.current && !AppTheme.reduceMotion
    if (animated) {
        DisposableEffect(clock) {
            clock.attach()
            onDispose { clock.detach() }
        }
    }
    Spacer(
        modifier = modifier
            .clearAndSetSemantics {}
            .drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                val clip = Path().apply { addOutline(outline) }
                val band = size.width * BandFraction
                // Built once per size; each frame only moves it.
                val brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to colors.skeletonHighlight,
                    1f to Color.Transparent,
                    startX = 0f,
                    endX = band,
                )
                onDrawBehind {
                    drawOutline(outline, colors.skeleton)
                    if (animated) {
                        val x = -band + (size.width + band) * clock.progress
                        clipPath(clip) {
                            translate(left = x) { drawRect(brush, size = size.copy(width = band)) }
                        }
                    }
                }
            },
    )
}

/** A line of placeholder text, [widthFraction] of the space it is given. */
@Composable
fun AppSkeletonLine(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    height: Dp = 14.dp,
) {
    AppSkeleton(modifier = modifier.fillMaxWidth(widthFraction).height(height), shape = AppTheme.shapes.xs)
}

@Composable
fun AppSkeletonCircle(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    AppSkeleton(modifier = modifier.size(size), shape = AppTheme.shapes.pill)
}

/** A placeholder shaped like a list row with a leading avatar and two lines. */
@Composable
fun AppSkeletonListItem(modifier: Modifier = Modifier, showLeading: Boolean = true) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLeading) AppSkeletonCircle()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppSkeletonLine(widthFraction = 0.6f)
            AppSkeletonLine(widthFraction = 0.9f, height = 12.dp)
        }
    }
}

private const val BandFraction = 0.6f
