package com.base.app.core.designsystem.component.overlay

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.content.AppSurface
import com.base.app.core.designsystem.foundation.draw.disabledAlpha
import com.base.app.core.designsystem.foundation.draw.drawHairline
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A menu anchored to whatever composes it, such as a pull-down from a button. It grows out of its
 * anchor, keeps the screen's gutter rather than touching an edge, and opens upward when there is no
 * room below. It is its own window, so it sits over everything, and it animates out before it goes.
 * Choosing an item asks for it to close through [onDismissRequest]. For a menu behind a long press
 * or a right click, use [AppContextMenu].
 *
 * ```
 * Box {
 *     AppIconButton(AppIcons.MoreHorizontal, more, onClick = { open = true })
 *     AppMenu(expanded = open, onDismissRequest = { open = false }) {
 *         AppMenuItem(rename, onClick = onRename)
 *     }
 * }
 * ```
 */
@Composable
public fun AppMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = expanded
    if (!state.currentState && !state.targetState) return
    // A popup is a window of its own, over any cover in the app's: while the window's overlays are
    // suspended the menu is not shown, and it comes back open when they return.
    if (LocalOverlayHost.current?.suspended == true) return

    val density = LocalDensity.current
    val position = remember(density) {
        MenuPosition(margin = with(density) { AppMenuDefaults.WindowMargin.roundToPx() }, gap = with(density) { AppMenuDefaults.AnchorGap.roundToPx() })
    }
    val springs = AppTheme.springs
    val dismiss by rememberUpdatedState(onDismissRequest)
    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        val transition = rememberTransition(state, label = "menu")
        val shown = transition.animateFloat(transitionSpec = { springs.dialog.unit }, label = "menuShown") { if (it) 1f else 0f }
        MenuPanel(
            modifier = modifier.graphicsLayer {
                // The panel's own layer: its alpha costs the menu's size, never the window's.
                val p = shown.value
                alpha = p.coerceIn(0f, 1f)
                scaleX = MenuStartScale + (1f - MenuStartScale) * p
                scaleY = scaleX
                transformOrigin = position.origin
            },
            onChoose = { dismiss() },
            content = content,
        )
    }
}

/**
 * A row in a menu. A [destructive] row is red, glyph and all. It highlights under the finger, and
 * choosing it runs [onClick], then closes the menu it is in.
 */
@Composable
public fun AppMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val tint = if (destructive) AppTheme.colors.danger.content else AppTheme.colors.contentPrimary
    val chosen = LocalMenuItemChosen.current
    Row(
        modifier = modifier
            .pressable(
                onClick = {
                    onClick()
                    chosen()
                },
                feedback = PressFeedback.Highlight,
                enabled = enabled,
            )
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

/** Ends a group of items: the gap iOS leaves between groups, where items have a hairline. */
@Composable
public fun AppMenuDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .then(GroupGap)
            .fillMaxWidth()
            .height(AppMenuDefaults.GroupGapHeight)
            .background(AppTheme.colors.fill),
    )
}

/** What choosing an item does besides its own action: closing the menu it sits in. */
internal val LocalMenuItemChosen = staticCompositionLocalOf<() -> Unit> { {} }

/** The menu's card: items edge to edge, a hairline between neighbours, a gap between groups. */
@Composable
internal fun MenuPanel(
    onChoose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AppSurface(
        modifier = modifier.widthIn(min = AppMenuDefaults.MinWidth, max = AppMenuDefaults.MaxWidth),
        shape = AppTheme.shapes.md,
        color = AppTheme.colors.surfaceElevated,
        elevation = AppTheme.elevation.overlay,
        clipContent = true,
    ) {
        CompositionLocalProvider(LocalMenuItemChosen provides onChoose) {
            MenuColumn(content)
        }
    }
}

/**
 * Stacks the items and draws a hairline between two neighbouring items, never next to a group gap
 * nor at the ends, so items need not know where they sit.
 */
@Composable
private fun MenuColumn(content: @Composable () -> Unit) {
    val separator = AppTheme.colors.separator
    var placedEdges by remember { mutableStateOf(IntArray(0)) }
    Layout(
        content = content,
        modifier = Modifier.drawBehind {
            val edges = placedEdges
            for (y in edges) drawHairline(separator, 0f, size.width, y - 1f)
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val width = placeables.maxOfOrNull { it.width }?.coerceIn(constraints.minWidth, constraints.maxWidth) ?: constraints.minWidth
        val gaps = measurables.map { it.parentData === GroupGap }
        val edges = IntArray(placeables.size)
        var count = 0
        var y = 0
        placeables.forEachIndexed { index, placeable ->
            if (index > 0 && !gaps[index] && !gaps[index - 1]) edges[count++] = y
            y += placeable.height
        }
        val separators = edges.copyOf(count)
        if (!separators.contentEquals(placedEdges)) placedEdges = separators
        layout(width, y.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var top = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(0, top)
                top += placeable.height
            }
        }
    }
}

/** Marks a child of the menu as a group gap. */
private object GroupGap : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = GroupGap
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

/**
 * The menu's width, its distance from the window's edges and from its anchor, and the gap between
 * its groups.
 */
public object AppMenuDefaults {
    public val WindowMargin: Dp = 16.dp
    public val AnchorGap: Dp = 6.dp
    public val MinWidth: Dp = 200.dp
    public val MaxWidth: Dp = 280.dp
    public val GroupGapHeight: Dp = 8.dp
}

internal const val MenuStartScale = 0.85f

@AppComponentPreviews
@Composable
private fun AppMenuPreview() {
    Box(modifier = Modifier.background(AppTheme.colors.background).fillMaxWidth().height(AppTheme.sizes.rowHeight * 7)) {
        AppMenu(expanded = true, onDismissRequest = {}) {
            AppMenuItem(text = "Edit", onClick = {}, icon = AppIcons.Edit)
            AppMenuItem(text = "Duplicate to another workspace", onClick = {}, icon = AppIcons.Copy)
            AppMenuItem(text = "Export", onClick = {}, enabled = false)
            AppMenuDivider()
            AppMenuItem(text = "Delete", onClick = {}, icon = AppIcons.Trash, destructive = true)
        }
    }
}
