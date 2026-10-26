package com.example.finalafter.crui.core.designsystem.foundation.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * What one piece of glass is made of. [GlassDefaults] has the sets the app uses: content glass,
 * chrome over a blurred scene, and frosted chrome.
 *
 * @property body the fill. Over a blurred scene it is a light tint; drawn glass is more opaque, and
 *   frosted chrome, which has nothing behind it but the content, more opaque still, so text on it
 *   stays legible over anything.
 * @property rim the edge light catches, at full strength along the top and fading toward the
 *   bottom.
 * @property sheen the light pooling near the top-left corner of drawn glass. Glass over a
 *   blurred scene has none: the scene is its light.
 */
@Immutable
class GlassColors(
    val body: Color,
    val rim: Color,
    val sheen: Color,
) {
    override fun equals(other: Any?): Boolean =
        other is GlassColors && body == other.body && rim == other.rim && sheen == other.sheen

    override fun hashCode(): Int = (body.hashCode() * 31 + rim.hashCode()) * 31 + sheen.hashCode()
}
