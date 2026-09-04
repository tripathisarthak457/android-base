package com.base.app.core.designsystem.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import com.base.app.core.designsystem.theme.AppMotion
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.rememberReduceMotion
import kotlinx.coroutines.delay

/**
 * The transitions the app animates content with, as named pairs.
 *
 * Every one of these already respects the theme's durations and easings, so a screen never writes
 * a raw `tween(300)` — the value that ends up half a beat out of step with everything else on the
 * same screen.
 *
 * They are also all *reduce-motion aware* through [rememberAppTransitions], which is the whole
 * reason they are a lookup rather than a set of free functions: honouring the setting at each of
 * forty call sites is a promise nobody keeps.
 */
class AppTransitions internal constructor(
    private val motion: AppMotion,
    private val reduceMotion: Boolean,
) {

    /** For content that appears where it is. The default for almost everything. */
    val fadeIn: EnterTransition
        get() = if (reduceMotion) EnterTransition.None else fadeIn(tween(motion.medium, easing = motion.enter))

    val fadeOut: ExitTransition
        get() = if (reduceMotion) ExitTransition.None else fadeOut(tween(motion.quick, easing = motion.exit))

    /**
     * For a block that opens in place — an accordion, a validation message, an expanding card.
     *
     * The fade is faster than the expansion on purpose: text that is fully opaque while the box
     * is still growing reads as the text being clipped, rather than as the box opening.
     */
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

    /**
     * A lateral move: step 2 of a form replacing step 1.
     *
     * [forward] flips the direction so going back moves left, which is the only thing that makes
     * a multi-step flow feel like a line rather than a shuffle.
     */
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

    /**
     * The entrance for row [index] of a list that is appearing for the first time.
     *
     * Each row is offset by [STAGGER_MILLIS], up to [MAX_STAGGERED_ROWS]. The cap matters more
     * than the delay: without it, row 40 waits a second and a half to appear, and a list that is
     * scrolled quickly shows a cascade of blanks. Past the cap everything arrives together.
     */
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

/**
 * The transition set for the current theme and accessibility settings.
 *
 * Read it once at the top of a screen and use it throughout; it is cheap, but re-reading
 * `rememberReduceMotion()` per row is not.
 */
@Composable
fun rememberAppTransitions(): AppTransitions {
    val motion = AppTheme.motion
    val reduceMotion = rememberReduceMotion()
    return remember(motion, reduceMotion) { AppTransitions(motion, reduceMotion) }
}

/**
 * Fades and lifts its content in once, shortly after it first composes.
 *
 * For a screen's content arriving after a load. A plain `AnimatedVisibility(visible = true)` does
 * nothing — the content is already visible on the first frame, so there is no transition to run;
 * flipping a flag in a `LaunchedEffect` is what gives it something to animate *from*.
 */
@Composable
fun AppAppear(
    modifier: Modifier = Modifier,
    delayMillis: Long = 0,
    content: @Composable () -> Unit,
) {
    val transitions = rememberAppTransitions()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = transitions.fadeIn + slideInVertically { APPEAR_OFFSET_PX },
        exit = transitions.fadeOut,
    ) {
        content()
    }
}

private const val APPEAR_OFFSET_PX = 20

/**
 * Animates a row into place when the list is reordered, filtered or inserted into.
 *
 * A thin wrapper over `Modifier.animateItem` so that feature code does not repeat the spec, and
 * so reduce-motion turns it off in one place.
 *
 * It only works when the list supplies a stable `key` — without one, Compose cannot tell an
 * inserted row from a changed one and there is nothing to animate between.
 */
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

/**
 * Applies a staggered entrance to a column of items that all appear at once.
 *
 * For a fixed set of rows — a settings group, a dashboard's cards. A `LazyColumn` should use
 * [appAnimateItem] instead: staggering rows the user scrolls to would re-animate them every time
 * they come back on screen.
 */
@Composable
fun AppStaggeredColumn(
    itemCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int) -> Unit,
) {
    val transitions = rememberAppTransitions()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(itemCount) { visible = true }

    Column(modifier = modifier) {
        repeat(itemCount) { index ->
            AnimatedVisibility(
                visible = visible,
                enter = transitions.staggeredIn(index),
                exit = transitions.fadeOut,
            ) {
                content(index)
            }
        }
    }
}

/**
 * Dims and blocks its content while [busy].
 *
 * `alpha` rather than a scrim on top, so the content stays legible — the user can still read what
 * they submitted while it is being saved — and a pointer filter rather than `enabled = false` on
 * every child, which would be dozens of parameters threaded down for one transient state.
 *
 * The pointer loop consumes every change, which is what actually blocks the input: a plain
 * `clickable {}` overlay would still let a scroll through, and a scrollable list that moves under
 * a "saving…" overlay reads as the app having lost the submission.
 */
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
