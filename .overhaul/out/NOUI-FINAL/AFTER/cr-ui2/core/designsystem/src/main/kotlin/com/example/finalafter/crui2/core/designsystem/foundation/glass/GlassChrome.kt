package com.example.finalafter.crui2.core.designsystem.foundation.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy

/**
 * The surface of one piece of floating chrome in a [GlassStage]'s chrome slot: the bar, its
 * accessory, its trailing capsule. Where the tier samples the scene it is the scene behind it,
 * blurred and saturated, under a light tint; otherwise it is frosted [glassSurface]. Either way
 * it has the same rim and the same cached shadow, drawn outside the sampled layer so that
 * neither is re-rendered when the scene changes. Not for the [GlassQuality.Solid] tier, which
 * draws the style's own bar.
 *
 * The chain is remembered: Kyant compares a new shape provider on every call by identity, so a
 * chain rebuilt in each recomposition would rebuild the effect chain and re-render the backdrop.
 */
@Composable
internal fun rememberGlassChrome(shape: Shape = GlassDefaults.ChromeShape): Modifier {
    val scene = LocalGlassScene.current
    val colors = if (scene != null) GlassDefaults.chromeColors() else GlassDefaults.frostedColors()
    val shadow = GlassDefaults.chromeShadow()
    return remember(scene, shape, colors, shadow) {
        if (scene == null) {
            Modifier.glassSurface(shape, colors, shadow)
        } else {
            val tint = colors.body
            Modifier
                .glassEdges(shape, colors, shadow)
                .drawBackdrop(
                    backdrop = scene,
                    shape = { shape },
                    effects = ChromeEffects,
                    highlight = null,
                    shadow = null,
                    onDrawSurface = { drawRect(tint) },
                )
        }
    }
}

/**
 * Kyant rebuilds its effect chain whenever a state read inside `effects` changes, so the chain
 * reads none: a top-level value is built once and compares equal on every recomposition.
 */
private val ChromeEffects: BackdropEffectScope.() -> Unit = {
    vibrancy()
    // allow(offscreen-layer): glass is floating chrome's blur of what is behind it, nowhere else
    blur(GlassDefaults.ChromeBlur.toPx())
}
