package com.base.app.core.navigation.scene

import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.foundation.LocalTabVisible
import com.base.app.core.navigation.AppBackStack
import com.base.app.core.navigation.back.BackGate
import com.base.app.core.navigation.back.StackChange
import com.base.app.core.navigation.shell.BarAnchor
import com.base.app.core.navigation.shell.tabs.rememberTabLifecycle
import com.base.app.core.navigation.stack.hidesBar
import com.base.app.core.navigation.stack.navKey
import com.base.app.core.navigation.transition.ReportMotion
import com.base.app.core.navigation.transition.ScreenMotion
import com.base.app.core.navigation.transition.arrival
import kotlin.math.roundToInt

/**
 * The top of a stack, with the entry beneath it kept composed, as iOS keeps the covered view
 * controller alive. Navigation 3 renders each entry through movable content keyed by the entry, and
 * hands an entry to the scene that claims it: so when a pop makes the covered entry the top of the
 * next scene, its composition moves there instead of starting again, and the pop's first frame
 * lays out a screen that already exists rather than composing one from nothing.
 *
 * Once the scene has arrived, the entry beneath is neither measured nor placed, its lifecycle is
 * held at CREATED and [LocalTabVisible] is false, so it costs nothing per frame. While the scene
 * slides in over it in a push, it is drawn where the covered scene would draw it, following the top
 * entry by the parallax, because this scene now owns it and the covered scene shows nothing. When
 * the scene arrives by a pop, or is revealed by a back swipe, the entry beneath stays hidden under
 * the arriving top, so it is not laid out at all.
 */
internal class KeepUnderScene(
    private val top: NavEntry<AppNavKey>,
    private val under: NavEntry<AppNavKey>?,
    override val previousEntries: List<NavEntry<AppNavKey>>,
    private val motion: ScreenMotion,
    private val stack: AppBackStack,
    private val barFollow: BarFollow?,
) : Scene<AppNavKey> {

    private val gate: BackGate get() = stack.gate

    override val key: Any = top.contentKey

    override val entries: List<NavEntry<AppNavKey>> = listOfNotNull(under, top)

    override val content: @Composable () -> Unit = {
        val transition = LocalNavAnimatedContentScope.current.transition
        val arrived = transition.arrival(motion)
        ReportMotion(transition, arrived, gate)
        barFollow?.let { follow ->
            val progress = remember(follow, transition, arrived) {
                {
                    // Pushed further, the screen that hid the bar is still on the stack, under the
                    // new top: the bar stays hidden while this scene leaves. Popped, or swiped back
                    // (the stack changes only when the swipe completes), it follows the slide.
                    val pushedDeeper = transition.targetState != EnterExitState.Visible && follow.coveredDeeper()
                    if (pushedDeeper) 1f else arrived.value
                }
            }
            DisposableEffect(follow.anchor, progress) {
                follow.anchor.attach(progress)
                onDispose { follow.anchor.detach(progress) }
            }
        }
        Box(Modifier.fillMaxSize()) {
            if (under != null) {
                // Only a push slides this scene over the screen beneath; a swipe back to this scene
                // has not changed the stack yet, so its top is not the stack's top.
                val arriving = transition.targetState == EnterExitState.Visible &&
                    transition.currentState != EnterExitState.Visible &&
                    gate.lastChange == StackChange.Push && stack.current == top.navKey
                CompositionLocalProvider(
                    LocalLifecycleOwner provides rememberTabLifecycle(active = arriving),
                    LocalTabVisible provides arriving,
                ) {
                    Box(
                        Modifier.layout { measurable, constraints ->
                            if (!arriving) return@layout layout(0, 0) {}
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                // Where the covered screen would be, relative to this one: a whole
                                // width behind the arriving edge, closing to the parallax.
                                val width = placeable.width
                                val behind = -width + (1f - motion.parallax) * width * arrived.value
                                val x = if (layoutDirection == LayoutDirection.Ltr) behind else -behind
                                placeable.place(x.roundToInt(), 0)
                            }
                        },
                    ) { under.Content() }
                }
            }
            top.Content()
        }
    }

    override fun equals(other: Any?): Boolean =
        other is KeepUnderScene && top == other.top && under == other.under &&
            previousEntries == other.previousEntries && motion == other.motion &&
            stack === other.stack && barFollow == other.barFollow

    override fun hashCode(): Int =
        ((top.hashCode() * 31 + under.hashCode()) * 31 + previousEntries.hashCode()) * 31 + motion.hashCode()

    override fun toString(): String = "KeepUnderScene(top=${top.contentKey}, under=${under?.contentKey})"
}

/** A scene whose top hides the tab bar over a screen that shows it: the bar follows that screen out and back. */
internal data class BarFollow(val anchor: BarAnchor, val coveringKey: AppNavKey, val stack: AppBackStack) {
    fun coveredDeeper(): Boolean = stack.contains(coveringKey) && stack.current != coveringKey
}

internal class KeepUnderSceneStrategy(
    private val motion: ScreenMotion,
    private val stack: AppBackStack,
    private val anchor: BarAnchor?,
) : SceneStrategy<AppNavKey> {

    override fun SceneStrategyScope<AppNavKey>.calculateScene(entries: List<NavEntry<AppNavKey>>): Scene<AppNavKey> {
        val top = entries.last()
        val under = entries.getOrNull(entries.lastIndex - 1)
        // The bar leaves with the last screen that showed it; pushed deeper, it is already gone.
        val coveringKey = top.navKey.takeIf { top.hidesBar && under != null && !under.hidesBar }
        return KeepUnderScene(
            top = top,
            under = under,
            previousEntries = entries.dropLast(1),
            motion = motion,
            stack = stack,
            barFollow = if (anchor != null && coveringKey != null) BarFollow(anchor, coveringKey, stack) else null,
        )
    }

    override fun equals(other: Any?): Boolean =
        other is KeepUnderSceneStrategy && motion == other.motion && stack === other.stack && anchor === other.anchor

    override fun hashCode(): Int = (motion.hashCode() * 31 + stack.hashCode()) * 31 + anchor.hashCode()
}
