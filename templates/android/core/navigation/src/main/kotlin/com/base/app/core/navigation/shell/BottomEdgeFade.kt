package com.base.app.core.navigation.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.foundation.layout.LocalBottomChrome
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.stack.coverScrimAlpha

/**
 * The scroll edge under the floating bar: content fading into the background toward the bottom
 * edge, so cards passing under the capsule and the home indicator do not cross its outline. One
 * gradient rectangle between the content and the bar, as iOS's scroll edge effect does; no blur
 * and no offscreen mask.
 *
 * It belongs to the bottom of the screen that shows the bar, so it travels with that screen the
 * way the bar does ([followCovered]), and it is gone while the keyboard is up.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BottomEdgeFade(anchor: BarAnchor, parallax: Float, modifier: Modifier = Modifier) {
    val chrome = LocalBottomChrome.current
    val colors = AppTheme.colors
    val ime = WindowInsets.ime
    Box(
        modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val height = (chrome.value + Tail).roundToPx().coerceAtMost(constraints.maxHeight)
                val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .followCovered(anchor, parallax, coverScrimAlpha(colors.isLight))
            .drawWithCache {
                val background = colors.background
                val brush = Brush.verticalGradient(
                    0f to background.copy(alpha = 0f),
                    MID_STOP to background.copy(alpha = MID_ALPHA),
                    1f to background.copy(alpha = END_ALPHA),
                )
                onDrawBehind {
                    if (ime.getBottom(this) == 0) drawRect(brush)
                }
            },
    )
}

/** How far above the bar the fade begins. */
private val Tail = 24.dp
private const val MID_STOP = 0.55f
private const val MID_ALPHA = 0.78f
private const val END_ALPHA = 0.94f
