package com.example.none.core.navigation.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.scene.Scene
import com.example.none.core.common.navigation.AppNavKey
import kotlin.math.PI

/**
 * The one spring every full-screen move rides: push, pop, covers and the back card's commit.
 * Critically damped, because a long travel with any overshoot in it reads as unstable. [response]
 * is in seconds, as iOS states springs: stiffness = (2π / response)².
 */
@Immutable
internal data class ScreenMotion(val response: Float = DEFAULT_RESPONSE, val parallax: Float = PARALLAX) {
    private val stiffness = ((2 * PI / response) * (2 * PI / response)).toFloat()

    val offset: SpringSpec<IntOffset> = spring(1f, stiffness, IntOffset.VisibilityThreshold)

    /** The same spring for a 0 to 1 progress, so a scrim or a follower moves in step with the slide. */
    val fraction: SpringSpec<Float> = spring(1f, stiffness, visibilityThreshold = FRACTION_THRESHOLD)

    companion object {
        /**
         * UIKit's push curve (ω₀ 18.26). A stiffer spring covers three quarters of the width in the
         * first 100 ms, which shoves the screen in rather than sliding it.
         */
        const val DEFAULT_RESPONSE = 0.344f

        /** How far the covered screen moves, as a fraction of the one arriving over it: UIKit's third. */
        const val PARALLAX = 0.333f

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

    /** A cover sinks off the bottom edge, unless the back card already showed it leaving ([shownByCard]). */
    fun dismiss(motion: ScreenMotion, shownByCard: () -> Boolean): SceneTransform = {
        if (shownByCard()) none else EnterTransition.None togetherWith slideOutVertically(motion.offset) { it }
    }

    val none: ContentTransform = EnterTransition.None togetherWith ExitTransition.None

    val noneSpec: SceneTransform = { none }

    val noneDuringPredictive: AnimatedContentTransitionScope<Scene<AppNavKey>>.(Int) -> ContentTransform = { none }
}
