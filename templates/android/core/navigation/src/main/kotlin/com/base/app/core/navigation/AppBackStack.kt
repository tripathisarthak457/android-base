package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer

/** The back stack, and the rules for changing it. */
class AppBackStack internal constructor(
    internal val entries: SnapshotStateList<AppNavKey>,
) {
    val current: AppNavKey get() = entries.last()

    val canGoBack: Boolean get() = entries.size > 1

    val size: Int get() = entries.size

    fun contains(key: AppNavKey): Boolean = entries.contains(key)

    /** Applies one command. */
    fun apply(command: NavCommand) {
        when (command) {
            is NavCommand.Navigate -> {
                // A pop inside a navigate may empty the stack, since the push refills it (the
                // sign-in case).
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
 * A back stack that survives process death. The whole stack round-trips as one polymorphic JSON
 * string.
 */
@Composable
fun rememberAppBackStack(
    startKey: AppNavKey,
    serialization: NavKeySerialization,
): AppBackStack {
    val saver = remember(serialization, startKey) { backStackSaver(startKey, serialization) }
    // Keyed on startKey so passing onboarding or sign-in starts a fresh stack.
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
