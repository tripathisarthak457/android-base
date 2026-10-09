package com.base.app.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.base.app.core.designsystem.theme.AppSprings

/** The app's screen-to-screen transitions. Three shapes, and only three. */
internal object NavTransitions {

    private const val TAB_ENTER_SCALE = 0.985f
    private const val OUTGOING_DIM = 0.28f
    private const val BEHIND_SHEET_SCALE = 0.96f

    fun push(motion: AppSprings): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = motion.screen.offset,
        ) togetherWith slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = motion.screen.offset,
            targetOffset = { fullWidth -> (fullWidth * motion.navigation.parallax).toInt() },
        ) + fadeOut(
            animationSpec = motion.fadeIn(),
            targetAlpha = 1f - OUTGOING_DIM,
        )
    }

    /** The exact mirror of [push]. Programmatic back, and the end of a completed gesture. */
    fun pop(motion: AppSprings): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        (
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = motion.screen.offset,
                initialOffset = { fullWidth -> (fullWidth * motion.navigation.parallax).toInt() },
            ) + fadeIn(
                animationSpec = motion.fadeIn(),
                initialAlpha = 1f - OUTGOING_DIM,
            )
            ) togetherWith slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = motion.screen.offset,
        )
    }

    /**
     * A layer over the current screen. Rises from the bottom edge; the screen underneath settles
     * back and dims, so the new layer reads as sitting on top of it rather than replacing it.
     */
    fun modal(motion: AppSprings): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        (
            slideInVertically(
                animationSpec = motion.sheet.offset,
                initialOffsetY = { fullHeight -> fullHeight },
            ) + fadeIn(motion.fadeIn())
            ) togetherWith (
            scaleOut(
                animationSpec = motion.fadeIn(),
                targetScale = BEHIND_SHEET_SCALE,
            ) + fadeOut(
                animationSpec = motion.fadeIn(),
                targetAlpha = 1f - OUTGOING_DIM,
            )
            )
    }

    fun modalDismiss(motion: AppSprings): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        (
            scaleIn(
                animationSpec = motion.fadeIn(),
                initialScale = BEHIND_SHEET_SCALE,
            ) + fadeIn(
                animationSpec = motion.fadeIn(),
                initialAlpha = 1f - OUTGOING_DIM,
            )
            ) togetherWith (
            slideOutVertically(
                animationSpec = motion.sheet.offset,
                targetOffsetY = { fullHeight -> fullHeight },
            ) + fadeOut(motion.fadeOut())
            )
    }

    /** Peers. Tab to tab, and a splash handing off. */
    fun fade(motion: AppSprings): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        (
            fadeIn(motion.fadeIn()) + slideInVertically(
                animationSpec = motion.fadeIn(),
                initialOffsetY = { fullHeight -> fullHeight / 44 },
            )
            ) togetherWith fadeOut(motion.fadeOut())
    }

    /**
     * Tab to tab: the outgoing tab fades quickly, the incoming one fades in and settles from a hair
     * below full size.
     */
    fun tabSwitch(motion: AppSprings): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        (
            fadeIn(motion.fadeIn()) +
                scaleIn(
                    animationSpec = motion.fadeIn(),
                    initialScale = TAB_ENTER_SCALE,
                )
            ) togetherWith fadeOut(motion.fadeOut())
    }

    fun none(): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        EnterTransition.None togetherWith ExitTransition.None
    }

    fun forStyle(
        style: NavTransitionStyle,
        motion: AppSprings,
        reduceMotion: Boolean,
    ): AnimatedContentTransitionScope<*>.() -> ContentTransform = when {
        reduceMotion || style == NavTransitionStyle.None -> none()
        style == NavTransitionStyle.Modal -> modal(motion)
        style == NavTransitionStyle.Fade -> fade(motion)
        else -> push(motion)
    }

    fun popForStyle(
        style: NavTransitionStyle,
        motion: AppSprings,
        reduceMotion: Boolean,
    ): AnimatedContentTransitionScope<*>.() -> ContentTransform = when {
        reduceMotion || style == NavTransitionStyle.None -> none()
        style == NavTransitionStyle.Modal -> modalDismiss(motion)
        style == NavTransitionStyle.Fade -> fade(motion)
        else -> pop(motion)
    }
}
