package com.base.app.core.navigation.shell

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.Constraints
import com.base.app.core.designsystem.component.navigation.AppTabBar
import com.base.app.core.designsystem.component.navigation.AppTabBarState
import com.base.app.core.designsystem.component.navigation.BarActionScope
import com.base.app.core.designsystem.component.navigation.BarOrientation
import com.base.app.core.designsystem.component.navigation.TabItem
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.ShellConfig
import com.base.app.core.navigation.ShellState
import com.base.app.core.navigation.ShellTab
import com.base.app.core.navigation.stack.coverScrimAlpha

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShellBar(
    tabs: List<ShellTab>,
    state: ShellState,
    barState: AppTabBarState,
    config: ShellConfig,
    trailingAction: (@Composable BarActionScope.() -> Unit)?,
    accessory: (@Composable () -> Unit)?,
    parallax: Float,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    val labels = tabs.map { stringResource(it.label) }
    val items = remember(tabs, labels) {
        tabs.mapIndexed { index, tab -> TabItem(labels[index], tab.icon, tab.selectedIcon, tab.badgeCount) }
    }
    val focus = LocalFocusManager.current
    AppTabBar(
        items = items,
        selected = state.selectedIndex,
        onSelect = { index ->
            // A field focused in the tab being left would keep the keyboard up over the next one.
            focus.clearFocus()
            state.select(index)
        },
        onReselect = state::select,
        modifier = if (vertical) {
            modifier
        } else {
            modifier
                .followCovered(state.anchors[state.selectedIndex], parallax, coverScrimAlpha(AppTheme.colors.isLight))
                .hideUnderKeyboard()
                .parkWhenCovered(state.anchors[state.selectedIndex])
        },
        orientation = if (vertical) BarOrientation.Vertical else BarOrientation.Horizontal,
        labels = config.barLabels,
        minimize = config.barMinimize,
        state = barState,
        trailingAction = trailingAction,
        accessory = accessory,
    )
}

/**
 * The keyboard rises over the bar: the bar moves down with it in the layout phase and is
 * transparent once the keyboard covers it, so it never floats above the keyboard's top edge.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Modifier.hideUnderKeyboard(): Modifier {
    val ime = WindowInsets.ime
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val density = this
        layout(placeable.width, placeable.height) {
            val keyboard = ime.getBottom(density).coerceAtMost(placeable.height)
            placeable.placeWithLayer(0, keyboard) { alpha = if (keyboard >= placeable.height) 0f else 1f }
        }
    }
}

/**
 * Fully covered by a screen that hides it, the bar is parked: still laid out, with its layers, but
 * drawn at alpha 0 off the window, out of reach of touch and out of the semantics tree. A pop or a
 * back gesture that brings it back changes two layer properties; nothing is composed, measured
 * or recorded. All of it follows [anchor] in the layer and semantics phases, so it recomposes
 * nothing either.
 */
private fun Modifier.parkWhenCovered(anchor: BarAnchor): Modifier = this then ParkWhenCoveredElement(anchor)

private data class ParkWhenCoveredElement(val anchor: BarAnchor) : ModifierNodeElement<ParkWhenCoveredNode>() {
    override fun create() = ParkWhenCoveredNode(anchor)

    override fun update(node: ParkWhenCoveredNode) {
        node.anchor = anchor
        node.follow()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "parkWhenCovered"
    }
}

private class ParkWhenCoveredNode(var anchor: BarAnchor) :
    Modifier.Node(), LayoutModifierNode, SemanticsModifierNode, ObserverModifierNode {

    private var parked = false

    private val layer: GraphicsLayerScope.() -> Unit = {
        if (anchor.progress() >= 1f) {
            alpha = 0f
            translationX = -2f * size.width
        }
    }

    override val shouldClearDescendantSemantics: Boolean get() = parked

    override fun SemanticsPropertyReceiver.applySemantics() = Unit

    override fun onAttach() = follow()

    override fun onObservedReadsChanged() = follow()

    fun follow() {
        observeReads {
            val covered = anchor.progress() >= 1f
            if (covered != parked) {
                parked = covered
                invalidateSemantics()
            }
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.placeWithLayer(0, 0, layerBlock = layer) }
    }
}
