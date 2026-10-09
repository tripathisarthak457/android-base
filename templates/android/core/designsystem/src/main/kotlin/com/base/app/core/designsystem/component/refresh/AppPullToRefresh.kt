package com.base.app.core.designsystem.component.refresh

import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.feedback.ACTIVITY_SPOKES
import com.base.app.core.designsystem.component.feedback.drawActivitySpokes
import com.base.app.core.designsystem.foundation.AppHaptic
import com.base.app.core.designsystem.foundation.LocalHapticsEnabled
import com.base.app.core.designsystem.foundation.LocalTopChrome
import com.base.app.core.designsystem.foundation.constant
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.LocalAppSprings
import com.base.app.core.designsystem.theme.LocalReduceMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow

/**
 * Pull to refresh over any scrolling content, as iOS's refresh control behaves. The content follows
 * the finger with rubber-band resistance and the spinner's spokes appear one by one in the gap it
 * opens. When the gap reaches the trigger distance the refresh starts there and then, with a haptic
 * tick, without waiting for the finger to lift. On release the content rests at the spinner's
 * height while [isRefreshing], a gap that scrolls away with the content as a content inset does,
 * and springs back with the finger's velocity when it ends. A refresh that ends at once still
 * shows its spinner briefly, so the pull is seen to have done something.
 *
 * A refresh the user did not pull for shows nothing unless [showExternalRefresh] asks, so a cached
 * list does not flash a spinner on every load. The spinner sits below whatever top bar the content
 * runs under.
 *
 * Pulling, settling and spinning only move a layer and redraw the spinner: nothing recomposes
 * except when [isRefreshing] changes.
 */
@Composable
fun AppPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    showExternalRefresh: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val spokeColor = remember(colors) { ColorProducer { colors.contentSecondary } }
    Box(
        modifier = modifier.then(
            PullToRefreshElement(
                refreshing = isRefreshing,
                onRefresh = onRefresh,
                showExternalRefresh = showExternalRefresh,
                spokeColor = spokeColor,
                refreshLabel = stringResource(R.string.designsystem_refresh),
                refreshingLabel = stringResource(R.string.designsystem_refreshing),
            ),
        ),
        content = content,
    )
}

private data class PullToRefreshElement(
    val refreshing: Boolean,
    val onRefresh: () -> Unit,
    val showExternalRefresh: Boolean,
    val spokeColor: ColorProducer,
    val refreshLabel: String,
    val refreshingLabel: String,
) : ModifierNodeElement<PullToRefreshNode>() {
    override fun create() =
        PullToRefreshNode(refreshing, onRefresh, showExternalRefresh, spokeColor, refreshLabel, refreshingLabel)

    override fun update(node: PullToRefreshNode) =
        node.update(refreshing, onRefresh, showExternalRefresh, spokeColor, refreshLabel, refreshingLabel)

    override fun InspectorInfo.inspectableProperties() {
        name = "pullToRefresh"
        properties["refreshing"] = refreshing
    }
}

/**
 * The whole control in one node: it is the nested-scroll connection that takes the pull, the layout
 * that moves the content by the gap, and the drawing of the spinner in the gap.
 *
 * `raw` is the finger travel the gap stands for. Up to `linear` the gap follows the finger one to
 * one (the resting gap of a running refresh); past it the rubber band applies, so the content never
 * stops dead under the finger.
 */
private class PullToRefreshNode(
    private var refreshing: Boolean,
    private var onRefresh: () -> Unit,
    private var showExternalRefresh: Boolean,
    private var spokeColor: ColorProducer,
    private var refreshLabel: String,
    private var refreshingLabel: String,
) : DelegatingNode(),
    NestedScrollConnection,
    LayoutModifierNode,
    DrawModifierNode,
    SemanticsModifierNode,
    CompositionLocalConsumerModifierNode {

    override val shouldAutoInvalidate: Boolean
        get() = false

    /** The visible gap above the content, in pixels. Read only in the layer block and in draw. */
    private var gap by mutableFloatStateOf(0f)
    private var raw = 0f
    private var linear = 0f
    private var dragging = false

    /** One pull starts one refresh, even when that refresh ends while the finger still holds the gap open. */
    private var triggeredThisDrag = false

    /** Showing a refresh: pulled past the trigger, or an external one shown on request. */
    private var active = false

    /** A finished refresh's spinner, still turning while the gap closes over it. */
    private var retracting = false
    private var activeSince = 0L
    private var head = 0

    private var settle: Job? = null
    private var spin: Job? = null
    private var finish: Job? = null

    private var viewport = 1f
    private var trigger = 0f
    private var rest = 0f

    init {
        delegate(nestedScrollModifierNode(this, null))
    }

    fun update(
        refreshing: Boolean,
        onRefresh: () -> Unit,
        showExternalRefresh: Boolean,
        spokeColor: ColorProducer,
        refreshLabel: String,
        refreshingLabel: String,
    ) {
        this.onRefresh = onRefresh
        this.showExternalRefresh = showExternalRefresh
        if (spokeColor != this.spokeColor) {
            this.spokeColor = spokeColor
            invalidateDraw()
        }
        if (refreshLabel != this.refreshLabel || refreshingLabel != this.refreshingLabel) {
            this.refreshLabel = refreshLabel
            this.refreshingLabel = refreshingLabel
            invalidateSemantics()
        }
        if (refreshing == this.refreshing) return
        this.refreshing = refreshing
        if (refreshing) {
            finish?.cancel()
            if (!active && showExternalRefresh && !dragging) {
                activate()
                settleTo(rest, velocity = 0f)
            }
        } else if (active) {
            finishAfterMinimum()
        }
        invalidateSemantics()
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y >= 0f || gap <= 0f) return Offset.Zero
        if (source == NestedScrollSource.SideEffect) {
            if (dragging) return Offset.Zero
            // A fling up through the gap closes it first, as a content inset scrolls away.
            settle?.cancel()
            val give = min(gap, -available.y)
            gap -= give
            raw = travelFor(gap)
            return Offset(0f, -give)
        }
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        beginDrag()
        // Pushing up closes the gap before the content scrolls.
        val give = min(raw, -available.y)
        raw -= give
        gap = visible(raw)
        return Offset(0f, -give)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
        beginDrag()
        raw += available.y
        gap = visible(raw)
        if (!active && !refreshing && !triggeredThisDrag && gap >= trigger) {
            triggeredThisDrag = true
            activate()
            perform(AppHaptic.Threshold)
            onRefresh()
            finishAfterMinimum()
        }
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!dragging) return Velocity.Zero
        dragging = false
        if (gap <= 0f) return Velocity.Zero
        // Flicked up: the content's own fling carries on and closes the gap on its way (onPreScroll).
        if (available.y < 0f) return Velocity.Zero
        // Let go inside a running refresh's gap: it stays where the finger left it, as an inset does.
        if (active && gap <= rest) return Velocity.Zero
        settleTo(if (active) rest else 0f, velocity = available.y * slope())
        // The release belongs to the pull, not to a fling of the content.
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (dragging || settle?.isActive == true) return Velocity.Zero
        when {
            !active && gap > 0f -> settleTo(0f, velocity = 0f)
            active && gap > rest -> settleTo(rest, velocity = 0f)
        }
        return Velocity.Zero
    }

    private fun beginDrag() {
        if (dragging) return
        dragging = true
        triggeredThisDrag = false
        settle?.cancel()
        linear = if (active) rest else 0f
        raw = travelFor(gap)
    }

    private fun visible(travel: Float): Float =
        if (travel <= linear) travel else linear + rubberBand(travel - linear, viewport - linear)

    private fun travelFor(visible: Float): Float =
        if (visible <= linear) visible else linear + rubberBandInverse(visible - linear, viewport - linear)

    /** How far the gap moves per pixel of finger travel where it is now: the band's slope. */
    private fun slope(): Float {
        if (raw <= linear) return 1f
        val range = (viewport - linear).coerceAtLeast(1f)
        return RESISTANCE * (1f - (gap - linear) / range).coerceAtLeast(0f).pow(2)
    }

    private fun activate() {
        active = true
        retracting = false
        activeSince = SystemClock.uptimeMillis()
        startSpin()
        invalidateDraw()
        invalidateSemantics()
    }

    /**
     * Retracts once the refresh is over and the spinner has been up for long enough to be seen.
     * Called on arming too, for a refresh the caller finished before telling us it had started.
     */
    private fun finishAfterMinimum() {
        finish?.cancel()
        finish = coroutineScope.launch {
            val shown = SystemClock.uptimeMillis() - activeSince
            if (shown < MIN_VISIBLE_MILLIS) delay(MIN_VISIBLE_MILLIS - shown)
            if (refreshing) return@launch
            active = false
            retracting = true
            invalidateSemantics()
            if (!dragging) settleTo(0f, velocity = 0f)
        }
    }

    private fun settleTo(target: Float, velocity: Float) {
        settle?.cancel()
        settle = coroutineScope.launch {
            val spring = currentValueOf(LocalAppSprings).bounceBack.pixels
            animate(gap, target, velocity, spring) { value, _ -> gap = value.coerceAtLeast(0f) }
            if (gap == 0f && !active) {
                retracting = false
                spin?.cancel()
                invalidateDraw()
            }
        }
    }

    private fun startSpin() {
        if (spin?.isActive == true) return
        spin = coroutineScope.launch {
            // iOS steps the spinner rather than turning it, so it redraws eight times a turn, not every frame.
            while (isActive) {
                val step = if (currentValueOf(LocalReduceMotion).value) SPIN_STEP_MILLIS * 2 else SPIN_STEP_MILLIS
                delay(step)
                head = (head + 1) % ACTIVITY_SPOKES
                invalidateDraw()
            }
        }
    }

    private fun perform(haptic: AppHaptic) {
        if (currentValueOf(LocalHapticsEnabled)) currentValueOf(LocalView).performHapticFeedback(haptic.constant())
    }

    override fun onDetach() {
        gap = 0f
        raw = 0f
        dragging = false
        triggeredThisDrag = false
        active = false
        retracting = false
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        viewport = placeable.height.toFloat().coerceAtLeast(1f)
        trigger = Trigger.toPx()
        rest = Rest.toPx()
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) { translationY = gap }
        }
    }

    override fun ContentDrawScope.draw() {
        val shown = gap
        if (shown > 0.5f) {
            // Content runs under the top bar, so the gap opens just below it.
            val top = currentValueOf(LocalTopChrome).value.toPx()
            val radius = SpinnerRadius.toPx()
            val spinning = active || retracting
            clipRect(top = top, bottom = top + shown) {
                val pulled = (shown / trigger).coerceIn(0f, 1f)
                drawActivitySpokes(
                    center = Offset(size.width / 2f, top + SpinnerTop.toPx() + radius),
                    radius = if (retracting) radius * (shown / rest).coerceIn(0f, 1f) else radius,
                    color = spokeColor(),
                    head = if (spinning) head else null,
                    revealed = if (spinning) ACTIVITY_SPOKES else ceil(ACTIVITY_SPOKES * pulled).toInt(),
                    alpha = if (spinning) 1f else FastOutSlowInEasing.transform((pulled / FADE_IN_FRACTION).coerceAtMost(1f)),
                )
            }
        }
        drawContent()
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (active || refreshing) {
            stateDescription = refreshingLabel
        } else {
            customActions = listOf(
                CustomAccessibilityAction(refreshLabel) {
                    activate()
                    settleTo(rest, velocity = 0f)
                    onRefresh()
                    finishAfterMinimum()
                    true
                },
            )
        }
    }
}

/** iOS's rubber band: `y = c·x·d / (d + c·x)`, so the further it is pulled the less it gives. */
private fun rubberBand(travel: Float, range: Float): Float {
    val d = range.coerceAtLeast(1f)
    return travel * RESISTANCE * d / (d + travel * RESISTANCE)
}

private fun rubberBandInverse(visible: Float, range: Float): Float {
    val d = range.coerceAtLeast(1f)
    val y = visible.coerceAtMost(d * 0.999f)
    return d * y / (RESISTANCE * (d - y))
}

private const val RESISTANCE = 0.55f
private const val SPIN_STEP_MILLIS = 125L
private const val MIN_VISIBLE_MILLIS = 400L

/** The spinner fades in over the first part of the pull, then the spokes alone show progress. */
private const val FADE_IN_FRACTION = 0.35f
private val Trigger = 100.dp
private val Rest = 60.dp
private val SpinnerRadius = 14.dp
private val SpinnerTop = 16.dp
