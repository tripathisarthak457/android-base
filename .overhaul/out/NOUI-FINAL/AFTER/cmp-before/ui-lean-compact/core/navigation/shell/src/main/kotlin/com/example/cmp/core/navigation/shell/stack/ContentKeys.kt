package com.example.cmp.core.navigation.shell.stack

import androidx.navigation3.runtime.NavEntry

/**
 * The entries of a back stack, each with a content key of its own.
 *
 * Navigation 3 treats entries that share a content key as one screen: one saved state, one
 * ViewModel store, and composing two of them at once fails. A stack can hold one key twice (a
 * profile opened from the profile it led to, a row selected again beside its list), and each is a
 * screen of its own, so a later entry of a key is told apart by how many entries of that key are
 * below it. An entry's count never changes while it is in the stack, since only what is above it
 * is pushed or popped.
 */
internal fun <K : Any> List<K>.toNavEntries(provide: (key: K, contentKey: String) -> NavEntry<K>): List<NavEntry<K>> {
    val below = HashMap<K, Int>()
    return map { key ->
        val count = below.getOrElse(key) { 0 }
        below[key] = count + 1
        provide(key, contentKey(key, count))
    }
}

/** The content key [toNavEntries] gives the last of these entries, or null for none. */
internal fun List<Any>.lastContentKey(): String? {
    val key = lastOrNull() ?: return null
    return contentKey(key, count { it == key } - 1)
}

// The class is part of it, as in Navigation 3's default: keys of two classes can print alike.
private fun contentKey(key: Any, below: Int): String {
    val first = "$key:${key::class}"
    return if (below == 0) first else "$first#$below"
}
