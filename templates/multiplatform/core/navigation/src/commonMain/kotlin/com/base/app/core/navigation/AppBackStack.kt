package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.base.app.core.common.log.Log
import com.base.app.core.common.navigation.AppNavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer

/**
 * One host's back stack and the rules for changing it. [floor] is how many entries Back never
 * pops: the root.
 */
@Stable
class AppBackStack internal constructor(
    internal val entries: SnapshotStateList<AppNavKey>,
    private val floor: Int = 1,
) {
    val current: AppNavKey get() = entries.last()

    val canGoBack: Boolean get() = entries.size > floor

    val size: Int get() = entries.size

    val keys: List<AppNavKey> get() = entries.toList()

    fun contains(key: AppNavKey): Boolean = entries.contains(key)

    /** Applies one command, without regard to motion: the host decides when. */
    fun apply(command: NavCommand) {
        when (command) {
            is NavCommand.Navigate -> {
                // A pop inside a navigate may empty the stack, since the push refills it (the
                // sign-in case).
                command.popUpTo?.let { popTo(it, command.inclusive, allowEmpty = true) }
                if (command.singleTop && entries.lastOrNull() == command.key) return
                entries.add(command.key)
            }

            NavCommand.Up -> pop(1)

            is NavCommand.ResetTo -> {
                entries.clear()
                entries.add(command.key)
            }

            is NavCommand.PopTo -> popTo(command.key, command.inclusive)
        }
    }

    /** Pops up to [count] entries, never below the floor, and returns how many it popped. */
    internal fun pop(count: Int): Int {
        val popped = count.coerceAtMost(entries.size - floor).coerceAtLeast(0)
        if (popped > 0) entries.removeRange(entries.size - popped, entries.size)
        return popped
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
        val floorCount = if (allowEmpty) keepCount else keepCount.coerceAtLeast(floor)
        if (entries.size > floorCount) entries.removeRange(floorCount, entries.size)
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
): Saver<AppBackStack, String> = Saver(
    save = { stack -> serialization.encodeStack(stack.keys) },
    restore = { encoded ->
        val keys = serialization.decodeStack(encoded)?.takeIf { it.isNotEmpty() } ?: listOf(startKey)
        AppBackStack(mutableStateListOf<AppNavKey>().apply { addAll(keys) })
    },
)

private val stackSerializer = ListSerializer(PolymorphicSerializer(AppNavKey::class))

/*
 * A key missing from its feature's navKeys block cannot be encoded, and the whole stack is lost with
 * it; logged, so the gap shows up in development rather than as an app that forgets where it was.
 */

internal fun NavKeySerialization.encodeStack(keys: List<AppNavKey>): String? =
    runCatching { json.encodeToString(stackSerializer, keys) }
        .onFailure { Log.e("Back stack not saved", it, NAV_LOG_TAG) }
        .getOrNull()

internal fun NavKeySerialization.decodeStack(encoded: String): List<AppNavKey>? =
    runCatching { json.decodeFromString(stackSerializer, encoded) }
        .onFailure { Log.e("Back stack not restored", it, NAV_LOG_TAG) }
        .getOrNull()

internal const val NAV_LOG_TAG = "Navigation"
