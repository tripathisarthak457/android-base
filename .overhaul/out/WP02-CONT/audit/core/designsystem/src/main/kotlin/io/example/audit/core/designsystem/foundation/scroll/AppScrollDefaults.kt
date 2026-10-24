package io.example.audit.core.designsystem.foundation.scroll

import android.os.SystemClock
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalDensity
import io.example.audit.core.designsystem.theme.AppTheme
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * How every scrolling container in the app flings. Compose gives a container Android's spline fling
 * unless it names another, so each `LazyColumn`, `LazyRow`, grid, `verticalScroll` and
 * `horizontalScroll` names this one, and each pager [pagerFlingBehavior]:
 *
 * ```
 * LazyColumn(state = listState, flingBehavior = AppScrollDefaults.flingBehavior()) { … }
 * Column(Modifier.verticalScroll(scroll, flingBehavior = AppScrollDefaults.flingBehavior())) { … }
 * ```
 *
 * The edge needs nothing: `AppTheme` installs [AppOverscroll]'s rubber band for every container.
 */
object AppScrollDefaults {

    /**
     * A fling that slows as an iOS scroll view does, keeping 0.998 of its speed every millisecond,
     * so a flick travels further and stops more gently than Android's default. Flicking again in
     * the same direction while it still moves adds to its speed rather than starting over, which is
     * how a long list is crossed with a few quick flicks.
     */
    @Composable
    fun flingBehavior(): FlingBehavior {
        val decay = AppTheme.springs.fling
        val density = LocalDensity.current.density
        return remember(decay, density) { AppFlingBehavior(decay, density) }
    }

    /**
     * A pager's fling: at most one page per flick, decelerating at the paging rate and settling on
     * the screen spring, which never bounces a full-width page.
     */
    @Composable
    fun pagerFlingBehavior(state: PagerState): TargetedFlingBehavior {
        val springs = AppTheme.springs
        return PagerDefaults.flingBehavior(
            state = state,
            decayAnimationSpec = springs.flingFast,
            snapAnimationSpec = springs.screen.pixels,
        )
    }
}

private class AppFlingBehavior(
    private val decay: DecayAnimationSpec<Float>,
    private val density: Float,
) : FlingBehavior {
    /** The speed a fling still had when a finger stopped it, and when that was. */
    private var interruptedVelocity = 0f
    private var interruptedAt = 0L

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val velocity = initialVelocity + carriedMomentum(initialVelocity)
        interruptedVelocity = 0f
        if (abs(velocity) <= 1f) return velocity
        // Scrolling must not stop when the system's animator duration scale is 0.
        return withContext(UnscaledMotion) {
            var velocityLeft = velocity
            var lastValue = 0f
            val animation = AnimationState(initialValue = 0f, initialVelocity = velocity)
            try {
                animation.animateDecay(decay) {
                    val delta = value - lastValue
                    val consumed = scrollBy(delta)
                    lastValue = value
                    velocityLeft = this.velocity
                    if (abs(delta - consumed) > 0.5f) cancelAnimation()
                }
            } catch (exception: CancellationException) {
                interruptedVelocity = animation.velocity
                interruptedAt = SystemClock.uptimeMillis()
                throw exception
            }
            velocityLeft
        }
    }

    /**
     * iOS's momentum: a new flick in the direction a stopped fling was going, quickly enough after
     * it and at least half as fast, carries part of the old speed on, more the faster it was. The
     * curve is iOS's, in points a second, so it is applied in dp.
     */
    private fun carriedMomentum(velocity: Float): Float {
        val previous = interruptedVelocity
        if (previous == 0f || sign(previous) != sign(velocity)) return 0f
        if (SystemClock.uptimeMillis() - interruptedAt > MomentumWindowMillis) return 0f
        if (abs(velocity) < abs(previous) * MomentumMinimumRatio) return 0f
        val previousDp = abs(previous) / density
        return sign(previous) * minOf(MomentumScale * previousDp.pow(MomentumExponent), MomentumMax) * density
    }
}

private val UnscaledMotion = object : MotionDurationScale {
    override val scaleFactor: Float = 1f
}

private const val MomentumWindowMillis = 500L
private const val MomentumMinimumRatio = 0.5f
private const val MomentumScale = 0.000816f
private const val MomentumExponent = 1.967f
private const val MomentumMax = 40_000f
