package com.example.wp04.dlplay.core.designsystem.component.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.wp04.dlplay.core.designsystem.component.feedback.AppBadgedBox
import com.example.wp04.dlplay.core.designsystem.foundation.HapticEffect
import com.example.wp04.dlplay.core.designsystem.foundation.rememberAppHaptics
import com.example.wp04.dlplay.core.designsystem.theme.AppTheme
import com.example.wp04.dlplay.core.designsystem.theme.BarTreatment
import com.example.wp04.dlplay.core.designsystem.theme.rememberReduceMotion
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The tabs as a sidebar beside a large window, as iPadOS and Google's tablet apps show them: a row
 * per tab with its glyph, label and badge, grouped under the [TabItem.section] headings. [header]
 * heads it (search, the toggle that folds it to a rail) and [footer] sits at its foot.
 *
 * The selection highlight slides to the new row in the draw phase; the rows only change colour on
 * the frame that selects, so a selection recomposes two rows and nothing per frame.
 */
@Composable
fun AppSidebar(
    items: List<TabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onReselect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = AppSidebarDefaults.Width,
    treatment: BarTreatment = AppTheme.style.bar,
    header: (@Composable BarActionScope.() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val look = barLook(treatment)
    val haptics = rememberAppHaptics()
    val reduceMotion = rememberReduceMotion()
    val highlight = remember(items.size) { SidebarHighlight(items.size, selected) }
    LaunchedEffect(highlight, selected) { highlight.moveTo(selected, animate = !reduceMotion) }
    val spacing = AppTheme.spacing
    Row(modifier = modifier.fillMaxHeight().background(look.surface)) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                .width(width)
                .padding(vertical = spacing.lg),
        ) {
            if (header != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.sm).padding(bottom = spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BarActions(look).header()
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .focusRestorer()
                    .focusGroup()
                    .selectableGroup()
                    .padding(horizontal = spacing.sm)
                    .drawBehind {
                        highlight.bounds { top, bottom ->
                            drawRoundRect(
                                color = look.indicator,
                                topLeft = Offset(0f, top),
                                size = Size(size.width, bottom - top),
                                cornerRadius = CornerRadius(HighlightCorner.toPx()),
                            )
                        }
                    },
            ) {
                items.forEachIndexed { index, item ->
                    if (item.section != null && item.section != items.getOrNull(index - 1)?.section) {
                        BasicText(
                            text = item.section,
                            style = AppTheme.typography.labelSmall,
                            color = { look.content },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = spacing.md, top = spacing.lg, bottom = spacing.xs)
                                .semantics { heading() },
                        )
                    }
                    SidebarRow(
                        item = item,
                        selected = index == selected,
                        look = look,
                        modifier = Modifier
                            .testTag("tab_$index")
                            .onPlaced { coordinates ->
                                val top = coordinates.positionInParent().y
                                highlight.place(index, top, top + coordinates.size.height)
                            },
                        onClick = {
                            if (index == selected) {
                                onReselect(index)
                            } else {
                                haptics.perform(HapticEffect.Select)
                                onSelect(index)
                            }
                        },
                    )
                }
            }
            if (footer != null) {
                Box(Modifier.fillMaxWidth().padding(horizontal = spacing.sm).padding(top = spacing.md)) { footer() }
            }
        }
        Box(Modifier.fillMaxHeight().width(1.dp).background(look.border))
    }
}

object AppSidebarDefaults {

    /** The sidebar's width on a large window. */
    val Width: Dp = 280.dp

    /** On an extra-large one, where the extra width reads as breathing room. */
    val WideWidth: Dp = 304.dp
}

@Composable
private fun SidebarRow(item: TabItem, selected: Boolean, look: BarLook, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) look.selectedContent else look.content
    val filter = remember(color) { ColorFilter.tint(color) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowHeight)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab, interactionSource = null, indication = null)
            .padding(horizontal = AppTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppBadgedBox(count = item.badgeCount) {
            Image(
                painter = rememberVectorPainter(if (selected) item.selectedIcon else item.icon),
                contentDescription = null,
                colorFilter = filter,
                modifier = Modifier.size(IconSize),
            )
        }
        BasicText(
            text = item.label,
            style = AppTheme.typography.bodyLarge,
            color = { color },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Where the selection highlight is: a position in rows, sprung toward the selected one, over the
 * row bounds the layout writes. Neither is read in composition.
 */
private class SidebarHighlight(count: Int, selected: Int) {
    private val tops = FloatArray(count)
    private val bottoms = FloatArray(count)
    private val position = Animatable(selected.toFloat())

    fun place(index: Int, top: Float, bottom: Float) {
        tops[index] = top
        bottoms[index] = bottom
    }

    suspend fun moveTo(index: Int, animate: Boolean) {
        if (animate) position.animateTo(index.toFloat(), HighlightSpring) else position.snapTo(index.toFloat())
    }

    inline fun bounds(block: (top: Float, bottom: Float) -> Unit) {
        if (tops.isEmpty()) return
        val p = position.value.coerceIn(0f, tops.lastIndex.toFloat())
        val low = floor(p).toInt()
        val high = ceil(p).toInt()
        val fraction = p - low
        val bottom = lerp(bottoms[low], bottoms[high], fraction)
        if (bottom > 0f) block(lerp(tops[low], tops[high], fraction), bottom)
    }
}

private val RowHeight = 48.dp
private val HighlightCorner = 10.dp

/** iOS's segmented-thumb spring, as the bar's other selection motion: response 0.28 s, no bounce. */
private val HighlightSpring = spring(dampingRatio = 1f, stiffness = 503.5f, visibilityThreshold = 0.001f)
