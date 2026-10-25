package com.base.app.core.designsystem.foundation.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.testTag
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.content.AppSurface
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/**
 * The stage liquid glass is drawn on: [content] is recorded once per frame into the scene that
 * the floating [chrome] above it samples.
 *
 * Only the tab bar's group (the bar, its accessory and its trailing capsule) samples the scene,
 * and it must sit in [chrome]: a surface inside the recorded content would sample a recording of
 * itself, which crashes the render thread, so glass inside a screen is drawn with
 * [Modifier.glassSurface] and samples nothing.
 *
 * Nothing is recorded unless [quality] samples the scene. A tier change swaps modifiers only, so
 * the content keeps its composition, its state and its scroll positions. The stage carries the
 * test tag `glass_stage`, which the performance scripts look for before they trust a glass
 * measurement.
 *
 * @param background what the window shows where the content is transparent, such as during a
 *   screen transition. Each sample is filled with it before the scene is drawn into it, so a blur
 *   never pulls transparent pixels in at an edge.
 */
@Composable
fun GlassStage(
    background: Color,
    chrome: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    quality: GlassQuality = LocalGlassQuality.current,
    content: @Composable () -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val currentBackground by rememberUpdatedState(background)
    val scene = remember(layer) { SceneBackdrop(layer) { currentBackground } }
    val samples = quality.samplesScene
    Box(modifier.testTag(StageTag)) {
        Box(Modifier.fillMaxSize().then(if (samples) Modifier.recordScene(scene) else Modifier)) {
            content()
        }
        CompositionLocalProvider(LocalGlassScene provides if (samples) scene else null) {
            chrome()
        }
    }
}

/** The scene chrome samples; null outside a stage's chrome, and in tiers that sample nothing. */
internal val LocalGlassScene = staticCompositionLocalOf<SceneBackdrop?> { null }

private const val StageTag = "glass_stage"

@AppComponentPreviews
@Composable
private fun GlassStagePreview() {
    GlassStage(
        background = AppTheme.colors.background,
        modifier = Modifier.fillMaxWidth().height(AppTheme.sizes.rowHeight * 5),
        chrome = {
            AppSurface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(AppTheme.spacing.lg),
                shape = AppTheme.shapes.pill,
                color = AppTheme.colors.surfaceElevated,
            ) {
                AppText(text = "Floating chrome", modifier = Modifier.padding(AppTheme.spacing.md))
            }
        },
    ) {
        Column {
            AppRow(title = "Content runs under the chrome")
            AppRow(title = "Second row")
            AppRow(title = "Third row")
        }
    }
}
