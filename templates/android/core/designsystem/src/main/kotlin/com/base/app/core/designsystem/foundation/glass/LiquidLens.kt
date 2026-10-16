package com.base.app.core.designsystem.foundation.glass

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangularShape
import kotlin.math.max

/**
 * The lens itself: the scene, plus whatever [backdrop] combines with it (the tinted tabs), bent
 * at the edge of [shape] with its colours split at the rim, magnified, under a rim of light, with
 * a shadow inside its top edge and another cast on the bar beneath. Only for [GlassQuality.Live].
 *
 * It shows only while [LiquidLensState.samplesBackdrop]: under a finger, and while a selection
 * carries it. The rest of the time it is skipped whole and samples nothing, and the selection is
 * the bar's drawn indicator, which the lens covers when it appears. As
 * [LiquidLensState.intensity] rises, [surface] (the resting selection's colour) fades out over
 * the sample, the sample is magnified about the lens's centre, and the rim light, the inner
 * shadow and the cast shadow deepen; the lens swells and stretches along [orientation] through
 * its own layer ([LiquidLensState.scale]). Nothing outside the lens's layer and draw reads any of
 * it, and the effect chain is built once: glass appears by refraction, never by an alpha over a
 * backdrop. While hidden, the layer and draw read nothing but [LiquidLensState.samplesBackdrop],
 * so a lens that has handed over is not redrawn while it settles.
 *
 * These are the Kyant catalog's lens effects (refraction with dispersion, a highlight, an inner
 * and an outer shadow, a swell with a stretch), made static: the catalog scales its refraction
 * with the press and rebuilds the shader chain every frame, and re-records three shadow and
 * highlight layers with it; here the chain is fixed, the shadows and the rim are rasterised once
 * and composited at the frame's intensity, and the press is read in draw.
 *
 * Kyant's lens shader only knows rounded rectangles and throws on any other shape from Tiramisu
 * on, hence the shape's type. Build it inside `remember`, as [rememberGlassChrome] does.
 *
 * @param shadow the shadow the lens casts on the bar once lifted; null draws none.
 */
internal fun Modifier.liquidLens(
    backdrop: Backdrop,
    shape: RoundedRectangularShape,
    state: LiquidLensState,
    orientation: Orientation,
    surface: ColorProducer,
    shadow: Shadow? = null,
): Modifier {
    val band = InnerBand()
    return this
        .graphicsLayer {
            if (state.samplesBackdrop) {
                val factor = state.scale(orientation)
                scaleX = factor.scaleX
                scaleY = factor.scaleY
            } else {
                // The render thread skips a node with zero alpha outright, so a lens at rest costs
                // no offscreen pass. Only 0 or 1: anything between would composite the sample
                // through an alpha layer.
                alpha = 0f
            }
        }
        .then(if (shadow != null) Modifier.glassShadow(shape, shadow) { state.intensity } else Modifier)
        .rimLight(shape, GlassDefaults.LensRim) { state.intensity }
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = LensEffects,
            highlight = null,
            shadow = null,
            onDrawBackdrop = { drawSample ->
                if (state.samplesBackdrop) {
                    // The layer above scales the whole lens; the sample is drawn smaller by the
                    // same factor, so the scene under a swollen lens stays where it is, and then
                    // magnified about the lens's centre. LensEffects pads nothing (the refraction
                    // band is wider than the blur), so the recording's centre is the lens's.
                    val factor = state.scale(orientation)
                    val magnification = 1f + max(PressMagnification * state.pressure, TravelMagnification * state.travel)
                    scale(magnification / factor.scaleX, magnification / factor.scaleY, pivot = center) { drawSample() }
                }
            },
            onDrawSurface = {
                val glass = if (state.samplesBackdrop) state.intensity else 0f
                drawRect(surface(), alpha = 1f - glass)
                if (glass > 0f) drawRect(Color.Black, alpha = ShadeAlpha * glass)
            },
            onDrawFront = {
                if (state.samplesBackdrop) {
                    drawRect(band.brush(size, InnerShadowReach.toPx()), alpha = state.intensity)
                }
            },
        )
}

/**
 * The shadow inside the lens's top edge, as light from above casts it: a band fading from the
 * edge, drawn inside the shape's clip. One brush per size, rebuilt only when the lens is resized.
 */
private class InnerBand {
    private var brush: Brush? = null
    private var size = Size.Unspecified
    private var reach = 0f

    fun brush(size: Size, reach: Float): Brush {
        val brush = brush
        if (brush != null && size == this.size && reach == this.reach) return brush
        this.size = size
        this.reach = reach
        return Brush.verticalGradient(0f to InnerShadowColor, 1f to Color.Transparent, startY = 0f, endY = reach)
            .also { this.brush = it }
    }
}

/**
 * Kyant rebuilds its effect chain whenever a state read inside `effects` changes, so the chain
 * reads none. The refraction height and amount match the catalog's lens at full press; what the
 * eye sees rise with the press is the resting tint fading off it and the magnification.
 */
private val LensEffects: BackdropEffectScope.() -> Unit = {
    vibrancy()
    blur(LensBlur.toPx())
    lens(LensRefractionHeight.toPx(), LensRefractionAmount.toPx(), chromaticAberration = true)
}

/** The catalog's pressed tabs are drawn at 1.2; a travelling lens magnifies less, briefly. */
private const val PressMagnification = 0.2f
private const val TravelMagnification = 0.12f

/** The faint darkening the catalog adds to a pressed lens. */
private const val ShadeAlpha = 0.03f
private val LensBlur = 2.dp
private val LensRefractionHeight = 12.dp
private val LensRefractionAmount = 20.dp

/** The catalog's inner shadow: 8 dp, offset by its radius, so it reaches about twice that. */
private val InnerShadowReach = 16.dp
private val InnerShadowColor = Color.Black.copy(alpha = 0.15f)
