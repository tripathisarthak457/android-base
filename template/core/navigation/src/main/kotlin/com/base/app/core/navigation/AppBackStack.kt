package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer

/**
 * The back stack, and the rules for changing it.
 *
 * Plain Compose state that this module owns outright, rather than something the navigation
 * library hides. Every transition in [NavTransitions] animates against it, and a stack that can
 * only be reached through a library API cannot be read during an animation frame.
 */
class AppBackStack internal constructor(
    internal val entries: SnapshotStateList<AppNavKey>,
) {
    val current: AppNavKey get() = entries.last()

    val canGoBack: Boolean get() = entries.size > 1

    val size: Int get() = entries.size

    fun contains(key: AppNavKey): Boolean = entries.contains(key)

    /**
     * Applies one command.
     *
     * Kept here, next to the stack, rather than in the host composable — this is the whole of the
     * project's navigation semantics, and it is exactly the logic worth having a test for. See
     * `AppBackStackTest`.
     */
    fun apply(command: NavCommand) {
        when (command) {
            is NavCommand.Navigate -> {
                // A pop that is part of a navigate may empty the stack, because the push that
                // follows immediately refills it. `Navigate(Home, popUpTo = Login, inclusive =
                // true)` is exactly the sign-in case, and it has to be able to remove the last
                // pre-existing entry.
                command.popUpTo?.let { popTo(it, command.inclusive, allowEmpty = true) }
                if (command.singleTop && entries.lastOrNull() == command.key) return
                entries.add(command.key)
            }

            NavCommand.Up -> if (canGoBack) entries.removeAt(entries.lastIndex)

            is NavCommand.ResetTo -> {
                entries.clear()
                entries.add(command.key)
            }

            is NavCommand.PopTo -> popTo(command.key, command.inclusive)
        }
    }

    /**
     * Pops down to [key]. Does nothing if it is not on the stack.
     *
     * Searching from the top, not the bottom: with a stack like Home → Detail → Home → Detail,
     * "pop to Home" means the Home the user just came from, not the one at the root.
     */
    private fun popTo(key: AppNavKey, inclusive: Boolean, allowEmpty: Boolean = false) {
        val index = entries.indexOfLast { it == key }
        if (index < 0) return
        val keepCount = if (inclusive) index else index + 1
        // A standalone pop must leave something behind: a display with nothing to show crashes,
        // and a caller that wants a genuinely fresh start means ResetTo.
        val floor = if (allowEmpty) keepCount else keepCount.coerceAtLeast(1)
        while (entries.size > floor) entries.removeAt(entries.lastIndex)
    }
}

/**
 * A back stack that survives process death.
 *
 * The whole stack round-trips as one polymorphic JSON string. The alternative — a hand-written
 * `toSavedName()`/`fromSavedName()` pair — is a branch per destination in two places that has to
 * be edited every time a key gains a parameter, and silently drops the stack when only one of the
 * two is updated. This has no per-destination code at all, so a new destination cannot be
 * forgotten.
 *
 * A stack that fails to restore falls back to [startKey] rather than throwing. The realistic
 * cause is an app update that removed a destination the user was on; landing them at the start
 * is a far better outcome than a crash on resume, and it is invisible to everyone else.
 */
@Composable
fun rememberAppBackStack(
    startKey: AppNavKey,
    serialization: NavKeySerialization,
): AppBackStack {
    val saver = remember(serialization, startKey) { backStackSaver(startKey, serialization) }
    // Keyed on startKey: when the app moves past a gate — onboarding finished, or a session
    // established — the entry point changes, and the stack has to start again from the new one.
    // Without the key the old stack survives and the app shows the walkthrough behind Back.
    return rememberSaveable(startKey, saver = saver) {
        AppBackStack(mutableStateListOf(startKey))
    }
}

private fun backStackSaver(
    startKey: AppNavKey,
    serialization: NavKeySerialization,
): Saver<AppBackStack, String> {
    val listSerializer = ListSerializer(PolymorphicSerializer(AppNavKey::class))

    return Saver(
        save = { stack ->
            runCatching {
                serialization.json.encodeToString(listSerializer, stack.entries.toList())
            }.getOrNull()
        },
        restore = { encoded ->
            val keys = runCatching { serialization.json.decodeFromString(listSerializer, encoded) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?: listOf(startKey)
            AppBackStack(mutableStateListOf<AppNavKey>().apply { addAll(keys) })
        },
    )
}
