package com.example.finalafter.crui2.core.designsystem.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import com.example.finalafter.crui2.core.designsystem.component.text.AppText
import com.example.finalafter.crui2.core.designsystem.preview.AppComponentPreviews
import com.example.finalafter.crui2.core.designsystem.theme.AppSprings
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import kotlinx.coroutines.delay

/**
 * The transitions content changes with, as named pairs. Anything that moves rides a spring from
 * [AppSprings], so it can be interrupted and retargeted mid-flight; only opacity uses a fade.
 */
class AppTransitions internal constructor(
    private val springs: AppSprings,
    private val reduceMotion: Boolean,
) {
    /** For content that appears where it is. The default for almost everything. */
    val fadeIn: EnterTransition
        get() = if (reduceMotion) EnterTransition.None else fadeIn(springs.fadeIn())

    val fadeOut: ExitTransition
        get() = if (reduceMotion) ExitTransition.None else fadeOut(springs.fadeOut())

    /** For a block that opens in place: an expanded row, a validation message. */
    val expandIn: EnterTransition
        get() = if (reduceMotion) {
            EnterTransition.None
        } else {
            expandVertically(springs.layout.size) + fadeIn(springs.quickFade())
        }

    val collapseOut: ExitTransition
        get() = if (reduceMotion) {
            ExitTransition.None
        } else {
            shrinkVertically(springs.layout.size) + fadeOut(springs.quickFade())
        }

    /** For something arriving from the bottom edge: a banner, a bar. */
    val riseIn: EnterTransition
        get() = if (reduceMotion) EnterTransition.None else slideInVertically(springs.sheet.offset) { it }

    val sinkOut: ExitTransition
        get() = if (reduceMotion) ExitTransition.None else slideOutVertically(springs.sheet.offset) { it }

    /** For an element that should read as appearing, not moving: a badge, a checkmark. */
    val popIn: EnterTransition
        get() = if (reduceMotion) {
            EnterTransition.None
        } else {
            scaleIn(springs.emphasis.unit, initialScale = PopScale) + fadeIn(springs.quickFade())
        }

    val popOut: ExitTransition
        get() = if (reduceMotion) {
            ExitTransition.None
        } else {
            scaleOut(springs.press.unit, targetScale = PopScale) + fadeOut(springs.quickFade())
        }

    /** A lateral move inside one screen: step 2 of a form replacing step 1. */
    fun slideIn(forward: Boolean = true): EnterTransition =
        if (reduceMotion) {
            EnterTransition.None
        } else {
            slideInHorizontally(springs.screen.offset) { width -> if (forward) width else -width }
        }

    fun slideOut(forward: Boolean = true): ExitTransition =
        if (reduceMotion) {
            ExitTransition.None
        } else {
            slideOutHorizontally(springs.screen.offset) { width -> if (forward) -width else width }
        }
}

private const val PopScale = 0.85f

/** The transition set for the current theme and accessibility settings. */
@Composable
fun rememberAppTransitions(): AppTransitions {
    val springs = AppTheme.springs
    val reduceMotion = AppTheme.reduceMotion
    return remember(springs, reduceMotion) { AppTransitions(springs, reduceMotion) }
}

/** Fades and lifts its content in once, the first time it is shown. */
@Composable
fun AppAppear(
    modifier: Modifier = Modifier,
    delayMillis: Long = 0,
    content: @Composable () -> Unit,
) {
    val springs = AppTheme.springs
    val progress = rememberAppearProgress(delayMillis, springs.fadeSlow + springs.staggerMillis)
    val lift = with(LocalDensity.current) { springs.appearOffset.toPx() }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * lift
            // A card's shadow sits outside its bounds; an offscreen fade would clip it square.
            compositingStrategy = CompositingStrategy.ModulateAlpha
        },
    ) {
        content()
    }
}

/** Animates a row into place when the list is reordered, filtered or inserted into. */
@Composable
fun LazyItemScope.appAnimateItem(modifier: Modifier = Modifier): Modifier {
    if (AppTheme.reduceMotion) return modifier
    val springs = AppTheme.springs
    return modifier.animateItem(
        fadeInSpec = springs.fadeIn(),
        placementSpec = springs.layout.offset,
        fadeOutSpec = springs.fadeOut(),
    )
}

/**
 * A column whose items rise in one after another the first time it is shown: [AppSprings.staggerMillis]
 * apart, the first [AppSprings.staggerMaxItems] waiting their turn and the rest arriving with the last.
 */
@Composable
fun AppStaggeredColumn(
    itemCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int) -> Unit,
) {
    val springs = AppTheme.springs
    val stagger = springs.staggerMillis
    val span = springs.fadeSlow + stagger
    val total = (span + stagger * springs.staggerMaxItems).toFloat()
    // Linear overall, eased per item: each item eases over its own slice of the timeline, which a
    // value that was already eased would distort.
    val progress = rememberAppearProgress(delayMillis = 0, durationMillis = total.toInt(), linear = true)
    val lift = with(LocalDensity.current) { springs.appearOffset.toPx() }

    Column(modifier = modifier) {
        repeat(itemCount) { index ->
            val start = index.coerceAtMost(springs.staggerMaxItems) * stagger / total
            Box(
                modifier = Modifier.graphicsLayer {
                    val local = ((progress.value - start) / (span / total)).coerceIn(0f, 1f)
                    val eased = springs.enterEasing.transform(local)
                    alpha = eased
                    translationY = (1f - eased) * lift
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                },
            ) {
                content(index)
            }
        }
    }
}

/** 0 to 1, once per saved-state lifetime. Starts at 1 under reduce motion, so nothing animates. */
@Composable
private fun rememberAppearProgress(
    delayMillis: Long,
    durationMillis: Int,
    linear: Boolean = false,
): Animatable<Float, AnimationVector1D> {
    val springs = AppTheme.springs
    val reduceMotion = AppTheme.reduceMotion
    var shown by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (shown || reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            if (delayMillis > 0) delay(delayMillis)
            progress.animateTo(1f, springs.arrival(durationMillis, linear))
        }
        shown = true
    }
    return progress
}

/**
 * Dims content and blocks every touch on it while [busy]: a form while it submits. The touch is
 * taken before children see it, so a button inside cannot fire a second time.
 */
fun Modifier.busyOverlay(busy: Boolean): Modifier {
    if (!busy) return this
    return this
        .alpha(BusyAlpha)
        .semantics { disabled() }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }
}

private const val BusyAlpha = 0.4f

@AppComponentPreviews
@Composable
private fun AppTransitionsPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppAppear { AppText(text = "Appears once composed") }
        AppStaggeredColumn(itemCount = 3) { index -> AppText(text = "Staggered row ${index + 1}") }
        LazyColumn(modifier = Modifier.height(AppTheme.sizes.rowHeight * 2)) {
            items(listOf("Placed in a list", "Moves when the list changes"), key = { it }) { item ->
                AppText(text = item, modifier = appAnimateItem())
            }
        }
    }
}
