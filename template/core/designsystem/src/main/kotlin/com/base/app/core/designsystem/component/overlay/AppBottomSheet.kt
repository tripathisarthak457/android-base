package com.base.app.core.designsystem.component.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.clickableNoIndication
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.launch

/**
 * A modal bottom sheet, drag-to-dismiss included.
 *
 * ## Why it is built by hand
 *
 * There is no bottom sheet outside Material. What it needs — a full-window surface, a scrim, an
 * entrance that can be interrupted, and a drag that either dismisses or springs back — is about
 * a hundred lines, and building it means the sheet obeys this project's motion tokens rather
 * than Material's.
 *
 * ## Dismissal is by distance *or* velocity
 *
 * A slow drag past the halfway mark dismisses; so does a fast flick that never got there. Testing
 * only distance means a confident flick springs back, which feels broken because the user has
 * already moved on. Testing only velocity means a deliberate slow drag to the bottom does
 * nothing. Both, and neither gesture is misread.
 *
 * ## Exit is animated, unlike the dialog's
 *
 * Dismissal runs the slide-down first and calls [onDismissRequest] when it finishes, so the sheet
 * is still composed while it animates. Achieving the same for a dialog is not worth the ceremony;
 * for a sheet — which the user is physically dragging — a snap-off would be jarring.
 */
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    showHandle: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val scope = rememberCoroutineScope()
        // Captured once: AppTheme's accessors are composable getters, and the drag callbacks and
        // the dismiss animation below all run outside composition.
        val motion = AppTheme.motion
        val offsetY = remember { Animatable(START_OFFSET) }
        var sheetHeight by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(Unit) {
            offsetY.animateTo(0f, motion.sheet())
        }

        fun dismiss() {
            scope.launch {
                offsetY.animateTo(
                    targetValue = sheetHeight.takeIf { it > 0f } ?: START_OFFSET,
                    animationSpec = tween(motion.medium, easing = motion.exit),
                )
                onDismissRequest()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // The scrim fades with the sheet's own travel, so dragging the sheet down lightens
            // the screen behind it progressively — the feedback that tells you the gesture is
            // going to dismiss before you have committed to it.
            val progress = if (sheetHeight > 0f) {
                (1f - offsetY.value / sheetHeight).coerceIn(0f, 1f)
            } else {
                0f
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress }
                    .background(AppTheme.colors.scrim)
                    .then(
                        if (dismissOnClickOutside) {
                            Modifier.clickableNoIndication(onClick = ::dismiss)
                        } else {
                            Modifier
                        },
                    ),
            )

            Column(
                modifier = modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { sheetHeight = it.height.toFloat() }
                    .graphicsLayer { translationY = offsetY.value }
                    .clip(AppTheme.shapes.sheet)
                    .background(AppTheme.colors.surface)
                    .draggable(
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                // Upward drags are clamped at zero: a sheet that can be pulled
                                // above its resting position exposes the window behind it.
                                offsetY.snapTo((offsetY.value + delta).coerceAtLeast(0f))
                            }
                        },
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            val past = sheetHeight > 0f && offsetY.value > sheetHeight * DISMISS_FRACTION
                            if (past || velocity > DISMISS_VELOCITY) {
                                dismiss()
                            } else {
                                offsetY.animateTo(0f, motion.sheet())
                            }
                        },
                    )
                    .navigationBarsPadding()
                    .padding(bottom = AppTheme.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showHandle) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = AppTheme.spacing.md)
                            .width(HANDLE_WIDTH)
                            .height(HANDLE_HEIGHT)
                            .clip(AppTheme.shapes.pill)
                            .background(AppTheme.colors.borderStrong),
                    )
                }

                title?.let {
                    AppText(
                        text = it,
                        style = AppTheme.typography.headingMedium,
                        color = AppTheme.colors.contentPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = AppTheme.spacing.gutter,
                                vertical = AppTheme.spacing.sm,
                            ),
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.spacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                    content = content,
                )
            }
        }
    }
}

/** Large enough to be off-screen before the sheet has been measured. */
private const val START_OFFSET = 3000f
private const val DISMISS_FRACTION = 0.4f
private const val DISMISS_VELOCITY = 1200f
private val HANDLE_WIDTH = 38.dp
private val HANDLE_HEIGHT = 4.dp
