package com.base.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.container.AppVerticalDivider
import com.base.app.core.designsystem.component.feedback.AppEmptyState
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * Which half of a list-detail pair a destination is. A [List] and the [Detail] opened from it sit
 * side by side when the window has room, and stack as two screens when it does not — the same
 * back stack, the same keys, nothing in the feature knowing which.
 */
enum class NavPane { Single, List, Detail }

internal const val PANE_METADATA_KEY = "com.base.app.navigation.pane"

internal val NavEntry<*>.pane: NavPane
    get() = metadata[PANE_METADATA_KEY] as? NavPane ?: NavPane.Single

/**
 * Which entries a two-pane scene shows, as indices into the stack: the list, and the detail open
 * beside it (null for none yet). Null when the top of the stack is not a list with details over it.
 *
 * Details opened one after another from the same list stack up, and the newest is shown; Back steps
 * through the earlier ones in the detail pane before it closes the pane.
 */
internal fun listDetailPanes(panes: List<NavPane>): Pair<Int, Int?>? {
    val top = panes.lastIndex
    return when (panes.lastOrNull()) {
        NavPane.List -> top to null
        NavPane.Detail -> {
            val list = panes.indexOfLast { it != NavPane.Detail }
            if (list >= 0 && panes[list] == NavPane.List) list to top else null
        }
        else -> null
    }
}

/** Puts a list and its detail side by side whenever [twoPanes] is true. */
internal data class ListDetailSceneStrategy(
    private val twoPanes: Boolean,
    private val listPaneWidth: Dp,
    private val motion: ScreenMotion,
    private val gate: BackGate,
) : SceneStrategy<AppNavKey> {

    override fun SceneStrategyScope<AppNavKey>.calculateScene(
        entries: List<NavEntry<AppNavKey>>,
    ): Scene<AppNavKey>? {
        if (!twoPanes) return null
        val (listIndex, detailIndex) = listDetailPanes(entries.map { it.pane }) ?: return null
        return ListDetailScene(
            list = entries[listIndex],
            detail = detailIndex?.let(entries::get),
            previousEntries = entries.dropLast(1),
            listPaneWidth = listPaneWidth,
            motion = motion,
            gate = gate,
        )
    }
}

private class ListDetailScene(
    private val list: NavEntry<AppNavKey>,
    private val detail: NavEntry<AppNavKey>?,
    override val previousEntries: List<NavEntry<AppNavKey>>,
    private val listPaneWidth: Dp,
    private val motion: ScreenMotion,
    private val gate: BackGate,
) : Scene<AppNavKey> {

    // Keyed by the list, so opening another item swaps the detail pane in place instead of
    // animating the whole screen away and back.
    override val key: Any = list.contentKey

    override val entries: List<NavEntry<AppNavKey>> = listOfNotNull(list, detail)

    override val content: @Composable () -> Unit = {
        val transition = LocalNavAnimatedContentScope.current.transition
        ReportMotion(transition, transition.arrival(motion), gate)
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.width(listPaneWidth).fillMaxHeight()) { list.Content() }
            AppVerticalDivider()
            // Swapped, not cross-faded: the pane changes in the frame the selection does.
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                key(detail?.contentKey) {
                    if (detail != null) detail.Content() else NothingSelected()
                }
            }
        }
    }

    // Recomputed on every back-stack change; an equal scene must compare equal, or Navigation 3
    // starts a transition to a scene that looks identical.
    override fun equals(other: Any?): Boolean =
        other is ListDetailScene && list == other.list && detail == other.detail &&
            previousEntries == other.previousEntries && listPaneWidth == other.listPaneWidth &&
            motion == other.motion && gate === other.gate

    override fun hashCode(): Int =
        ((list.hashCode() * 31 + detail.hashCode()) * 31 + previousEntries.hashCode()) * 31 + listPaneWidth.hashCode()
}

@Composable
private fun NothingSelected() {
    AppEmptyState(
        title = stringResource(R.string.designsystem_nothing_selected),
        message = stringResource(R.string.designsystem_nothing_selected_message),
        icon = AppIcons.ListView,
    )
}
