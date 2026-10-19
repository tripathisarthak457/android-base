package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.base.app.core.common.navigation.AppNavKey
import org.jetbrains.compose.resources.StringResource

/** A tab of the signed-in shell: the root destination it opens, and how the bar labels it. */
@Immutable
data class ShellTab(
    val key: AppNavKey,
    val label: StringResource,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

/** The signed-in shell's navigation state: one back stack per tab, and the tab in front. */
@Stable
class ShellState internal constructor(
    internal val stacks: List<AppBackStack>,
    initialTab: Int,
) {
    var selectedIndex: Int by mutableIntStateOf(initialTab)
        private set

    /** The stack of the tab in front, where navigation commands land. */
    val current: AppBackStack get() = stacks[selectedIndex]

    /**
     * Brings tab [index] to the front. Selecting the tab already in front pops its stack to the
     * root, as a second tap on an iOS tab does.
     */
    fun select(index: Int) {
        if (index !in stacks.indices) return
        if (index == selectedIndex) {
            current.pop(current.size - 1)
        } else {
            selectedIndex = index
        }
    }
}

/**
 * The shell's state, restored after process death: the selected tab and every tab's stack, each
 * stack written as the polymorphic JSON of its keys.
 */
@Composable
fun rememberShellState(tabs: List<ShellTab>, serialization: NavKeySerialization): ShellState {
    val roots = remember(tabs) { tabs.map { it.key } }
    val saver = remember(roots, serialization) { shellSaver(roots, serialization) }
    return rememberSaveable(roots, saver = saver) { ShellState(roots.map(::stackOf), initialTab = 0) }
}

private fun stackOf(vararg keys: AppNavKey) = AppBackStack(mutableStateListOf(*keys))

private fun shellSaver(roots: List<AppNavKey>, serialization: NavKeySerialization): Saver<ShellState, Any> =
    Saver(
        save = { state ->
            listOf(state.selectedIndex.toString()) + state.stacks.map { serialization.encodeStack(it.keys) }
        },
        restore = { saved ->
            @Suppress("UNCHECKED_CAST")
            val values = saved as List<String?>
            val stacks = roots.mapIndexed { index, root ->
                // A stack that does not start at its tab's root belongs to another build of the
                // app, whose tabs were different: it starts again at the root.
                val keys = values.getOrNull(index + 1)?.let(serialization::decodeStack)
                    ?.takeIf { it.firstOrNull() == root }
                    ?: listOf(root)
                stackOf(*keys.toTypedArray())
            }
            ShellState(stacks, initialTab = values.first()?.toIntOrNull()?.coerceIn(roots.indices) ?: 0)
        },
    )
