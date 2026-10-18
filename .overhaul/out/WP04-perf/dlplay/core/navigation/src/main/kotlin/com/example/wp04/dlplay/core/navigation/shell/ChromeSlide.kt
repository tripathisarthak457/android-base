package com.example.wp04.dlplay.core.navigation.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.wp04.dlplay.core.designsystem.component.navigation.AppTabBarDefaults
import com.example.wp04.dlplay.core.designsystem.theme.rememberReduceMotion
import com.example.wp04.dlplay.core.navigation.NavigationChrome
import com.example.wp04.dlplay.core.navigation.ShellState
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
        val reduceMotion = rememberReduceMotion()
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
@Composable
internal fun Modifier.followSlide(slide: ChromeSlide): Modifier {
    val toward = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    return graphicsLayer { translationX = toward * slide.offset.value }
}

/** Paints the chrome's surface into the gap the content leaves while the sidebar folds. */
@Composable
internal fun Modifier.fillGap(slide: ChromeSlide): Modifier {
    val surface = AppTabBarDefaults.surface()
    return drawBehind {
        val gap = slide.offset.value
        if (gap <= 0f) return@drawBehind
        val left = if (layoutDirection == LayoutDirection.Rtl) -gap else size.width
        drawRect(surface, topLeft = Offset(left, 0f), size = Size(gap, size.height))
    }
}

/** `springs.layout`: response 0.36 s, no bounce. */
private val SlideSpring = spring(dampingRatio = 1f, stiffness = 304.6f, visibilityThreshold = 0.5f)
