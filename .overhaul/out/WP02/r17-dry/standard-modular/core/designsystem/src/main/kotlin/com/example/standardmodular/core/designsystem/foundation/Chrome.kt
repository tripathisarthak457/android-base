package com.example.standardmodular.core.designsystem.foundation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Height of floating chrome over one edge. The holder never changes; its value is snapshot state written with
 * settled heights, so a change recomposes only the code that reads it rather than every screen below the provider.
 */
@Stable
class ChromeInset {
    var value: Dp by mutableStateOf(0.dp)
}

/**
 * Chrome over the bottom edge: the tab bar, its accessory, or the keyboard when it hides the bar. One holder per
 * window, provided by AppTheme and written only by the shell, so a toast host above the shell can read it too.
 */
val LocalBottomChrome = staticCompositionLocalOf { ChromeInset() }

/**
 * Chrome over the top edge of the current screen: its top bar. Every AppScaffold provides its own holder, so two
 * screens composed during a push or pop never write the same state.
 */
val LocalTopChrome = staticCompositionLocalOf { ChromeInset() }

/**
 * False inside a kept tab or a covered entry that is not on screen. Ambient animations (shimmer, spinners, anything
 * infinite) stop while it is false, so hidden content costs nothing per frame.
 */
val LocalTabVisible = compositionLocalOf { true }

/** [base] plus the chrome at both ends, for a scrolling container's contentPadding. */
@Composable
fun chromeContentPadding(base: PaddingValues = PaddingValues(0.dp)): PaddingValues {
    val top = LocalTopChrome.current
    val bottom = LocalBottomChrome.current
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = base.calculateStartPadding(direction),
        top = base.calculateTopPadding() + top.value,
        end = base.calculateEndPadding(direction),
        bottom = base.calculateBottomPadding() + bottom.value,
    )
}
