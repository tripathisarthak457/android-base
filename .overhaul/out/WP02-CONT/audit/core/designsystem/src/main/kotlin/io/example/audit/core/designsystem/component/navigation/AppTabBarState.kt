package io.example.audit.core.designsystem.component.navigation

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The bar's minimize state. [nestedScrollConnection] goes around the content that drives it; the
 * shell installs it around the visible tab.
 *
 * Reading on (the content scrolled down) past a short hysteresis folds the bar to its selected tab;
 * scrolling back up past it, reaching the top or tapping the bar opens it. The change is a state,
 * animated on its own spring and reversed with its velocity, as iOS's tab bar minimize is: scrubbed
 * by the scroll, the fold passed in one or two frames of a normal fling and read as a jump.
 *
 * [collapse] runs from 0 (full bar) to 1 (minimized); read it only in layout and draw.
 * [settledHeight] changes only once the bar is at rest, so content padding built from it never
 * moves while the user is scrolling.
 */
@Stable
class AppTabBarState internal constructor(private val density: Density, private val scope: CoroutineScope) {

    @get:FrequentlyChangingValue
    var collapse by mutableFloatStateOf(0f)
        private set

    /** The pixels the bar takes from the bottom of the window, written only when it is at rest. */
    var settledHeight by mutableIntStateOf(0)
        private set

    internal var behavior = BarMinimize.Never
        set(value) {
            field = value
            if (value == BarMinimize.Never) fold(minimized = false)
        }

    private var latestHeight = 0
    private var target = 0f
    private var velocity = 0f

    /** Scroll since the direction last changed, in px; positive while reading on. */
    private var drift = 0f
    private var motion: Job? = null

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (behavior == BarMinimize.Never) return Offset.Zero
            // Scroll up that the content could not take: it is at the top.
            if (available.y > 0f) {
                fold(minimized = false)
                return Offset.Zero
            }
            val readOn = -consumed.y
            if (readOn == 0f) return Offset.Zero
            drift = if (sign(drift) == sign(readOn)) drift + readOn else readOn
            if (abs(drift) >= with(density) { Hysteresis.toPx() }) fold(minimized = drift > 0f)
            return Offset.Zero
        }
    }

    /** Opens a minimized bar, as tapping it does, and returns once it is open. */
    suspend fun expand() {
        fold(minimized = false)
        motion?.join()
    }

    private fun fold(minimized: Boolean) {
        val to = if (minimized) 1f else 0f
        if (to == target) return
        target = to
        motion?.cancel()
        motion = scope.launch {
            animate(collapse, to, velocity, MinimizeSpring) { value, speed ->
                collapse = value
                velocity = speed
            }
            velocity = 0f
            settledHeight = latestHeight
        }
    }

    internal fun onMeasured(height: Int) {
        latestHeight = height
        if (motion?.isActive != true) settledHeight = height
    }
}

/** An [AppTabBarState] kept for as long as the caller is composed, with the bar open. */
@Composable
fun rememberAppTabBarState(): AppTabBarState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    return remember(density, scope) { AppTabBarState(density, scope) }
}

private val Hysteresis = 10.dp

/** iOS's segmented-thumb spring, response 0.28 s with no bounce: the bar moves, it does not wobble. */
private val MinimizeSpring = spring(dampingRatio = 1f, stiffness = 503.5f, visibilityThreshold = 0.001f)
