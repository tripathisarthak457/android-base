package com.base.app.core.designsystem.component.overlay

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.content.AppSurface
import com.base.app.core.designsystem.foundation.draw.HairlineEdge
import com.base.app.core.designsystem.foundation.draw.disabledAlpha
import com.base.app.core.designsystem.foundation.draw.hairline
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.theme.AppTheme

/** One choice in an [AppActionSheet]. */
@Immutable
class SheetAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
)

/**
 * "What would you like to do with this?", as iOS asks it: a group of choices rising from the bottom
 * with Cancel apart beneath them. It renders in the window's overlay layer, above the tab bar. A
 * choice runs before the sheet asks to close, so a caller that clears its state on dismiss still has
 * it when the choice runs.
 */
@Composable
fun AppActionSheet(
    visible: Boolean,
    actions: List<SheetAction>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    cancelLabel: String = stringResource(R.string.designsystem_cancel),
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState) return
    val dismiss by rememberUpdatedState(onDismissRequest)
    OverlayBackHandler(enabled = state.targetState) { dismiss() }

    HostedOverlay(modal = true) {
        val springs = AppTheme.springs
        val colors = AppTheme.colors
        val transition = rememberTransition(state, label = "actionSheet")
        val shown = transition.animateFloat(transitionSpec = { springs.dialog.unit }, label = "actionSheetShown") { if (it) 1f else 0f }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind { drawRect(colors.scrim, alpha = shown.value.coerceIn(0f, 1f)) }
                    .pointerInput(Unit) { detectTapGestures { dismiss() } },
            )
            Column(
                modifier = modifier
                    .widthIn(max = AppTheme.layout.sheetMaxWidth)
                    .fillMaxWidth()
                    .graphicsLayer { translationY = (1f - shown.value) * size.height }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(AppTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                AppSurface(modifier = Modifier.fillMaxWidth(), shape = AppTheme.shapes.lg, color = colors.surfaceElevated, clipContent = true) {
                    Column {
                        if (title != null || message != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.md),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                title?.let { AppText(text = it, style = AppTheme.typography.footnote.copy(fontWeight = FontWeight.SemiBold), color = colors.contentSecondary, textAlign = TextAlign.Center) }
                                message?.let { AppText(text = it, style = AppTheme.typography.footnote, color = colors.contentSecondary, textAlign = TextAlign.Center) }
                            }
                        }
                        actions.forEachIndexed { index, action ->
                            ActionRow(
                                label = action.label,
                                icon = action.icon,
                                color = if (action.destructive) colors.danger.content else colors.accentContent,
                                enabled = action.enabled,
                                divider = index > 0 || title != null || message != null,
                                onClick = {
                                    action.onClick()
                                    dismiss()
                                },
                            )
                        }
                    }
                }
                AppSurface(modifier = Modifier.fillMaxWidth(), shape = AppTheme.shapes.lg, color = colors.surfaceElevated, clipContent = true) {
                    ActionRow(label = cancelLabel, icon = null, color = colors.accentContent, enabled = true, divider = false, bold = true, onClick = dismiss)
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    label: String,
    icon: ImageVector?,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    divider: Boolean,
    onClick: () -> Unit,
    bold: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (divider) Modifier.hairline(HairlineEdge.Top) else Modifier)
            .pressable(onClick = onClick, feedback = PressFeedback.Highlight, enabled = enabled)
            .disabledAlpha(enabled)
            .heightIn(min = RowHeight)
            .padding(horizontal = AppTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { AppIcon(it, contentDescription = null, tint = color) }
        AppText(
            text = label,
            style = if (bold) AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold) else AppTheme.typography.body,
            color = color,
        )
    }
}

private val RowHeight = 56.dp
