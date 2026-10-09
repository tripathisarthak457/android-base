package com.base.app.core.designsystem.component.feedback

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.foundation.LocalTabVisible
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.delay

/**
 * iOS's activity indicator: eight spokes, the brightest stepping round. It steps rather than spins,
 * so it redraws eight times a revolution, not every frame, and it stops when its screen is hidden.
 * Its motion is the message, so it keeps turning with motion reduced, at half the pace.
 */
@Composable
fun AppActivityIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = AppTheme.colors.contentSecondary,
) {
    var head by remember { mutableIntStateOf(0) }
    val visible = LocalTabVisible.current
    val step = if (AppTheme.reduceMotion) STEP_MILLIS * 2 else STEP_MILLIS
    if (visible) {
        LaunchedEffect(step) {
            while (true) {
                delay(step)
                head = (head + 1) % SPOKES
            }
        }
    }
    val description = stringResource(R.string.designsystem_loading)
    Canvas(modifier.size(size).semantics { contentDescription = description }) {
        val outer = this.size.minDimension / 2f
        val inner = outer * 0.48f
        val width = outer * 0.2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        for (i in 0 until SPOKES) {
            val age = (head - i + SPOKES) % SPOKES
            rotate(degrees = i * 360f / SPOKES, pivot = center) {
                drawLine(
                    color = color,
                    start = Offset(center.x, center.y - inner),
                    end = Offset(center.x, center.y - outer + width / 2f),
                    strokeWidth = width,
                    cap = StrokeCap.Round,
                    alpha = 1f - age * (0.75f / SPOKES),
                )
            }
        }
    }
}

/** A determinate bar. The fill glides to each new [progress] on the layout spring, read while drawing. */
@Composable
fun AppLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.accent,
    trackColor: Color = AppTheme.colors.fill,
) {
    val shown = animateFloatAsState(progress.coerceIn(0f, 1f), AppTheme.springs.layout.unit, label = "linearProgress")
    Canvas(modifier.fillMaxWidth().height(4.dp).progressSemantics(progress.coerceIn(0f, 1f))) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(trackColor, cornerRadius = radius)
        val width = size.width * shown.value
        if (width > 0f) {
            val start = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
            drawRoundRect(color, topLeft = Offset(start, 0f), size = Size(width, size.height), cornerRadius = radius)
        }
    }
}

private const val SPOKES = 8
private const val STEP_MILLIS = 85L
