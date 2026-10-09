package com.base.app.core.designsystem.component.overlay

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.base.app.core.designsystem.component.container.AppDivider
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.content.AppSurface
import com.base.app.core.designsystem.foundation.draw.disabledAlpha
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A menu anchored to whatever composes it: a pull-down from a button, or a context menu from a long
 * press (`Modifier.pressable(onLongClick = { open = true }, …)`). It grows out of its anchor, keeps
 * the screen's gutter rather than touching an edge, and opens upward when there is no room below.
 * It is its own window, so it sits over everything, and it animates out before it goes.
 *
 * ```
 * Box {
 *     AppIconButton(AppIcons.MoreHorizontal, more, onClick = { open = true })
 *     AppMenu(expanded = open, onDismissRequest = { open = false }) {
 *         AppMenuItem(rename, onClick = { open = false; onRename() })
 *     }
 * }
 * ```
 */
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = expanded
    if (!state.currentState && !state.targetState) return

    val density = LocalDensity.current
    val position = remember(density) {
        MenuPosition(margin = with(density) { 16.dp.roundToPx() }, gap = with(density) { 6.dp.roundToPx() })
    }
    val springs = AppTheme.springs
    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        val transition = rememberTransition(state, label = "menu")
        val shown = transition.animateFloat(transitionSpec = { springs.sheet.unit }, label = "menuShown") { if (it) 1f else 0f }
        AppSurface(
            modifier = modifier
                .widthIn(min = MinWidth, max = MaxWidth)
                .graphicsLayer {
                    val p = shown.value
                    alpha = p.coerceIn(0f, 1f)
                    scaleX = START_SCALE + (1f - START_SCALE) * p
                    scaleY = scaleX
                    transformOrigin = position.origin
                },
            shape = AppTheme.shapes.md,
            color = AppTheme.colors.surfaceElevated,
            elevation = AppTheme.elevation.overlay,
            clipContent = true,
        ) {
            Column(modifier = Modifier.padding(vertical = AppTheme.spacing.xs)) { content() }
        }
    }
}

/** A row in a menu. A [destructive] row is red, glyph and all. It highlights under the finger. */
@Composable
fun AppMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val tint = if (destructive) AppTheme.colors.danger.content else AppTheme.colors.contentPrimary
    Row(
        modifier = modifier
            .pressable(onClick = onClick, feedback = PressFeedback.Highlight, enabled = enabled)
            .disabledAlpha(enabled)
            .heightIn(min = AppTheme.sizes.rowHeight)
            .padding(horizontal = AppTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = text,
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.body,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        icon?.let { AppIcon(it, contentDescription = null, tint = tint) }
    }
}

@Composable
fun AppMenuDivider(modifier: Modifier = Modifier) {
    AppDivider(modifier = modifier.padding(vertical = AppTheme.spacing.xs))
}

/**
 * Opens under its anchor, lined up with the anchor's leading edge, and moves in rather than touch the
 * window's edge; with no room below it opens above. Remembers which corner it grew from, so the menu
 * scales out of the anchor rather than out of its own middle.
 */
private class MenuPosition(private val margin: Int, private val gap: Int) : PopupPositionProvider {
    var origin by mutableStateOf(TransformOrigin(0f, 0f))
        private set

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val ltr = layoutDirection == LayoutDirection.Ltr
        val leading = if (ltr) anchorBounds.left else anchorBounds.right - popupContentSize.width
        val x = leading.coerceIn(margin, (windowSize.width - margin - popupContentSize.width).coerceAtLeast(margin))
        val below = anchorBounds.bottom + gap
        val fitsBelow = below + popupContentSize.height <= windowSize.height - margin
        val y = if (fitsBelow) below else (anchorBounds.top - gap - popupContentSize.height).coerceAtLeast(margin)
        val anchorCentre = (anchorBounds.left + anchorBounds.right) / 2f
        val originX = ((anchorCentre - x) / popupContentSize.width.coerceAtLeast(1)).coerceIn(0f, 1f)
        origin = TransformOrigin(originX, if (fitsBelow) 0f else 1f)
        return IntOffset(x, y)
    }
}

private val MinWidth = 200.dp
private val MaxWidth = 300.dp
private const val START_SCALE = 0.85f
