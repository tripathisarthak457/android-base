package com.base.app.core.designsystem.component.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.lerp
import com.base.app.core.designsystem.theme.TabBarSprings
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Where the pill rests in each slot, along the bar's axis from its start edge, in px. */
@Immutable
internal class PillSlots(
    private val starts: FloatArray,
    private val ends: FloatArray,
    val crossStart: Float,
    val crossEnd: Float,
) {
    val count: Int get() = starts.size

    fun startAt(position: Float): Float = interpolate(starts, position)

    fun endAt(position: Float): Float = interpolate(ends, position)

    fun lengthAt(position: Float): Float = endAt(position) - startAt(position)

    private fun interpolate(values: FloatArray, position: Float): Float {
        if (values.isEmpty()) return 0f
        val p = position.coerceIn(0f, values.lastIndex.toFloat())
        val low = floor(p).toInt()
        val high = ceil(p).toInt()
        return lerp(values[low], values[high], p - low)
    }

    override fun equals(other: Any?): Boolean = other is PillSlots &&
        starts.contentEquals(other.starts) && ends.contentEquals(other.ends) &&
        crossStart == other.crossStart && crossEnd == other.crossEnd

    override fun hashCode(): Int = 31 * starts.contentHashCode() + ends.contentHashCode()

    companion object {
        val Empty = PillSlots(FloatArray(0), FloatArray(0), 0f, 0f)
    }
}

/**
 * The selection pill's two edges, in slot units. The edge facing the new tab runs ahead on a
 * quicker spring with a little give and the trailing one follows, so the pill stretches toward its
 * target and contracts as it lands, instead of sliding over as a rigid block. Read in draw only.
 */
@Stable
internal class IndicatorEdges(selected: Int) {
    val start = Animatable(selected.toFloat())
    val end = Animatable(selected.toFloat())

    /** Whether the pill last moved toward the end side; the trailing edge is the one clamped. */
    var forward = true
        private set

    suspend fun moveTo(target: Int) = coroutineScope {
        val to = target.toFloat()
        forward = to > start.value
        val lead = if (forward) end else start
        val trail = if (forward) start else end
        launch { lead.animateTo(to, TabBarSprings.PillLead) }
        trail.animateTo(to, TabBarSprings.PillTrail)
    }

    /** The pill's drawn span in slot units, never wider than [MAX_STRETCH] past one slot. */
    inline fun span(block: (from: Float, to: Float) -> Unit) {
        var from = min(start.value, end.value)
        var to = max(start.value, end.value)
        if (to - from > MAX_STRETCH) {
            if (forward) from = to - MAX_STRETCH else to = from + MAX_STRETCH
        }
        block(from, to)
    }

    companion object {
        const val MAX_STRETCH = 0.5f

        /** How much the pill thins per slot of stretch, so it reads as a gel rather than a bar. */
        const val SQUASH = 0.06f
    }
}

/**
 * Each tab's glyph dips and springs back when the user selects it, as an SF Symbol bounce does:
 * it reads as the glyph being pressed, on a bar whose items have no press overlay of their own,
 * and it never grows the glyph past the pill.
 */
@Stable
internal class TabBounce(count: Int) {
    private val scales = List(count) { Animatable(1f) }

    fun scale(index: Int): Float = scales.getOrNull(index)?.value ?: 1f

    suspend fun play(index: Int) {
        val scale = scales.getOrNull(index) ?: return
        scale.animateTo(BOUNCE_DOWN, TabBarSprings.GlyphDip)
        scale.animateTo(1f, TabBarSprings.GlyphReturn)
    }

    private companion object {
        const val BOUNCE_DOWN = 0.84f
    }
}
