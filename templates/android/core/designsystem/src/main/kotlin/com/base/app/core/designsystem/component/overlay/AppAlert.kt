package com.base.app.core.designsystem.component.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.container.AppVerticalDivider
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.foundation.HairlineEdge
import com.base.app.core.designsystem.foundation.PressFeedback
import com.base.app.core.designsystem.foundation.hairline
import com.base.app.core.designsystem.foundation.pressable
import com.base.app.core.designsystem.theme.AppTheme

/**
 * An alert, as iOS draws one: a title, a message, and one or two actions, over a dimmed window. It
 * renders in the window's overlay layer, above the tab bar and any sheet, whichever screen calls it.
 * Keep it composed and drive [visible], so it can animate out.
 *
 * The confirm action only calls [onConfirm]: the caller decides when to close, so a "Delete" that
 * clears its pending item on dismiss still has the item when it runs. Back and [dismissLabel] call
 * [onDismissRequest]; a tap outside does nothing, as on iOS, so a choice is never lost to a stray
 * touch.
 */
@Composable
fun AppAlert(
    visible: Boolean,
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissLabel: String? = null,
    destructive: Boolean = false,
    icon: ImageVector? = null,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState) return
    val dismiss by rememberUpdatedState(onDismissRequest)

    HostedOverlay(modal = true) {
        val springs = AppTheme.springs
        val transition = rememberTransition(state, label = "alert")
        val shown = transition.animateFloat(transitionSpec = { springs.sheet.unit }, label = "alertShown") { if (it) 1f else 0f }
        BackHandler(enabled = state.targetState) { dismiss() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = shown.value.coerceIn(0f, 1f) }
                .background(AppTheme.colors.scrim)
                // Whatever the alert's own buttons leave goes nowhere; the screen beneath never sees it.
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
            contentAlignment = Alignment.Center,
        ) {
            AppSurface(
                modifier = modifier
                    .padding(AppTheme.spacing.xl)
                    .widthIn(max = AlertWidth)
                    .graphicsLayer {
                        // Arrives from slightly larger, as iOS's does; leaves by fading.
                        val p = shown.value
                        val scale = if (state.targetState) 1f + (1f - p) * ENTER_SCALE else 1f
                        scaleX = scale
                        scaleY = scale
                    }
                    .semantics { paneTitle = title },
                shape = AppTheme.shapes.lg,
                color = AppTheme.colors.surfaceElevated,
                elevation = AppTheme.elevation.modal,
            ) {
                Column {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                    ) {
                        icon?.let { AppIcon(it, contentDescription = null, tint = if (destructive) AppTheme.colors.danger.content else AppTheme.colors.accentContent, size = 28.dp) }
                        AppText(text = title, style = AppTheme.typography.headline, color = AppTheme.colors.contentPrimary, textAlign = TextAlign.Center)
                        message?.let {
                            AppText(text = it, style = AppTheme.typography.footnote, color = AppTheme.colors.contentPrimary, textAlign = TextAlign.Center)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).hairline(HairlineEdge.Top)) {
                        dismissLabel?.let {
                            AlertAction(it, AppTheme.colors.accentContent, bold = true, onClick = dismiss, modifier = Modifier.weight(1f))
                            AppVerticalDivider()
                        }
                        AlertAction(
                            label = confirmLabel,
                            color = if (destructive) AppTheme.colors.danger.content else AppTheme.colors.accentContent,
                            bold = dismissLabel == null,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertAction(label: String, color: Color, bold: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .pressable(onClick = onClick, feedback = PressFeedback.Highlight)
            .heightIn(min = ActionHeight)
            .padding(horizontal = AppTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = label,
            style = if (bold) AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold) else AppTheme.typography.body,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private val AlertWidth = 300.dp
private val ActionHeight = 44.dp
private const val ENTER_SCALE = 0.12f
