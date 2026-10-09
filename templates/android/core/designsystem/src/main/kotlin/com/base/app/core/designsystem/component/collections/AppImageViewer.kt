package com.base.app.core.designsystem.component.collections

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * A full-screen picture: pinch or double-tap to zoom, drag to look around the zoomed picture
 * without running off its edges, and at normal size swipe down to close, the background fading as
 * it goes. Show it as a full-screen cover; [content] draws the image (an `AsyncImage`, a painter).
 *
 * Every gesture moves plain floats read in the picture's layer, so zooming and panning redraw and
 * never recompose.
 */
@Composable
fun AppImageViewer(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val springs = AppTheme.springs
    val dismiss by rememberUpdatedState(onDismiss)
    var size by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var pull by remember { mutableFloatStateOf(0f) }
    val zoom = remember { Animatable(1f) }

    fun bounded(offset: Offset, at: Float): Offset {
        val maxX = (size.width * (at - 1f)) / 2f
        val maxY = (size.height * (at - 1f)) / 2f
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }

    fun settleZoom(target: Float, focus: Offset) {
        scope.launch {
            val startScale = scale
            val startPan = pan
            // Keep the tapped point under the finger as the picture zooms in on it.
            val centre = Offset(size.width / 2f, size.height / 2f)
            val targetPan = if (target > 1f) bounded((centre - focus) * (target - 1f), target) else Offset.Zero
            zoom.snapTo(0f)
            zoom.animateTo(1f, springs.sheet.unit) {
                scale = startScale + (target - startScale) * value
                pan = startPan + (targetPan - startPan) * value
            }
        }
    }

    BackHandler { dismiss() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(Color.Black, alpha = 1f - (abs(pull) / DISMISS_FADE).coerceIn(0f, MAX_FADE)) }
            .onSizeChanged { size = it },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { at -> settleZoom(if (scale > 1.01f) 1f else DOUBLE_TAP_ZOOM, at) })
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var pulling = false
                        do {
                            val event = awaitPointerEvent()
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            if (event.changes.size > 1 || scale > 1f) {
                                scale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
                                pan = bounded(pan + panChange, scale)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (panChange.y != 0f && (pulling || abs(panChange.y) > abs(panChange.x))) {
                                pulling = true
                                pull += panChange.y
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        if (pulling) {
                            if (abs(pull) > DISMISS_DISTANCE) {
                                dismiss()
                            } else {
                                scope.launch { animate(pull, 0f, animationSpec = springs.sheet.pixels) { value, _ -> pull = value } }
                            }
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = pan.x
                    translationY = pan.y + pull
                },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        AppIconButton(
            icon = AppIcons.Close,
            contentDescription = stringResource(R.string.designsystem_close),
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.statusBars).padding(AppTheme.spacing.sm),
            variant = ButtonVariant.Tinted,
            tint = Color.White,
        )
    }
}

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val DISMISS_DISTANCE = 220f
private const val DISMISS_FADE = 600f
private const val MAX_FADE = 0.85f
