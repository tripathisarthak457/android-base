package com.base.app.core.designsystem.component.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import kotlin.math.roundToInt

/**
 * A small set of mutually exclusive options, with a pill that slides to the selection.
 *
 * The pill is a single element that moves, rather than a background that appears on the selected
 * option and disappears from the old one. That is the whole effect: the eye follows one object
 * across, which makes the relationship between the two options obvious. A cross-fade between two
 * backgrounds communicates nothing about direction.
 *
 * Segments are equal width, so the pill's position is arithmetic rather than a measurement pass —
 * which is also why a long label truncates rather than stretching its segment and making every
 * other one jump.
 *
 * Use it for two to four options. Beyond that the labels stop fitting and the right control is
 * [com.base.app.core.designsystem.component.navigation.AppTabRow] or a dropdown.
 */
@Composable
fun AppSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (options.isEmpty()) return

    val colors = AppTheme.colors
    val position by animateFloatAsState(
        targetValue = selectedIndex.coerceIn(0, options.lastIndex).toFloat(),
        animationSpec = AppTheme.motion.sheet(),
        label = "segmentPosition",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(AppTheme.shapes.sm)
            .background(colors.surfaceVariant)
            .border(AppTheme.sizes.borderWidth, colors.border, AppTheme.shapes.sm)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layout { measurable, constraints ->
                    val segmentWidth = constraints.maxWidth / options.size
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = segmentWidth, maxWidth = segmentWidth),
                    )
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.placeRelative((position * segmentWidth).roundToInt(), 0)
                    }
                }
                .clip(AppTheme.shapes.xs)
                .background(colors.surface),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                val content by animateColorAsState(
                    targetValue = if (selected) colors.contentPrimary else colors.contentTertiary,
                    animationSpec = tween(AppTheme.motion.quick),
                    label = "segmentContent",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .selectable(
                            selected = selected,
                            enabled = enabled,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = option,
                        style = AppTheme.typography.titleSmall,
                        color = content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }
        }
    }
}
