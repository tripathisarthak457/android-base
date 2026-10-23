package io.example.fitcnetdb.core.designsystem.foundation.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import io.example.fitcnetdb.core.designsystem.theme.AppTheme

/**
 * Fills the width up to [maxWidth], then stops and centres. Content that stretches across a tablet
 * or a desktop window is content nobody can read: lines too long to follow, buttons a room apart.
 *
 * ```
 * Column(Modifier.centeredMaxWidth(AppTheme.layout.formMaxWidth)) { … }
 * ```
 */
fun Modifier.centeredMaxWidth(maxWidth: Dp): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = maxWidth)

/**
 * Whether a screen with two halves — a picture and its words, a list and its detail — should put
 * them side by side. True from Expanded width, and on anything Medium or wider that is short: a
 * phone held sideways, where stacking the halves would push the second one off screen.
 */
val shouldUseTwoPanes: Boolean
    @Composable get() {
        val container = AppTheme.containerClass
        return container.hasRoomForTwoPanes || (container.isShort && container.width >= WindowWidth.Medium)
    }

/**
 * Two halves of one screen, side by side when [shouldUseTwoPanes] says there is room, stacked
 * otherwise. [firstWeight] is the first half's share of the width when they are side by side.
 *
 * On a foldable half open across this pane, the hinge decides instead: held like a book, the halves
 * sit on either side of it; lying in tabletop, the first is above the fold and the second below,
 * so neither half is ever split by the crease.
 */
@Composable
fun AppTwoPane(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    firstWeight: Float = 0.5f,
    sideBySide: Boolean = shouldUseTwoPanes,
    spacing: Dp = AppTheme.spacing.xl,
) {
    val posture = AppTheme.posture
    val hinge = posture.hinge
    if (hinge != null && posture.kind != WindowPosture.Kind.Flat) {
        HingeSplit(first, second, hinge, vertical = posture.kind == WindowPosture.Kind.Book, modifier)
        return
    }
    if (sideBySide) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(firstWeight)) { first() }
            Box(modifier = Modifier.weight(1f - firstWeight)) { second() }
        }
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
            first()
            second()
        }
    }
}

/**
 * The two halves laid out around [hinge], which is in window pixels. The pane's own place in the
 * window is read once it is placed, along the hinge's axis only, so a fold or unfold costs one more
 * layout and a scroll across the hinge costs nothing. A hinge that misses the pane leaves the
 * halves meeting in its middle.
 */
@Composable
private fun HingeSplit(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    hinge: Rect,
    vertical: Boolean,
    modifier: Modifier,
) {
    var origin by remember { mutableFloatStateOf(0f) }
    Layout(
        contents = listOf(first, second),
        modifier = modifier.onPlaced { origin = it.positionInWindow().let { at -> if (vertical) at.x else at.y } },
    ) { (firstSlot, secondSlot), constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val start = (if (vertical) hinge.left else hinge.top) - origin
        val end = (if (vertical) hinge.right else hinge.bottom) - origin
        val extent = if (vertical) width else height
        val split = start > 0f && end < extent && constraints.hasBoundedWidth && constraints.hasBoundedHeight
        val before = if (split) start.toInt() else extent / 2
        val after = if (split) end.toInt() else extent / 2
        // The hinge is fixed in the window, so the halves are placed by absolute side: in a
        // right-to-left layout the first half is the one on the right.
        val firstLeads = !vertical || layoutDirection == LayoutDirection.Ltr
        val leading = (if (firstLeads) firstSlot else secondSlot).first().measure(
            if (vertical) Constraints.fixed(before, height) else Constraints.fixed(width, before),
        )
        val trailing = (if (firstLeads) secondSlot else firstSlot).first().measure(
            if (vertical) Constraints.fixed(width - after, height) else Constraints.fixed(width, height - after),
        )
        layout(width, height) {
            leading.place(0, 0)
            if (vertical) trailing.place(after, 0) else trailing.place(0, after)
        }
    }
}
