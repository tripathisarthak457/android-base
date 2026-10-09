package com.base.app.core.navigation.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.scene.Scene
import com.base.app.core.common.navigation.AppNavKey
import kotlin.math.PI

/**
 * The one spring every full-screen move rides: push, pop and covers. Critically damped, because a
 * long travel with any overshoot in it reads as unstable, and short, because a slow settle leaves a
 * sliver of the old screen crawling at the edge. [response] is in seconds, as iOS states springs:
 * stiffness = (2π / response)².
 */
@Immutable
internal data class ScreenMotion(val response: Float = DEFAULT_RESPONSE, val parallax: Float = PARALLAX) {
    private val stiffness = ((2 * PI / response) * (2 * PI / response)).toFloat()

    val offset: SpringSpec<IntOffset> = spring(1f, stiffness, IntOffset.VisibilityThreshold)

    /** The same spring for a 0 to 1 progress, so a scrim or a follower moves in step with the slide. */
    val fraction: SpringSpec<Float> = spring(1f, stiffness, visibilityThreshold = FRACTION_THRESHOLD)

    /** How long the spring takes to bring [distancePx] of travel to rest, to within a pixel. */
    fun settleMillis(distancePx: Float): Int {
        val nanos = FloatSpringSpec(1f, stiffness, visibilityThreshold = 1f)
            .getDurationNanos(initialValue = distancePx, targetValue = 0f, initialVelocity = 0f)
        return (nanos / 1_000_000).toInt().coerceAtLeast(1)
    }

    companion object {
        /** Provisional: the user picks between 0.24, 0.30 and 0.36 s from device recordings. */
        const val DEFAULT_RESPONSE = 0.24f

        /** How far the covered screen moves, as a fraction of the one arriving over it. */
        const val PARALLAX = 0.30f

        private const val FRACTION_THRESHOLD = 0.001f
    }
}

private typealias SceneTransform = AnimatedContentTransitionScope<Scene<AppNavKey>>.() -> ContentTransform

/**
 * Transitions of the presentation host, which `NavDisplay` draws. Covers only slide: no alpha and
 * no scale, because either renders the whole screen offscreen on every frame.
 */
internal object NavTransitions {

    /** A cover rises over whatever is presented beneath it, which stays where it is. */
    fun present(motion: ScreenMotion): SceneTransform = {
        slideInVertically(motion.offset) { it } togetherWith ExitTransition.KeepUntilTransitionsFinished
    }

    fun dismiss(motion: ScreenMotion): SceneTransform = {
        EnterTransition.None togetherWith slideOutVertically(motion.offset) { it }
    }

    fun predictiveDismiss(durationMs: Int): AnimatedContentTransitionScope<Scene<AppNavKey>>.(Int) -> ContentTransform = {
        EnterTransition.None togetherWith slideOutVertically(tween(durationMs, easing = LinearEasing)) { it }
    }

    val none: ContentTransform = EnterTransition.None togetherWith ExitTransition.None

    val noneSpec: SceneTransform = { none }

    val noneDuringPredictive: AnimatedContentTransitionScope<Scene<AppNavKey>>.(Int) -> ContentTransform = { none }
}
