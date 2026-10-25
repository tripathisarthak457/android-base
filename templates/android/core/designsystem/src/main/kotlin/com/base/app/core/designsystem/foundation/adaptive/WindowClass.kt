package com.base.app.core.designsystem.foundation.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import com.base.app.core.designsystem.theme.LocalAppSpacing

internal val LocalWindowClass = staticCompositionLocalOf { SizeClass.Phone }
internal val LocalContainerClass = staticCompositionLocalOf { SizeClass.Phone }
internal val LocalAppLayout = staticCompositionLocalOf { AppLayout.forClass(SizeClass.Phone) }
internal val LocalWindowPosture = staticCompositionLocalOf { WindowPosture() }
internal val LocalInputTraits = staticCompositionLocalOf { InputTraits() }

/** The window's exact size, for lambdas that run in layout or drawing. See [WindowMetrics]. */
public val LocalWindowMetrics: ProvidableCompositionLocal<WindowMetrics> = staticCompositionLocalOf { PhoneMetrics }

/**
 * The window's size class, recomputed as the window changes size but changing only when a
 * breakpoint is crossed, so the theme and everything under it recompose then and only then.
 */
@Composable
internal fun rememberWindowClass(): SizeClass {
    val info = LocalWindowInfo.current
    val density = LocalDensity.current
    val windowClass by remember(info, density) {
        derivedStateOf {
            val size = info.containerSize
            if (size.width <= 0 || size.height <= 0) {
                SizeClass.Phone
            } else {
                with(density) { SizeClass.of(size.width.toDp().value, size.height.toDp().value) }
            }
        }
    }
    return windowClass
}

@Composable
internal fun rememberWindowMetrics(): WindowMetrics {
    val info = LocalWindowInfo.current
    val density = LocalDensity.current
    return remember(info, density) {
        WindowMetrics { with(density) { info.containerSize.toSize().toDpSize() } }
    }
}

/**
 * Tells everything in [content] that it lays out in a pane [width] wide rather than in the whole
 * window, so components size and present themselves for the pane: a list pane on a tablet gets
 * phone-sized rows and phone-sized gutters. The height class stays the window's.
 */
@Composable
public fun ProvideContainerWidth(width: Dp, content: @Composable () -> Unit) {
    val window = LocalWindowClass.current
    val container = SizeClass.of(width.value, 0f).width.let { SizeClass(it, window.height) }
    val layout = remember(container) { AppLayout.forClass(container) }
    val spacing = LocalAppSpacing.current
    CompositionLocalProvider(
        LocalContainerClass provides container,
        LocalAppLayout provides layout,
        LocalAppSpacing provides remember(spacing, layout) { spacing.copy(gutter = layout.gutter) },
        content = content,
    )
}
