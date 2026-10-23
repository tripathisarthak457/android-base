package io.example.fitcnetdb.core.designsystem.component.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.example.fitcnetdb.core.designsystem.component.button.AppLinkButton
import io.example.fitcnetdb.core.designsystem.component.feedback.AppTone
import io.example.fitcnetdb.core.designsystem.component.text.AppIcon
import io.example.fitcnetdb.core.designsystem.component.text.AppText
import io.example.fitcnetdb.core.designsystem.foundation.content.AppSurface
import io.example.fitcnetdb.core.designsystem.foundation.interaction.AppHaptic
import io.example.fitcnetdb.core.designsystem.foundation.interaction.rememberAppHaptics
import io.example.fitcnetdb.core.designsystem.foundation.layout.ChromeInset
import io.example.fitcnetdb.core.designsystem.foundation.layout.LocalBottomChrome
import io.example.fitcnetdb.core.designsystem.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * One toast: a confirmation, a non-blocking failure, or an undo offer. [hud] draws it as iOS's HUD,
 * a square in the middle of the screen with the tone's glyph, for "Copied" or "Saved".
 */
@Immutable
class ToastMessage(
    val text: String,
    val title: String? = null,
    val tone: AppTone = AppTone.Neutral,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val durationMillis: Long = if (actionLabel != null) ActionDuration else DefaultDuration,
    val hud: Boolean = false,
) {
    /** The same message, shown again: it restarts the one on screen rather than queueing a copy. */
    internal fun sameAs(other: ToastMessage) =
        text == other.text && title == other.title && tone == other.tone && actionLabel == other.actionLabel && hud == other.hud

    private companion object {
        const val DefaultDuration = 2_500L
        const val ActionDuration = 4_500L
    }
}

/**
 * The queue behind the toast layer. One toast shows at a time; the next waits until it leaves. Its
 * time counts only while the app is on screen, so a toast sent while the app is in the background
 * is still there when the user comes back.
 */
@Stable
class ToastQueue internal constructor() {
    internal var current by mutableStateOf<ToastMessage?>(null)
        private set

    /** Bumped when the message on screen is shown again, which restarts its time. */
    internal var restarts by mutableIntStateOf(0)
        private set

    private val waiting = ArrayDeque<ToastMessage>()

    fun show(message: ToastMessage) {
        val showing = current
        when {
            showing == null -> current = message
            showing.sameAs(message) -> restarts++
            waiting.none { it.sameAs(message) } -> waiting.addLast(message)
        }
    }

    /** Takes the toast on screen away now, and brings on the next one. */
    fun dismiss() {
        current = waiting.removeFirstOrNull()
    }
}

/** A toast's look, used by the toast layer and on its own wherever one is shown inline. */
@Composable
fun AppToast(
    message: ToastMessage,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val tone = message.tone.colors
    if (message.hud) {
        AppSurface(
            modifier = modifier.size(HudSize),
            shape = AppTheme.shapes.lg,
            color = colors.surfaceElevated,
            elevation = AppTheme.elevation.overlay,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm, Alignment.CenterVertically),
            ) {
                AppIcon(message.tone.icon, contentDescription = null, tint = tone.content, size = 40.dp)
                AppText(text = message.text, style = AppTheme.typography.subheadline, color = colors.contentPrimary, maxLines = 2)
            }
        }
        return
    }
    AppSurface(
        modifier = modifier.fillMaxWidth().widthIn(max = AppTheme.layout.sheetMaxWidth),
        shape = AppTheme.shapes.lg,
        color = colors.surfaceElevated,
        elevation = AppTheme.elevation.overlay,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (message.tone != AppTone.Neutral) AppIcon(message.tone.icon, contentDescription = null, tint = tone.content)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                message.title?.let { AppText(text = it, style = AppTheme.typography.headline, color = colors.contentPrimary) }
                AppText(text = message.text, style = AppTheme.typography.subheadline, color = colors.contentPrimary)
            }
            if (message.actionLabel != null && message.onAction != null) {
                AppLinkButton(
                    text = message.actionLabel,
                    onClick = {
                        message.onAction.invoke()
                        onDismiss()
                    },
                    style = AppTheme.typography.headline,
                )
            }
        }
    }
}

/**
 * Layer 5. The toast rises from above whatever covers the bottom edge (the tab bar, an action bar,
 * the keyboard) as the shell reports it, can be swiped down, and leaves on a spring. While [held]
 * returns true its time stands still and it gives no haptic, as while the app is in the background.
 */
@Composable
internal fun ToastLayer(queue: ToastQueue, held: () -> Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val bottomChrome = LocalBottomChrome.current
    val navigationBars = WindowInsets.navigationBars
    val haptics = rememberAppHaptics()
    val springs = AppTheme.springs
    val message = queue.current

    LaunchedEffect(message) {
        snapshotFlow(held).first { !it }
        when (message?.tone) {
            AppTone.Success -> haptics.perform(AppHaptic.Confirm)
            AppTone.Error -> haptics.perform(AppHaptic.Reject)
            else -> Unit
        }
    }
    LaunchedEffect(message, queue.restarts) {
        val showing = message ?: return@LaunchedEffect
        var left = showing.durationMillis
        while (left > 0) {
            lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.STARTED) }
            snapshotFlow(held).first { !it }
            delay(Tick)
            left -= Tick
        }
        if (queue.current === showing) queue.dismiss()
    }

    // The container holds the whole window and does not animate itself: each toast moves by its
    // own height, measured from where it rests, so it rises from the bottom edge and stops above the
    // chrome rather than travelling the height of the window.
    AnimatedContent(
        targetState = message,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None using null },
        contentKey = { it },
        label = "toast",
    ) { shown ->
        if (shown == null) return@AnimatedContent
        if (shown.hud) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppToast(
                    message = shown,
                    modifier = Modifier
                        .animateEnterExit(
                            enter = fadeIn(springs.fadeIn()) + scaleIn(springs.emphasis.unit, initialScale = HudStartScale),
                            exit = fadeOut(springs.fadeOut()),
                        )
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                SwipeAway(
                    onGone = queue::dismiss,
                    modifier = Modifier.animateEnterExit(
                        enter = slideInVertically(springs.sheet.offset) { it } + fadeIn(springs.fadeIn()),
                        exit = slideOutVertically(springs.sheet.offset) { it } + fadeOut(springs.fadeOut()),
                    ),
                ) {
                    AppToast(
                        message = shown,
                        onDismiss = queue::dismiss,
                        modifier = Modifier
                            .padding(horizontal = AppTheme.spacing.gutter)
                            .aboveBottomChrome(bottomChrome, navigationBars)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }
    }
}

/**
 * Lifts the toast above the bottom chrome and the navigation bar. Read while laying out, so a bar
 * that changes height moves the toast without recomposing it.
 */
private fun Modifier.aboveBottomChrome(chrome: ChromeInset, navigationBars: WindowInsets): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val lift = maxOf(chrome.value.roundToPx(), navigationBars.getBottom(this)) + ToastGap.roundToPx()
        layout(placeable.width, placeable.height + lift) { placeable.place(0, 0) }
    }

/**
 * A vertical drag moves the content as a plain float. A release that would coast far enough down
 * dismisses it, so a flick goes even from close to where it rests; anything else springs back at
 * the finger's speed.
 */
@Composable
private fun SwipeAway(onGone: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    var drag by remember { mutableFloatStateOf(0f) }
    val spring = AppTheme.springs.sheet.pixels
    val dismissAt = with(LocalDensity.current) { DismissDistance.toPx() }
    Box(
        modifier = modifier
            .graphicsLayer { translationY = drag.coerceAtLeast(0f) }
            .draggable(
                state = rememberDraggableState { delta -> drag = (drag + delta).coerceAtLeast(-Resisted) },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (drag + velocity * ProjectionSeconds > dismissAt) {
                        onGone()
                    } else {
                        scope.launch { animate(drag, 0f, velocity, spring) { value, _ -> drag = value } }
                    }
                },
            ),
    ) {
        content()
    }
}

private const val Tick = 100L
private val DismissDistance = 48.dp

// How far ahead of the toast a release looks for where it was thrown (iOS's 0.99 projection).
private const val ProjectionSeconds = 0.099f
private const val Resisted = 12f
private val ToastGap = 12.dp
private val HudSize = 140.dp
private const val HudStartScale = 0.9f
