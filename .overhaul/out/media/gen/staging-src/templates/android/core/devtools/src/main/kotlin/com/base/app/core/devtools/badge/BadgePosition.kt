package com.base.app.core.devtools.badge

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Where the badge is: docked to the start or end edge at a fraction of the free height, or held
 * free while a finger moves it and while it settles. Offsets are start-relative, so a right-to-left
 * layout mirrors them by itself.
 *
 * A drag writes plain state, read only when the badge is placed: a moving badge is re-placed every
 * frame and never recomposed. Only the release starts a coroutine, for the settle.
 */
@Stable
internal class BadgePosition(dockedAtEnd: Boolean = true, heightFraction: Float = DefaultHeightFraction) {

    var dockedAtEnd by mutableStateOf(dockedAtEnd)
        private set

    private var heightFraction by mutableFloatStateOf(heightFraction)

    /** Under a finger, or settling after one; the badge is a whole capsule then, and opaque. */
    var held by mutableStateOf(false)
        private set

    private var settling = false
    private var freeX by mutableFloatStateOf(0f)
    private var freeY by mutableFloatStateOf(0f)

    fun offset(bounds: BadgeBounds): IntOffset {
        val x = if (held) freeX.coerceIn(bounds.minX, bounds.maxX) else bounds.dockedX(dockedAtEnd)
        val y = if (held) freeY.coerceIn(bounds.minY, bounds.maxY) else bounds.yAt(heightFraction)
        return IntOffset(x.roundToInt(), y.roundToInt())
    }

    /** A finger is down: the badge leaves its edge, or is caught mid-settle, from where it is now. */
    fun hold(bounds: BadgeBounds) {
        if (!held) {
            freeX = bounds.dockedX(dockedAtEnd)
            freeY = bounds.yAt(heightFraction)
        }
        settling = false
        held = true
    }

    fun moveBy(dx: Float, dy: Float, bounds: BadgeBounds) {
        freeX = (freeX + dx).coerceIn(bounds.minX, bounds.maxX)
        freeY = (freeY + dy).coerceIn(bounds.minY, bounds.maxY)
    }

    /**
     * Docks on the side the badge was thrown towards: where it would come to rest at [velocityX]
     * decides, as with iOS's AssistiveTouch, not only where the finger let go.
     */
    suspend fun settle(bounds: BadgeBounds, velocityX: Float, spec: AnimationSpec<Float>) {
        val from = freeX.coerceIn(bounds.minX, bounds.maxX)
        val toEnd = from + velocityX * ProjectionSeconds > (bounds.minX + bounds.maxX) / 2
        val target = bounds.dockedX(toEnd)
        dockedAtEnd = toEnd
        heightFraction = bounds.fractionOf(freeY)
        settling = true
        try {
            animate(from, target, initialVelocity = velocityX, animationSpec = spec) { value, _ -> freeX = value }
        } finally {
            // Cancelled by a new drag, which has already taken the position over through hold().
            if (settling) {
                settling = false
                held = false
            }
        }
    }

    companion object {
        val Saver: Saver<BadgePosition, Any> = Saver(
            save = { listOf(it.dockedAtEnd, it.heightFraction) },
            restore = { saved ->
                val (end, fraction) = saved as List<*>
                BadgePosition(end as Boolean, fraction as Float)
            },
        )
    }
}

/** Where the badge's top-start corner may go, in pixels from the window's top-start corner. */
internal class BadgeBounds(
    window: IntSize,
    badge: IntSize,
    start: Float,
    end: Float,
    top: Float,
    bottom: Float,
) {
    val minX = start
    val maxX = (window.width - end - badge.width).coerceAtLeast(minX)
    val minY = top
    val maxY = (window.height - bottom - badge.height).coerceAtLeast(minY)

    fun dockedX(atEnd: Boolean) = if (atEnd) maxX else minX

    fun yAt(fraction: Float) = minY + (maxY - minY) * fraction

    fun fractionOf(y: Float) = if (maxY > minY) ((y - minY) / (maxY - minY)).coerceIn(0f, 1f) else DefaultHeightFraction
}

/** A little below the middle: under every title, above every tab bar. */
private const val DefaultHeightFraction = 0.55f

/** How far ahead a throw is projected to decide the side, as in UIKit's deceleration. */
private const val ProjectionSeconds = 0.2f
