package com.base.app.core.designsystem.component.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.foundation.ChromeInset
import com.base.app.core.designsystem.foundation.LocalBottomChrome
import com.base.app.core.designsystem.foundation.LocalTopChrome
import com.base.app.core.designsystem.foundation.centeredMaxWidth
import com.base.app.core.designsystem.theme.AppTheme

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
 * for text and settings, `contentMaxWidth` for mixed content. The sides clear display cutouts,
 * which a phone held sideways puts at one edge; a screen with a full-bleed panel passes
 * [clearDisplayCutout] false and pads that panel's content itself.
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    background: Color = AppTheme.colors.background,
    contentMaxWidth: Dp = Dp.Unspecified,
    clearDisplayCutout: Boolean = true,
    content: @Composable () -> Unit,
) {
    val topChrome = remember { ChromeInset() }
    val bottomChrome = if (bottomBar != null) remember { ChromeInset() } else LocalBottomChrome.current
    val width = if (contentMaxWidth == Dp.Unspecified) Modifier else Modifier.centeredMaxWidth(contentMaxWidth)

    CompositionLocalProvider(LocalTopChrome provides topChrome) {
        SubcomposeLayout(
            modifier = modifier
                .background(background)
                .then(
                    if (clearDisplayCutout) {
                        Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
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
