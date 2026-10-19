package com.base.app.core.designsystem.component.sheet.action

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.overlay.HostedOverlay
import com.base.app.core.designsystem.component.overlay.OverlayBackHandler
import com.base.app.core.designsystem.component.sheet.AppSheetDefaults
import com.base.app.core.designsystem.component.sheet.surface.SheetShadowCache
import com.base.app.core.designsystem.foundation.content.AppSurface
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.roundToInt

/**
 * The action sheet where there is room: a card in the window's overlay layer, under the
 * anchor (above it when there is no room below), lined up with its start edge. It grows from the
 * anchor's nearest edge on the dialog spring. A tap outside closes it; so do Back and Escape. It is
 * modal for screen readers: what is beneath is hidden, focus moves to its first action and returns
 * to the anchor when it closes. Never a popup window, so it stays in the app's one window.
 */
@Composable
internal fun ActionCard(
    visible: Boolean,
    actions: List<SheetAction>,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    title: String?,
    message: String?,
    anchor: SheetAnchor?,
) {
    val presence = remember { MutableTransitionState(false) }
    presence.targetState = visible
    if (!presence.currentState && !presence.targetState) return
    val dismiss by rememberUpdatedState(onDismissRequest)
    // Back and Escape close the card; while it leaves they are taken, so nothing beneath pops.
    OverlayBackHandler(enabled = true) { if (presence.targetState) dismiss() }

    HostedOverlay(modal = { true }) {
        val springs = AppTheme.springs
        val colors = AppTheme.colors
        val shown = rememberTransition(presence, label = "actionCard").animateFloat(
            transitionSpec = { springs.dialog.unit },
            label = "actionCardShown",
        ) { if (it) 1f else 0f }
        val pane = title ?: stringResource(R.string.designsystem_actions_pane)
        val dismissLabel = stringResource(R.string.designsystem_sheet_dismiss)
        // allow(sheet-shadow): the sheet's own nine-slice spec, which SheetShadowCache draws
        val shadow = AppSheetDefaults.shadow()
        val shadowCache = remember { SheetShadowCache() }
        val radius = AppTheme.style.sheetRadius
        val first = remember { FocusRequester() }
        val pivot = remember { FloatArray(2) }
        // With every action disabled no row holds the requester, and Compose would log a misuse warning.
        val focusable = actions.any { it.enabled }
        SideEffect(focusable) { if (focusable) first.requestFocus() }
        DisposableEffect(anchor) { onDispose { anchor?.focus?.requestFocus() } }

        Layout(
            content = {
                AppSurface(
                    modifier = modifier
                        .graphicsLayer {
                            val progress = shown.value.coerceIn(0f, 1f)
                            val scale = CardScale + (1f - CardScale) * progress
                            scaleX = scale
                            scaleY = scale
                            alpha = progress
                            transformOrigin = TransformOrigin(pivot[0], pivot[1])
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        }
                        .drawBehind {
                            if (shadow != null) {
                                val corner = radius.toPx()
                                with(shadowCache) { drawShadow(shadow, 0f, 0f, size.width, size.height, corner, 1f) }
                            }
                        }
                        .semantics {
                            paneTitle = pane
                            dismiss(dismissLabel) {
                                dismiss()
                                true
                            }
                        },
                    shape = AppTheme.shapes.lg,
                    color = colors.surfaceElevated,
                    clipContent = true,
                ) {
                    Column {
                        if (title != null || message != null) ActionHeading(title, message)
                        var focused = false
                        actions.forEachIndexed { index, action ->
                            val takesFocus = !focused && action.enabled
                            if (takesFocus) focused = true
                            ActionRow(
                                action = action,
                                divider = index > 0 || title != null || message != null,
                                onClick = {
                                    action.onClick()
                                    dismiss()
                                },
                                modifier = if (takesFocus) Modifier.focusRequester(first) else Modifier,
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .drawBehind { if (anchor == null) drawRect(colors.scrim, alpha = shown.value.coerceIn(0f, 1f)) }
                .pointerInput(Unit) { detectTapGestures { if (presence.targetState) dismiss() } },
        ) { measurables, constraints ->
            val margin = Margin.roundToPx()
            val width = minOf(DesignTokens.Sheet.ActionSheet.cardWidth.roundToPx(), constraints.maxWidth - 2 * margin)
            val card = measurables.first().measure(Constraints(maxWidth = width, maxHeight = constraints.maxHeight - 2 * margin))
            layout(constraints.maxWidth, constraints.maxHeight) {
                val bounds = anchor?.bounds
                val x: Int
                val y: Int
                if (bounds == null) {
                    x = (constraints.maxWidth - card.width) / 2
                    y = (constraints.maxHeight - card.height) / 2
                    pivot[0] = 0.5f
                    pivot[1] = 0.5f
                } else {
                    val below = bounds.bottom.roundToInt() + margin
                    val fitsBelow = below + card.height <= constraints.maxHeight - margin
                    y = if (fitsBelow) below else (bounds.top.roundToInt() - margin - card.height).coerceAtLeast(margin)
                    val start = if (layoutDirection == LayoutDirection.Ltr) bounds.left.roundToInt() else bounds.right.roundToInt() - card.width
                    x = start.coerceIn(margin, (constraints.maxWidth - margin - card.width).coerceAtLeast(margin))
                    pivot[0] = ((bounds.center.x - x) / card.width).coerceIn(0f, 1f)
                    pivot[1] = if (fitsBelow) 0f else 1f
                }
                card.placeRelative(x, y)
            }
        }
    }
}

/** The card arrives from this scale, as iOS 26's menus and popovers grow from their source. */
private const val CardScale = 0.94f
private val Margin = 8.dp
