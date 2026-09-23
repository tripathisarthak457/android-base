package com.base.app.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.base.app.core.designsystem.theme.AppMotion

/** The app's screen-to-screen transitions. Three shapes, and only three. */
internal object NavTransitions {

    private const val TAB_ENTER_SCALE = 0.985f

    fun push(motion: AppMotion): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = motion.navigation(),
        ) togetherWith slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = motion.navigation(),
            targetOffset = { fullWidth -> (fullWidth * motion.outgoingParallax).toInt() },
        ) + fadeOut(
            animationSpec = tween(motion.medium, easing = motion.standard),
            targetAlpha = 1f - motion.outgoingDim,
        )
    }

    /** The exact mirror of [push]. Programmatic back, and the end of a completed gesture. */
    fun pop(motion: AppMotion): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        (
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = motion.navigation(),
                initialOffset = { fullWidth -> (fullWidth * motion.outgoingParallax).toInt() },
            ) + fadeIn(
                animationSpec = tween(motion.medium, easing = motion.standard),
                initialAlpha = 1f - motion.outgoingDim,
            )
            ) togetherWith slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = motion.navigation(),
        )
    }

    /**
     * A layer over the current screen. Rises from the bottom edge; the screen underneath settles
     * back and dims, so the new layer reads as sitting on top of it rather than replacing it.
     */
    fun modal(motion: AppMotion): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        (
            slideInVertically(
                animationSpec = motion.sheet(),
                initialOffsetY = { fullHeight -> fullHeight },
            ) + fadeIn(tween(motion.quick))
            ) togetherWith (
            scaleOut(
                animationSpec = tween(motion.medium, easing = motion.standard),
                targetScale = motion.behindSheetScale,
            ) + fadeOut(
                animationSpec = tween(motion.medium, easing = motion.standard),
                targetAlpha = 1f - motion.outgoingDim,
            )
            )
    }

    fun modalDismiss(motion: AppMotion): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        (
            scaleIn(
                animationSpec = tween(motion.medium, easing = motion.enter),
                initialScale = motion.behindSheetScale,
            ) + fadeIn(
                animationSpec = tween(motion.medium, easing = motion.standard),
                initialAlpha = 1f - motion.outgoingDim,
            )
            ) togetherWith (
            slideOutVertically(
                animationSpec = motion.sheet(),
                targetOffsetY = { fullHeight -> fullHeight },
            ) + fadeOut(tween(motion.slow))
            )
    }

    /** Peers. Tab to tab, and a splash handing off. */
    fun fade(motion: AppMotion): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        (
            fadeIn(tween(motion.medium, easing = motion.enter)) + slideInVertically(
                animationSpec = tween(motion.medium, easing = motion.enter),
                initialOffsetY = { fullHeight -> fullHeight / 44 },
            )
            ) togetherWith fadeOut(tween(motion.quick))
    }

    /**
     * Tab to tab: the outgoing tab fades quickly, the incoming one fades in and settles from a hair
     * below full size.
     */
    fun tabSwitch(motion: AppMotion): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        (
            fadeIn(tween(motion.medium, delayMillis = motion.instant, easing = motion.enter)) +
                scaleIn(
                    animationSpec = tween(motion.medium, delayMillis = motion.instant, easing = motion.enter),
                    initialScale = TAB_ENTER_SCALE,
                )
            ) togetherWith fadeOut(tween(motion.instant + motion.instant / 2, easing = motion.exit))
    }

    fun none(): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        EnterTransition.None togetherWith ExitTransition.None
    }

    fun forStyle(
        style: NavTransitionStyle,
        motion: AppMotion,
        reduceMotion: Boolean,
    ): AnimatedContentTransitionScope<*>.() -> ContentTransform = when {
        reduceMotion || style == NavTransitionStyle.None -> none()
        style == NavTransitionStyle.Modal -> modal(motion)
        style == NavTransitionStyle.Fade -> fade(motion)
        else -> push(motion)
    }

    fun popForStyle(
        style: NavTransitionStyle,
        motion: AppMotion,
        reduceMotion: Boolean,
    ): AnimatedContentTransitionScope<*>.() -> ContentTransform = when {
        reduceMotion || style == NavTransitionStyle.None -> none()
        style == NavTransitionStyle.Modal -> modalDismiss(motion)
        style == NavTransitionStyle.Fade -> fade(motion)
        else -> pop(motion)
    }
}
