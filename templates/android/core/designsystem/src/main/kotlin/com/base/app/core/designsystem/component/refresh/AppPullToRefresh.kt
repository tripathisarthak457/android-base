package com.base.app.core.designsystem.component.refresh

import android.os.SystemClock
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.feedback.ActivitySpokes
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.LocalHapticsEnabled
import com.base.app.core.designsystem.foundation.interaction.constant
import com.base.app.core.designsystem.foundation.layout.ChromeInset
import com.base.app.core.designsystem.foundation.layout.LocalTopChrome
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.LocalAppSprings
import com.base.app.core.designsystem.theme.LocalReduceMotion
import com.base.app.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
public fun AppPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    showExternalRefresh: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val spokeColor = remember(colors) { ColorProducer { colors.contentSecondary } }
    val indicator = remember { RefreshIndicator() }
    val topChrome = LocalTopChrome.current
    val measurePolicy = remember(topChrome) { RefreshMeasurePolicy(topChrome) }
    Layout(
        contents = listOf(
            {
                Box(Modifier.offset { IntOffset(0, indicator.gap.roundToInt()) }, content = content)
            },
            // Its own layer, so each step of the spinner redraws a few dozen pixels, not the screen.
            { Spacer(Modifier.graphicsLayer().then(RefreshSpinnerElement(indicator, spokeColor))) },
        ),
        modifier = modifier.then(
            PullToRefreshElement(
                indicator = indicator,
                refreshing = isRefreshing,
                onRefresh = onRefresh,
                showExternalRefresh = showExternalRefresh,
                refreshLabel = stringResource(R.string.designsystem_refresh),
                refreshingLabel = stringResource(R.string.designsystem_refreshing),
            ),
        ),
        measurePolicy = measurePolicy,
    )
}

/** The content as measured, and the spinner centred just below whatever top bar the content runs under. */
private class RefreshMeasurePolicy(private val topChrome: ChromeInset) : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val (body, spinnerSlot) = measurables
        val content = body.first().measure(constraints)
        val diameter = (AppPullToRefreshDefaults.SpinnerRadius * 2).roundToPx()
        val spinner = spinnerSlot.first().measure(Constraints.fixed(diameter, diameter))
        return layout(content.width, content.height) {
            content.place(0, 0)
            spinner.place(
                (content.width - diameter) / 2,
                (
                    topChrome.value +
                        AppPullToRefreshDefaults.SpinnerTop
                    ).roundToPx(),
            )
        }
    }
}

private data class PullToRefreshElement(
    val indicator: RefreshIndicator,
    val refreshing: Boolean,
    val onRefresh: () -> Unit,
    val showExternalRefresh: Boolean,
    val refreshLabel: String,
    val refreshingLabel: String,
) : ModifierNodeElement<PullToRefreshNode>() {
    override fun create() = PullToRefreshNode(
        indicator,
        refreshing,
        onRefresh,
        showExternalRefresh,
        refreshLabel,
        refreshingLabel,
    )

    override fun update(node: PullToRefreshNode) =
        node.update(refreshing, onRefresh, showExternalRefresh, refreshLabel, refreshingLabel)

    override fun InspectorInfo.inspectableProperties() {
        name = "pullToRefresh"
        properties["refreshing"] = refreshing
    }
}

/**
 * The pull itself: the nested-scroll connection that takes it, the gap the content moves down by,
 * the refresh it starts and the spinner's state. It draws nothing; the content's offset and the
 * spinner read [indicator].
 *
 * `raw` is the finger travel the gap stands for. Up to `linear` the gap follows the finger one to
 * one (the resting gap of a running refresh); past it the rubber band applies, so the content never
 * stops dead under the finger.
 */
private class PullToRefreshNode(
    private val indicator: RefreshIndicator,
    private var refreshing: Boolean,
    private var onRefresh: () -> Unit,
    private var showExternalRefresh: Boolean,
    private var refreshLabel: String,
    private var refreshingLabel: String,
) : DelegatingNode(),
    NestedScrollConnection,
    LayoutAwareModifierNode,
    SemanticsModifierNode,
    CompositionLocalConsumerModifierNode {

    override val shouldAutoInvalidate: Boolean
        get() = false

    private var gap: Float
        get() = indicator.gap
        set(value) {
            indicator.gap = value
        }
    private var raw = 0f
    private var linear = 0f
    private var dragging = false

    /** One pull starts one refresh, even when that refresh ends while the finger still holds the gap open. */
    private var triggeredThisDrag = false

    /** Showing a refresh: pulled past the trigger, or an external one shown on request. */
    private var active = false
    private var activeSince = 0L

    private var settle: Job? = null
    private var spin: Job? = null
    private var finish: Job? = null

    private var viewport = 1f
    private val trigger: Float
        get() = with(requireDensity()) { AppPullToRefreshDefaults.Trigger.toPx() }
    private val rest: Float
        get() = with(requireDensity()) { AppPullToRefreshDefaults.Rest.toPx() }

    init {
        delegate(nestedScrollModifierNode(this, null))
    }

    fun update(
        refreshing: Boolean,
        onRefresh: () -> Unit,
        showExternalRefresh: Boolean,
        refreshLabel: String,
        refreshingLabel: String,
    ) {
        this.onRefresh = onRefresh
        this.showExternalRefresh = showExternalRefresh
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

    override fun onRemeasured(size: IntSize) {
        viewport = size.height.toFloat().coerceAtLeast(1f)
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

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
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

    private fun travelFor(visible: Float): Float = if (visible <=
        linear
    ) {
        visible
    } else {
        linear + rubberBandInverse(visible - linear, viewport - linear)
    }

    /** How far the gap moves per pixel of finger travel where it is now: the band's slope. */
    private fun slope(): Float {
        if (raw <= linear) return 1f
        val range = (viewport - linear).coerceAtLeast(1f)
        return Resistance * (1f - (gap - linear) / range).coerceAtLeast(0f).pow(2)
    }

    private fun activate() {
        active = true
        activeSince = SystemClock.uptimeMillis()
        indicator.retracting = false
        indicator.spinning = true
        startSpin()
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
            if (shown < MinVisibleMillis) delay(MinVisibleMillis - shown)
            if (refreshing) return@launch
            active = false
            indicator.retracting = true
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
                indicator.retracting = false
                indicator.spinning = false
                spin?.cancel()
            }
        }
    }

    private fun startSpin() {
        if (spin?.isActive == true) return
        spin = coroutineScope.launch {
            // iOS steps the spinner rather than turning it, so it redraws eight times a turn, not every frame.
            while (isActive) {
                val step = if (currentValueOf(
                        LocalReduceMotion,
                    ).value
                ) {
                    SpinStepMillis * 2
                } else {
                    SpinStepMillis
                }
                delay(step)
                indicator.head = (indicator.head + 1) % ActivitySpokes
            }
        }
    }

    private fun perform(haptic: AppHaptic) {
        if (currentValueOf(
                LocalHapticsEnabled,
            )
        ) {
            currentValueOf(LocalView).performHapticFeedback(haptic.constant())
        }
    }

    override fun onDetach() {
        gap = 0f
        raw = 0f
        dragging = false
        triggeredThisDrag = false
        active = false
        indicator.spinning = false
        indicator.retracting = false
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
    return travel * Resistance * d / (d + travel * Resistance)
}

private fun rubberBandInverse(visible: Float, range: Float): Float {
    val d = range.coerceAtLeast(1f)
    val y = visible.coerceAtMost(d * 0.999f)
    return d * y / (Resistance * (d - y))
}

private const val Resistance = DesignTokens.Motion.rubberBand
private const val SpinStepMillis = 125L
private const val MinVisibleMillis = 400L

/** How far a pull refreshes, where the content rests while it does, and the spinner. */
public object AppPullToRefreshDefaults {
    public val Trigger: Dp = 100.dp
    public val Rest: Dp = 60.dp
    public val SpinnerRadius: Dp = 14.dp
    public val SpinnerTop: Dp = 16.dp
}

@AppComponentPreviews
@Composable
private fun AppPullToRefreshPreview() {
    Row(
        modifier = Modifier.background(
            AppTheme.colors.background,
        ).height(AppTheme.sizes.rowHeight * 4),
    ) {
        AppPullToRefresh(isRefreshing = true, onRefresh = {}, modifier = Modifier.weight(1f)) {
            Column {
                AppRow(title = "Refreshing")
                AppRow(title = "Inbox")
            }
        }
        AppPullToRefresh(isRefreshing = false, onRefresh = {}, modifier = Modifier.weight(1f)) {
            Column {
                AppRow(title = "Settled")
                AppRow(title = "Inbox")
            }
        }
    }
}
