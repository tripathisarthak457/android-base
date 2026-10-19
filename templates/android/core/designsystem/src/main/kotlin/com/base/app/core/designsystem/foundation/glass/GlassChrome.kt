package com.base.app.core.designsystem.foundation.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * The surface of one piece of floating chrome in a [GlassStage]'s chrome slot: the bar, its
 * accessory, its trailing capsule. Where the tier samples the scene it is the scene behind it,
 * blurred and saturated, bent inward along its edge at [GlassQuality.Live], under a light tint and
 * a rim of light; otherwise it is frosted [glassSurface]. Either way the shadow is cached and
 * drawn outside the sampled layer, so it is never re-rendered when the scene changes. Not for the
 * [GlassQuality.Solid] tier, which draws the style's own bar.
 *
 * The chain is remembered: Kyant compares a new shape provider on every call by identity, so a
 * chain rebuilt in each recomposition would rebuild the effect chain and re-render the backdrop.
 */
@Composable
internal fun rememberGlassChrome(shape: Shape = GlassDefaults.ChromeShape): Modifier {
    val scene = LocalGlassScene.current
    val refracts = LocalGlassQuality.current.refracts
    val colors = if (scene != null) GlassDefaults.chromeColors() else GlassDefaults.frostedColors()
    val shadow = GlassDefaults.chromeShadow()
    return remember(scene, refracts, shape, colors, shadow) {
        if (scene == null) {
            Modifier.glassSurface(shape, colors, shadow)
        } else {
            val tint = colors.body
            Modifier
                .glassEdges(shape, colors, shadow, rim = false)
                .rimLight(shape, GlassDefaults.ChromeRim)
                .drawBackdrop(
                    backdrop = scene,
                    shape = { shape },
                    effects = if (refracts) ChromeEffects else ChromeEffectsNoLens,
                    highlight = null,
                    shadow = null,
                    onDrawSurface = { drawRect(tint) },
                )
        }
    }
}

/**
 * Kyant rebuilds its effect chain whenever a state read inside `effects` changes, so the chains
 * read none: a top-level value is built once and compares equal on every recomposition. The
 * refraction band is wider than the blur's padding, so the recording stays the chrome's own size.
 */
private val ChromeEffects: BackdropEffectScope.() -> Unit = {
    vibrancy()
    blur(GlassDefaults.ChromeBlur.toPx())
    lens(GlassDefaults.ChromeRefractionHeight.toPx(), GlassDefaults.ChromeRefractionAmount.toPx())
}

/** The bar on a tier that runs no runtime shader: the blur and the saturation alone. */
private val ChromeEffectsNoLens: BackdropEffectScope.() -> Unit = {
    vibrancy()
    blur(GlassDefaults.ChromeBlur.toPx())
}
