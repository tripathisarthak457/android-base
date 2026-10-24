package io.example.audit.core.navigation.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import io.example.audit.core.designsystem.component.navigation.AppTabBarDefaults
import io.example.audit.core.designsystem.theme.AppTheme
import io.example.audit.core.navigation.NavigationChrome
import io.example.audit.core.navigation.ShellState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The sidebar opening or folding. The content takes its new width at once, in one layout, and
 * then slides from where it was to where it is, in its layer: nothing is laid out or composed per
 * frame. Opening, the content starts over the new sidebar and uncovers it; folding, the rail's
 * surface fills the gap the content leaves until it closes.
 */
@Stable
internal class ChromeSlide(private val scope: CoroutineScope) {

    /** Px the content is still away from its place, toward the end side; 0 at rest. */
    val offset = Animatable(0f)

    /** What the sidebar toggle (button or Ctrl+Shift+S) runs on a window wide enough for one. */
    @Composable
    fun toggle(state: ShellState, chrome: NavigationChrome): () -> Unit {
        val delta = with(LocalDensity.current) { (sidebarWidth() - AppTabBarDefaults.RailWidth).toPx() }
        val reduceMotion = AppTheme.reduceMotion
        return remember(state, chrome, delta, reduceMotion) {
            {
                val opening = chrome != NavigationChrome.Sidebar
                state.showSidebar(opening)
                if (!reduceMotion) {
                    scope.launch {
                        offset.snapTo(if (opening) -delta else delta)
                        offset.animateTo(0f, SlideSpring)
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberChromeSlide(): ChromeSlide {
    val scope = rememberCoroutineScope()
    return remember(scope) { ChromeSlide(scope) }
}

/** Moves the content with [slide], mirrored on a right-to-left layout. */
internal fun Modifier.followSlide(slide: ChromeSlide): Modifier = this then FollowSlideElement(slide)

private data class FollowSlideElement(val slide: ChromeSlide) : ModifierNodeElement<FollowSlideNode>() {
    override fun create() = FollowSlideNode(slide)

    override fun update(node: FollowSlideNode) {
        node.slide = slide
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "followSlide"
    }
}

private class FollowSlideNode(var slide: ChromeSlide) : Modifier.Node(), LayoutModifierNode {

    private var toward = 1f

    // Reads the offset in the layer, so a frame of the slide changes one layer property.
    private val layer: GraphicsLayerScope.() -> Unit = { translationX = toward * slide.offset.value }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        toward = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
        return layout(placeable.width, placeable.height) { placeable.placeWithLayer(0, 0, layerBlock = layer) }
    }
}

/** Paints [color], the chrome's surface, into the gap the content leaves while the sidebar folds. */
internal fun Modifier.fillGap(slide: ChromeSlide, color: Color): Modifier = this then FillGapElement(slide, color)

private data class FillGapElement(val slide: ChromeSlide, val color: Color) : ModifierNodeElement<FillGapNode>() {
    override fun create() = FillGapNode(slide, color)

    override fun update(node: FillGapNode) {
        node.slide = slide
        node.color = color
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "fillGap"
    }
}

private class FillGapNode(var slide: ChromeSlide, var color: Color) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        val gap = slide.offset.value
        if (gap > 0f) {
            val left = if (layoutDirection == LayoutDirection.Rtl) -gap else size.width
            drawRect(color, topLeft = Offset(left, 0f), size = Size(gap, size.height))
        }
        drawContent()
    }
}

/** `springs.layout`: response 0.36 s, no bounce. */
private val SlideSpring = spring(dampingRatio = 1f, stiffness = 304.6f, visibilityThreshold = 0.5f)
