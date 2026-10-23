package io.example.fitcnet.core.designsystem.component.navigation

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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import io.example.fitcnet.core.designsystem.foundation.scroll.AppScrollDefaults
import io.example.fitcnet.core.designsystem.theme.AppTheme

/**
 * The bar stood on its side beside a wide window. Items shrink toward 56 dp before the rail
 * scrolls, so a short window (a phone held sideways, a split screen) still reaches every tab. The
 * trailing action heads the rail and the accessory sits at its foot, so search and a mini-player
 * stay on wide windows.
 */
@Composable
internal fun TabRail(
    items: List<TabItem>,
    selected: Int,
    edges: IndicatorEdges,
    labels: TabLabels,
    look: BarLook,
    bounce: TabBounce,
    onTap: (Int) -> Unit,
    header: (@Composable BarActionScope.() -> Unit)?,
    footer: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    val current = rememberUpdatedState(selected)
    val measure = remember { TabMeasure(vertical = true, state = null, { current.value }, mutableStateOf(PillSlots.Empty)) }
    val pill = remember { PillPaths() }
    // The rail has room for every label; Selected only saves room in the bottom bar.
    val railLabels = if (labels == TabLabels.Never) TabLabels.Never else TabLabels.Always
    Row(modifier = modifier.fillMaxHeight().background(look.surface)) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                // Insets first: a cutout on this side widens the rail instead of squeezing it.
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                .width(AppTabBarDefaults.RailWidth)
                .padding(vertical = AppTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (header != null) {
                Column(
                    modifier = Modifier.padding(bottom = AppTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BarActions(look).header()
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        measure.railViewport = constraints.maxHeight
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .verticalScroll(rememberScrollState(), flingBehavior = AppScrollDefaults.flingBehavior())
                    .focusRestorer()
                    .focusGroup()
                    .selectableGroup()
                    .drawBehind { drawIndicator(pill.fill, measure, edges, look, vertical = true, collapse = 0f) },
            ) {
                TabCells(
                    items, selected, TabLayer.Base, railLabels, look, bounce, measure, onTap,
                    Modifier.clipToPill(pill.outside, measure, edges, look, vertical = true, outside = true, collapse = { 0f }),
                )
                TabCells(
                    items, selected, TabLayer.Selected, railLabels, look, bounce, measure, null,
                    Modifier.clipToPill(pill.inside, measure, edges, look, vertical = true, outside = false, collapse = { 0f }),
                )
            }
            if (footer != null) {
                Box(Modifier.padding(top = AppTheme.spacing.md)) { footer() }
            }
        }
        Box(Modifier.fillMaxHeight().width(1.dp).background(look.border))
    }
}
