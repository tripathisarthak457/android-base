package com.base.app.core.designsystem.foundation.glass

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangularShape

/**
 * The lens itself: the scene, plus whatever [backdrop] combines with it, magnified and bent at the
 * edge of [shape]. Only for [GlassQuality.Live].
 *
 * It shows only while [LiquidLensState.samplesBackdrop]: under a finger, and while a selection
 * carries it. The rest of the time it is skipped whole and samples nothing, and the selection is
 * the bar's drawn indicator, which the lens covers when it appears. As
 * [LiquidLensState.intensity] rises, [surface] (the resting selection's colour) fades out over
 * the sample and the sample is magnified about the lens's centre; the lens swells and stretches
 * along [orientation] through its own layer ([LiquidLensState.scale]). Nothing outside the lens's
 * layer and draw reads any of it, and the effect chain is built once: glass appears by
 * refraction, never by an alpha over a backdrop. While hidden, the layer and draw read nothing
 * but [LiquidLensState.samplesBackdrop], so a lens that has handed over is not redrawn while it
 * settles.
 *
 * Kyant's lens shader only knows rounded rectangles and throws on any other shape from Tiramisu
 * on, hence the shape's type. Build it inside `remember`, as [rememberGlassChrome] does.
 */
internal fun Modifier.liquidLens(
    backdrop: Backdrop,
    shape: RoundedRectangularShape,
    state: LiquidLensState,
    orientation: Orientation,
    surface: ColorProducer,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = LensEffects,
    highlight = null,
    shadow = null,
    layerBlock = {
        if (state.samplesBackdrop) {
            val factor = state.scale(orientation)
            scaleX = factor.scaleX
            scaleY = factor.scaleY
        } else {
            // The render thread skips a node with zero alpha outright, so a lens at rest costs no
            // offscreen pass. Only 0 or 1: anything between would composite the sample through
            // an alpha layer.
            alpha = 0f
        }
    },
    onDrawBackdrop = { drawSample ->
        if (state.samplesBackdrop) {
            // LensEffects pads nothing (the refraction band is wider than the blur), so the
            // recording's centre is the lens's.
            scale(1f + LensMagnification * state.intensity, pivot = center) { drawSample() }
        }
    },
    onDrawSurface = {
        val glass = if (state.samplesBackdrop) state.intensity else 0f
        drawRect(surface(), alpha = 1f - glass)
    },
)

private val LensEffects: BackdropEffectScope.() -> Unit = {
    vibrancy()
    // allow(offscreen-layer): the lens blurs the content under the moving pill and nowhere else
    blur(LiquidLensDefaults.Blur.toPx())
    lens(LiquidLensDefaults.RefractionHeight.toPx(), LiquidLensDefaults.RefractionAmount.toPx())
}

private const val LensMagnification = 0.12f

/** How much the lens blurs, and how deep and strong its refraction is at the rim. */
internal object LiquidLensDefaults {
    val Blur = 2.dp
    val RefractionHeight = 12.dp
    val RefractionAmount = 20.dp
}
