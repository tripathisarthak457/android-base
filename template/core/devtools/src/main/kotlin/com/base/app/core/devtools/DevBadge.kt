package com.base.app.core.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.appClickable
import com.base.app.core.designsystem.theme.AppTheme
import kotlin.math.roundToInt

/**
 * The environment badge, and the panel it opens.
 *
 * Call this once, as the last child of the root Box, so it draws over whatever screen is showing.
 * An overlay rather than something the app's content is wrapped in: wrapping would mean one more
 * level of indentation around the whole app that exists only when this feature is switched on,
 * and a diff nobody can read. Everything here is a no-op when [DevEnvironment.visible] is false,
 * so there is no call site anybody has to remember to remove before a release.
 *
 * ## Why it is draggable
 *
 * A badge in a fixed corner covers something eventually — a top-bar action, a snackbar, the exact
 * row somebody is trying to read — and the answer to that cannot be "move it in the source and
 * rebuild". It stays where it was put across configuration changes and deliberately resets on a
 * fresh launch, because a badge dragged into a corner and forgotten is one nobody finds again.
 */
@Composable
fun DevToolsOverlay(environment: DevEnvironment, log: DevToolsLog) {
    if (!environment.visible) return

    var panelOpen by rememberSaveable { mutableStateOf(false) }

    DraggableBadge(label = environment.label, onClick = { panelOpen = true })

    if (panelOpen) {
        DevToolsPanel(
            environment = environment,
            log = log,
            onClose = { panelOpen = false },
        )
    }
}

@Composable
private fun DraggableBadge(label: String, onClick: () -> Unit) {
    val density = LocalDensity.current

    // Kept in pixels because a drag delta arrives in pixels. NaN means "never dragged", which is
    // what lets the default position be decided once the layout's size is known rather than
    // guessed at before it is.
    var offsetX by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var offsetY by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    var badge by remember { mutableStateOf(IntSize.Zero) }

    val startX = with(density) { AppTheme.spacing.lg.toPx() }
    val startY = with(density) { DEFAULT_TOP_INSET.toPx() }
    val x = if (offsetX.isNaN()) startX else offsetX
    val y = if (offsetY.isNaN()) startY else offsetY

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { bounds = it },
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .onSizeChanged { badge = it }
                .pointerInput(bounds, badge) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        // Clamped to the window: a badge dragged past the edge is unreachable
                        // without knowing to rotate the device to bring it back.
                        offsetX = (x + drag.x)
                            .coerceIn(0f, (bounds.width - badge.width).coerceAtLeast(0).toFloat())
                        offsetY = (y + drag.y)
                            .coerceIn(0f, (bounds.height - badge.height).coerceAtLeast(0).toFloat())
                    }
                }
                .background(
                    color = AppTheme.colors.warning.content,
                    shape = RoundedCornerShape(percent = ROUNDED_PERCENT),
                )
                .appClickable(onClick = onClick, minTouchTarget = 0.dp)
                .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs)
                .semantics { contentDescription = "$label build. Opens the developer inspector." },
        ) {
            AppText(
                text = label,
                style = AppTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

private val DEFAULT_TOP_INSET = 48.dp
private const val ROUNDED_PERCENT = 50
