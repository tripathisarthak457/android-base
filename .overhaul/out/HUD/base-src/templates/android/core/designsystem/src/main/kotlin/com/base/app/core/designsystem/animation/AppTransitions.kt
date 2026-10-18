package com.base.app.core.designsystem.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppMotion
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.rememberReduceMotion
import kotlinx.coroutines.delay

/** The transitions the app animates content with, as named pairs. */
class AppTransitions internal constructor(
    private val motion: AppMotion,
    private val reduceMotion: Boolean,
) {

    /** For content that appears where it is. The default for almost everything. */
    val fadeIn: EnterTransition
        get() = if (reduceMotion) EnterTransition.None else fadeIn(tween(motion.medium, easing = motion.enter))

    val fadeOut: ExitTransition
        get() = if (reduceMotion) ExitTransition.None else fadeOut(tween(motion.quick, easing = motion.exit))

    /** For a block that opens in place — an accordion, a validation message, an expanding card. */
    val expandIn: EnterTransition
        get() = if (reduceMotion) {
            EnterTransition.None
        } else {
            expandVertically(tween(motion.medium, easing = motion.enter)) +
                fadeIn(tween(motion.quick))
        }

    val collapseOut: ExitTransition
        get() = if (reduceMotion) {
            ExitTransition.None
        } else {
            shrinkVertically(tween(motion.medium, easing = motion.exit)) +
                fadeOut(tween(motion.instant))
        }

    /** For something arriving from the bottom edge: a sheet, a banner, a toast. */
    val riseIn: EnterTransition
        get() = if (reduceMotion) {
            EnterTransition.None
        } else {
            slideInVertically(motion.sheet()) { it } + fadeIn(tween(motion.quick))
        }

    val sinkOut: ExitTransition
        get() = if (reduceMotion) {
            ExitTransition.None
        } else {
            slideOutVertically(tween(motion.quick, easing = motion.exit)) { it } +
                fadeOut(tween(motion.instant))
        }

    /** For an element that should read as *appearing*, not moving: a badge, a checkmark, a FAB. */
    val popIn: EnterTransition
        get() = if (reduceMotion) {
            EnterTransition.None
        } else {
            scaleIn(motion.press(), initialScale = 0.85f) + fadeIn(tween(motion.quick))
        }

    val popOut: ExitTransition
        get() = if (reduceMotion) {
            ExitTransition.None
        } else {
            scaleOut(tween(motion.instant), targetScale = 0.85f) + fadeOut(tween(motion.instant))
        }

    /** A lateral move: step 2 of a form replacing step 1. */
    fun slideIn(forward: Boolean = true): EnterTransition =
        if (reduceMotion) {
            EnterTransition.None
        } else {
            slideInHorizontally(tween(motion.medium, easing = motion.enter)) { width ->
                if (forward) width else -width
            } + fadeIn(tween(motion.quick))
        }

    fun slideOut(forward: Boolean = true): ExitTransition =
        if (reduceMotion) {
            ExitTransition.None
        } else {
            slideOutHorizontally(tween(motion.medium, easing = motion.exit)) { width ->
                if (forward) -width else width
            } + fadeOut(tween(motion.quick))
        }

    /** The entrance for row [index] of a list that is appearing for the first time. */
    fun staggeredIn(index: Int): EnterTransition {
        if (reduceMotion) return EnterTransition.None
        val delay = (index.coerceAtMost(MAX_STAGGERED_ROWS) * STAGGER_MILLIS)
        return fadeIn(tween(motion.medium, delayMillis = delay, easing = motion.enter)) +
            slideInVertically(
                animationSpec = tween(motion.medium, delayMillis = delay, easing = motion.enter),
                initialOffsetY = { STAGGER_OFFSET_PX },
            )
    }

    internal companion object {
        const val STAGGER_MILLIS = 28
        const val MAX_STAGGERED_ROWS = 8
        const val STAGGER_OFFSET_PX = 24
    }
}

/** The transition set for the current theme and accessibility settings. */
@Composable
fun rememberAppTransitions(): AppTransitions {
    val motion = AppTheme.motion
    val reduceMotion = rememberReduceMotion()
    return remember(motion, reduceMotion) { AppTransitions(motion, reduceMotion) }
}

/** Fades and lifts its content in once, the first time it is shown. */
@Composable
fun AppAppear(
    modifier: Modifier = Modifier,
    delayMillis: Long = 0,
    content: @Composable () -> Unit,
) {
    val progress = rememberAppearProgress(delayMillis)
    val lift = with(LocalDensity.current) { APPEAR_OFFSET.toPx() }
    Box(modifier = modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * lift
    }) {
        content()
    }
}

private val APPEAR_OFFSET = 12.dp

/** Animates a row into place when the list is reordered, filtered or inserted into. */
@Composable
fun LazyItemScope.appAnimateItem(modifier: Modifier = Modifier): Modifier {
    val motion = AppTheme.motion
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) return modifier
    return modifier.animateItem(
        fadeInSpec = tween(motion.medium, easing = motion.enter),
        placementSpec = motion.navigation(),
        fadeOutSpec = tween(motion.quick, easing = motion.exit),
    )
}

/** Applies a staggered entrance to a column of items that all appear at once. */
@Composable
fun AppStaggeredColumn(
    itemCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int) -> Unit,
) {
    val motion = AppTheme.motion
    // Linear overall, eased per row: each row eases over its own slice of the timeline, which a
    // value that was already eased would distort.
    val progress = rememberAppearProgress(
        delayMillis = 0,
        durationMillis = motion.medium + AppTransitions.STAGGER_MILLIS * AppTransitions.MAX_STAGGERED_ROWS,
        easing = LinearEasing,
    )
    val lift = with(LocalDensity.current) { APPEAR_OFFSET.toPx() }
    val total = (motion.medium + AppTransitions.STAGGER_MILLIS * AppTransitions.MAX_STAGGERED_ROWS).toFloat()

    Column(modifier = modifier) {
        repeat(itemCount) { index ->
            val start = index.coerceAtMost(AppTransitions.MAX_STAGGERED_ROWS) * AppTransitions.STAGGER_MILLIS / total
            val span = motion.medium / total
            Box(modifier = Modifier.graphicsLayer {
                val local = ((progress.value - start) / span).coerceIn(0f, 1f)
                val eased = motion.enter.transform(local)
                alpha = eased
                translationY = (1f - eased) * lift
            }) {
                content(index)
            }
        }
    }
}

/** 0 to 1, once per saved-state lifetime. Starts at 1 under reduce motion, so nothing animates. */
@Composable
private fun rememberAppearProgress(
    delayMillis: Long,
    durationMillis: Int = AppTheme.motion.medium,
    easing: Easing = AppTheme.motion.enter,
): Animatable<Float, AnimationVector1D> {
    val reduceMotion = rememberReduceMotion()
    var shown by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (shown || reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            if (delayMillis > 0) delay(delayMillis)
            progress.animateTo(1f, tween(durationMillis, easing = easing))
        }
        shown = true
    }
    return progress
}

/** Dims and blocks its content while [busy]. */
@Composable
fun Modifier.busyOverlay(busy: Boolean): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (busy) BUSY_ALPHA else 1f,
        animationSpec = tween(AppTheme.motion.quick),
        label = "busyAlpha",
    )
    return this
        .alpha(alpha)
        .then(
            if (busy) {
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                }
            } else {
                Modifier
            },
        )
}

private const val BUSY_ALPHA = 0.4f
