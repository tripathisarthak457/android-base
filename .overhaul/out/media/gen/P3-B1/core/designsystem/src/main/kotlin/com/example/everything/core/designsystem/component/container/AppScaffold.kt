package com.example.everything.core.designsystem.component.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.example.everything.core.designsystem.component.sheet.LocalSheetSurface
import com.example.everything.core.designsystem.foundation.adaptive.centeredMaxWidth
import com.example.everything.core.designsystem.foundation.layout.ChromeInset
import com.example.everything.core.designsystem.foundation.layout.LocalBottomChrome
import com.example.everything.core.designsystem.foundation.layout.LocalPaintedBackground
import com.example.everything.core.designsystem.foundation.layout.LocalTopChrome
import com.example.everything.core.designsystem.theme.AppTheme

/**
 * The frame every screen sits in. The content fills the whole screen and runs under the top bar
 * and under whatever floats over the bottom edge; a scrolling container starts below the bar and
 * ends above the bottom chrome by padding itself with `chromeContentPadding()`.
 *
 * Each scaffold has its own top inset: its top bar writes its height there while it is measured,
 * before the content is composed, so the content is laid out right on the first frame, and two
 * screens composed together during a push never share a value. A [bottomBar] (an
 * [AppBottomActionBar]) replaces the window's bottom chrome for this content.
 *
 * [contentMaxWidth] stops the content stretching on wide windows and centres it, while the bars
 * and the background still span the window: `formMaxWidth` for a single form, `readableMaxWidth`
 * for text and settings, `contentMaxWidth` for mixed content. The sides clear what a phone held
 * sideways puts at one edge, its display cutout and a three-button navigation bar, so no row ends
 * under them; a screen with a full-bleed panel passes [clearDisplayCutout] false and pads that
 * panel's content itself.
 *
 * [background] is the theme's background by default. Inside a sheet the default paints nothing, so
 * the sheet's material shows, and the sheet keeps the content clear of the cutout itself. Where the
 * screen's host has already painted the same colour under it edge to edge ([LocalPaintedBackground]:
 * a navigation entry's frame, a presented sheet's or cover's), the scaffold paints nothing either, so
 * a screen is filled once per frame, not twice.
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    background: Color = Color.Unspecified,
    contentMaxWidth: Dp = Dp.Unspecified,
    clearDisplayCutout: Boolean = true,
    content: @Composable () -> Unit,
) {
    val topChrome = remember { ChromeInset() }
    val bottomChrome = if (bottomBar != null) remember { ChromeInset() } else LocalBottomChrome.current
    val width = if (contentMaxWidth == Dp.Unspecified) Modifier else Modifier.centeredMaxWidth(contentMaxWidth)
    val sheet = LocalSheetSurface.current
    // In a sheet the sheet's material shows through, and the sheet already keeps clear of the cutout.
    val fill = background.takeOrElse { if (sheet != null) Color.Transparent else AppTheme.colors.background }
        .let { if (it == LocalPaintedBackground.current) Color.Unspecified else it }
    val behindBars = background.takeOrElse { sheet?.container ?: AppTheme.colors.background }

    CompositionLocalProvider(LocalTopChrome provides topChrome, LocalScaffoldBackground provides behindBars) {
        SubcomposeLayout(
            modifier = modifier
                .then(if (fill.isSpecified) Modifier.background(fill) else Modifier)
                .then(
                    if (clearDisplayCutout && sheet == null) {
                        Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    } else {
                        Modifier
                    },
                ),
        ) { constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val bars = subcompose(ScaffoldSlot.TopBar, topBar).map { it.measure(loose) }
            val actions = bottomBar?.let { bar -> subcompose(ScaffoldSlot.BottomBar, bar).map { it.measure(loose) } }.orEmpty()
            if (bottomBar != null) bottomChrome.value = actions.maxOfOrNull { it.height }?.toDp() ?: Dp(0f)

            val body = subcompose(ScaffoldSlot.Content) {
                CompositionLocalProvider(LocalBottomChrome provides bottomChrome) {
                    Box(width) { content() }
                }
            }.map { it.measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight)) }

            layout(constraints.maxWidth, constraints.maxHeight) {
                body.forEach { it.place(0, 0) }
                actions.forEach { it.place(0, constraints.maxHeight - it.height) }
                bars.forEach { it.place(0, 0) }
            }
        }
    }
}

private enum class ScaffoldSlot { TopBar, BottomBar, Content }

/** The colour the current scaffold paints, for a bar that must cover content in the same colour. */
internal val LocalScaffoldBackground = staticCompositionLocalOf { Color.Unspecified }
