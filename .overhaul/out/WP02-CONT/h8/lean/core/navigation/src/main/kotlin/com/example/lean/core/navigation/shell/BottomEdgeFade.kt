package com.example.lean.core.navigation.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.lean.core.designsystem.component.navigation.AppTabBarDefaults
import com.example.lean.core.designsystem.foundation.layout.LocalBottomChrome
import com.example.lean.core.designsystem.theme.AppTheme
import com.example.lean.core.designsystem.theme.BarTreatment
import com.example.lean.core.navigation.ShellState
import com.example.lean.core.navigation.stack.coverScrimAlpha

/**
 * The scroll edge under the tab bar: content fading toward the bottom edge as it passes under the
 * bar, so cards under the capsule and the home indicator do not cross its outline. One gradient
 * rectangle between the content and the bar, as iOS's scroll edge effect does; no blur and no
 * offscreen mask.
 *
 * Under a floating bar it fades into the background of the screen in front; above a docked bar it
 * fades into the bar's surface, so content dissolves into the bar instead of meeting a hard edge.
 * Like the docked bar's surface it shows only while content runs under the bar ([scrolledUnder]),
 * so a list resting on its end keeps its last card clear.
 *
 * It belongs to the bottom of the screen that shows the bar, so it travels with that screen the
 * way the bar does ([followCovered]), and it is gone while the keyboard is up.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BottomEdgeFade(
    state: ShellState,
    parallax: Float,
    scrolledUnder: () -> Float,
    modifier: Modifier = Modifier,
) {
    val chrome = LocalBottomChrome.current
    val colors = AppTheme.colors
    val docked = AppTheme.style.bar != BarTreatment.Floating
    val barSurface = AppTabBarDefaults.surface()
    val ime = WindowInsets.ime
    Box(
        modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val height = (chrome.value + Tail).roundToPx().coerceAtMost(constraints.maxHeight)
                val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .followCovered(state.barAnchor, parallax, coverScrimAlpha(colors.isLight))
            .drawWithCache {
                val tail = Tail.toPx().coerceAtMost(size.height)
                val brush = if (docked) {
                    Brush.verticalGradient(0f to barSurface.copy(alpha = 0f), 1f to barSurface, endY = tail)
                } else {
                    val background = (state.tabFront?.background ?: Color.Unspecified).takeOrElse { colors.background }
                    Brush.verticalGradient(
                        0f to background.copy(alpha = 0f),
                        MID_STOP to background.copy(alpha = MID_ALPHA),
                        1f to background.copy(alpha = END_ALPHA),
                    )
                }
                // Above a docked bar only the tail is drawn: the bar paints its own surface below it.
                val area = if (docked) Size(size.width, tail) else size
                onDrawBehind {
                    val shown = scrolledUnder()
                    if (shown > 0f && ime.getBottom(this) == 0) drawRect(brush, Offset.Zero, area, alpha = shown)
                }
            },
    )
}

/**
 * How far above the bar the fade begins: no deeper than the gutter lists end with above the bar,
 * so a list resting on its end is never veiled where its screen does not report its scroll edge.
 */
private val Tail = 16.dp
private const val MID_STOP = 0.55f
private const val MID_ALPHA = 0.78f
private const val END_ALPHA = 0.94f
