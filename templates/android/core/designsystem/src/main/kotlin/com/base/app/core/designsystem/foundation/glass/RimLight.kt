package com.base.app.core.designsystem.foundation.glass

import android.graphics.BlurMaskFilter
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil

/**
 * The light a piece of glass catches along its edge: a thin band just inside the outline,
 * brightest where the edge faces the light and fading where it turns away, as the rim of a lens
 * does. The Kyant catalog draws its chrome and its lenses with this light (its `Highlight.Default`:
 * half a dp, white at half strength, lit from 45°); the shader below follows that one's maths.
 *
 * @property width the band's width, inside the edge.
 * @property angleDegrees where the light comes from, clockwise from the right: 45° is the top left.
 * @property falloff how quickly the light fades as the edge turns away; 1 is linear in the cosine.
 */
@Immutable
internal class RimLight(
    val width: Dp = 0.5.dp,
    val color: Color = Color.White.copy(alpha = 0.5f),
    val angleDegrees: Float = 45f,
    val falloff: Float = 1f,
) {
    override fun equals(other: Any?): Boolean = other is RimLight &&
        width == other.width && color == other.color &&
        angleDegrees == other.angleDegrees && falloff == other.falloff

    override fun hashCode(): Int =
        ((width.hashCode() * 31 + color.hashCode()) * 31 + angleDegrees.hashCode()) * 31 + falloff.hashCode()
}

/**
 * Draws [light] over this node's content, added to it, at [alpha] (read in draw).
 *
 * The band is recorded once per size into a layer and composited at the frame's alpha, so a lens
 * whose rim brightens as it lifts changes one layer property per frame and re-records nothing;
 * Kyant's own highlight node re-records its stroke, with a fresh blur mask, on every draw. Below
 * API 33 the band is a plain line, since the falloff is an AGSL shader.
 */
internal fun Modifier.rimLight(shape: Shape, light: RimLight, alpha: () -> Float = Full): Modifier =
    this then RimLightElement(shape, light, alpha)

private val Full: () -> Float = { 1f }

private data class RimLightElement(
    val shape: Shape,
    val light: RimLight,
    val alpha: () -> Float,
) : ModifierNodeElement<RimLightNode>() {
    override fun create() = RimLightNode(shape, light, alpha)

    override fun update(node: RimLightNode) = node.update(shape, light, alpha)

    override fun InspectorInfo.inspectableProperties() {
        name = "rimLight"
        properties["light"] = light
    }
}

private class RimLightNode(
    private var shape: Shape,
    private var light: RimLight,
    private var alpha: () -> Float,
) : Modifier.Node(), DrawModifierNode {

    private var layer: GraphicsLayer? = null
    private val paint = Paint().apply { style = PaintingStyle.Stroke }
    private val clip = Path()
    private var shader: RuntimeShader? = null

    // What the layer holds; it is recorded again only when one of these changes.
    private var recordedSize = Size.Unspecified
    private var recordedDensity = 0f
    private var recordedDirection: LayoutDirection? = null

    fun update(shape: Shape, light: RimLight, alpha: () -> Float) {
        if (shape != this.shape || light != this.light) recordedSize = Size.Unspecified
        this.shape = shape
        this.light = light
        this.alpha = alpha
        invalidateDraw()
    }

    override fun onAttach() {
        layer = requireGraphicsContext().createGraphicsLayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) shader = RuntimeShader(RimShader)
    }

    override fun onDetach() {
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
        shader = null
        recordedSize = Size.Unspecified
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val layer = layer ?: return
        if (size.width <= 0f || size.height <= 0f || light.width.value <= 0f) return
        if (size != recordedSize || density != recordedDensity || layoutDirection != recordedDirection) {
            record(layer)
        }
        layer.alpha = alpha().coerceIn(0f, 1f)
        if (layer.alpha == 0f) return
        translate(-1f, -1f) { drawLayer(layer) }
    }

    /**
     * The outline stroked at twice the band's width and clipped to itself, so exactly the inner
     * half shows; a pixel of margin on every side keeps the blur from being cut square.
     */
    private fun ContentDrawScope.record(layer: GraphicsLayer) {
        recordedSize = size
        recordedDensity = density
        recordedDirection = layoutDirection
        val outline = shape.createOutline(size, layoutDirection, this)
        val width = ceil(light.width.toPx().coerceAtMost(size.minDimension / 2f))
        paint.color = light.color
        paint.strokeWidth = width * 2f
        paint.asFrameworkPaint().maskFilter = BlurMaskFilter((width / 2f).coerceAtLeast(0.5f), BlurMaskFilter.Blur.NORMAL)
        val shader = shader
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
            paint.asFrameworkPaint().shader = shader.lit(size, outline, light)
        }
        layer.blendMode = BlendMode.Plus
        layer.record(IntSize(ceil(size.width).toInt() + 2, ceil(size.height).toInt() + 2)) {
            translate(1f, 1f) {
                val canvas = drawContext.canvas
                canvas.save()
                when (outline) {
                    is Outline.Rectangle -> canvas.clipRect(outline.rect)
                    is Outline.Rounded -> {
                        clip.rewind()
                        clip.addRoundRect(outline.roundRect)
                        canvas.clipPath(clip)
                    }
                    is Outline.Generic -> canvas.clipPath(outline.path)
                }
                canvas.drawOutline(outline, paint)
                canvas.restore()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.lit(size: Size, outline: Outline, light: RimLight): RuntimeShader = apply {
    setFloatUniform("size", size.width, size.height)
    val radii = when (outline) {
        is Outline.Rounded -> outline.roundRect.let {
            floatArrayOf(
                it.topLeftCornerRadius.x,
                it.topRightCornerRadius.x,
                it.bottomRightCornerRadius.x,
                it.bottomLeftCornerRadius.x,
            )
        }
        is Outline.Rectangle -> FloatArray(4)
        // A path with no corner data is treated as a capsule: the shapes this light goes on.
        is Outline.Generic -> FloatArray(4) { size.minDimension / 2f }
    }
    setFloatUniform("cornerRadii", radii)
    setColorUniform("color", light.color.copy(alpha = 1f).toArgb())
    setFloatUniform("angle", light.angleDegrees * (PI / 180.0).toFloat())
    setFloatUniform("falloff", light.falloff)
}

/**
 * The intensity at each pixel is how squarely the rounded rectangle's edge faces the light: the
 * dot of the signed-distance field's gradient with the light's direction, raised to the falloff.
 * Adapted from the Kyant backdrop library's highlight shader (Apache-2.0).
 */
private const val RimShader = """
uniform float2 size;
uniform float4 cornerRadii;
layout(color) uniform half4 color;
uniform float angle;
uniform float falloff;

float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centered = coord - halfSize;
    float radius = radiusAt(centered, cornerRadii);
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = gradSdRoundedRect(centered, halfSize, gradRadius);
    float2 normal = float2(cos(angle), sin(angle));
    float intensity = pow(abs(dot(grad, normal)), falloff);
    return color * intensity;
}
"""
