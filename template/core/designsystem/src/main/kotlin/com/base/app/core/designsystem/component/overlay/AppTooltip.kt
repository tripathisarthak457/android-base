package com.base.app.core.designsystem.component.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.base.app.core.designsystem.animation.rememberAppTransitions
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.delay

/**
 * A short explanation anchored above whatever it wraps.
 *
 * ## It is not a replacement for a label
 *
 * A tooltip is invisible until someone long-presses, and on a touch device most people never
 * will. It is for the *second* level of detail — what a threshold means, why a field is disabled
 * — never for the only description a control has. An icon-only button still needs its
 * `contentDescription`.
 *
 * ## Long-press, not hover
 *
 * Hover exists on desktop and on a connected mouse, and nowhere else that matters here. The
 * gesture is a long press, which is also what TalkBack's own "read more" gesture maps to.
 *
 * It dismisses itself after [durationMillis]; a tooltip that waits for a tap elsewhere leaves the
 * user tapping the screen to get rid of something they did not ask for.
 */
@Composable
fun AppTooltip(
    text: String,
    modifier: Modifier = Modifier,
    durationMillis: Long = 2_500,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val transitions = rememberAppTransitions()

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        delay(durationMillis)
        visible = false
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                // A tooltip must not steal the tap: the control it wraps keeps its own onClick,
                // and only the long press belongs to us.
                onClick = {},
                onLongClick = { visible = true },
            ),
        ) {
            content()
        }

        if (visible) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, -TOOLTIP_GAP_PX),
                onDismissRequest = { visible = false },
                properties = PopupProperties(focusable = false),
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = transitions.popIn,
                    exit = transitions.popOut,
                ) {
                    AppSurface(
                        shape = AppTheme.shapes.xs,
                        color = AppTheme.colors.surfaceInverse,
                        contentColor = AppTheme.colors.contentInverse,
                        elevation = AppTheme.elevation.overlay,
                    ) {
                        AppText(
                            text = text,
                            modifier = Modifier
                                .widthIn(max = MAX_WIDTH)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            style = AppTheme.typography.caption,
                            color = AppTheme.colors.contentInverse,
                        )
                    }
                }
            }
        }
    }
}

private val MAX_WIDTH = 240.dp
private const val TOOLTIP_GAP_PX = 8
