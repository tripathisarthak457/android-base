package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.base.app.core.designsystem.component.overlay.AppSheet
import com.base.app.core.designsystem.component.overlay.AppSheetState
import com.base.app.core.designsystem.component.overlay.SheetDetent

/** How a destination presented as a sheet rests: its detents, and whether it shows a close button. */
@Immutable
data class SheetPresentation(
    val detents: Set<SheetDetent> = setOf(SheetDetent.Medium, SheetDetent.Large),
    val showCloseButton: Boolean = false,
)

/**
 * The sheet strategy of the shell's presentation host: an entry registered with
 * [SheetSceneStrategy.sheet] metadata is shown as an `AppSheet` over the entries beneath it, which
 * stay composed and visible behind the scrim.
 *
 * Navigation 3 needs something under an overlay, so the presentation host keeps an empty root entry
 * at the bottom of its stack; with nothing beneath, this strategy declines.
 *
 * Each sheet's state is kept across the scene being rebuilt for every back-stack change, so a pop
 * from anywhere (Back, a programmatic pop, a parent closing it) slides the sheet away before the
 * entry leaves, and a second Back while it slides does nothing.
 */
class SheetSceneStrategy<T : Any> : SceneStrategy<T> {
    private val states = mutableMapOf<Any, AppSheetState>()

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val top = entries.lastOrNull()
        val presentation = top?.metadata?.get(SheetKey)
        if (top == null || presentation == null) {
            states.clear()
            return null
        }
        val below = entries.dropLast(1)
        if (below.isEmpty()) return null
        states.keys.retainAll(setOf(top.contentKey))
        val state = states.getOrPut(top.contentKey) { AppSheetState(presentation.detents) }
        return SheetScene(top, below, state, presentation, onBack)
    }

    companion object {
        object SheetKey : NavMetadataKey<SheetPresentation>

        /** The metadata that marks an entry as a sheet. */
        fun sheet(presentation: SheetPresentation = SheetPresentation()): Map<String, Any> =
            metadata { put(SheetKey, presentation) }
    }
}

@Composable
fun <T : Any> rememberSheetSceneStrategy(): SheetSceneStrategy<T> = remember { SheetSceneStrategy() }

private class SheetScene<T : Any>(
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    private val state: AppSheetState,
    private val presentation: SheetPresentation,
    private val onBack: () -> Unit,
) : OverlayScene<T> {
    override val key: Any = entry.contentKey
    override val entries: List<NavEntry<T>> = listOf(entry)
    override val overlaidEntries: List<NavEntry<T>> = previousEntries

    override val content: @Composable () -> Unit = {
        AppSheet(state = state, onDismiss = onBack, showCloseButton = presentation.showCloseButton) {
            entry.Content()
        }
    }

    override suspend fun onRemove() {
        state.dismiss()
    }

    override fun equals(other: Any?): Boolean =
        other is SheetScene<*> && entry == other.entry && previousEntries == other.previousEntries && presentation == other.presentation

    override fun hashCode(): Int = (31 * entry.hashCode() + previousEntries.hashCode()) * 31 + presentation.hashCode()
}
