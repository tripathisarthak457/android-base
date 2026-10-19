package com.base.app.core.perfhud.frame

/**
 * Screen names to small indexes into [FrameLog]'s per-screen counts. Index [UNKNOWN] holds
 * frames drawn before any screen was reported, and the last index every screen past the first
 * [CAPACITY] - 2, which an app with that many destinations shares as "other".
 *
 * A lookup compares the name with the previous frame's first, by identity: a screen's frames all
 * carry the same string instance until the next navigation, so the common case is one comparison.
 * Not thread-safe: [FrameLog] guards it.
 */
internal class ScreenTable {

    private val names = arrayOfNulls<String>(CAPACITY)
    private var lastName: String? = null
    private var lastIndex = UNKNOWN

    /** How many indexes are in use, [UNKNOWN] included. */
    var size: Int = 1
        private set

    fun indexOf(name: String?): Int {
        if (name == null) return UNKNOWN
        if (name === lastName) return lastIndex
        val found = find(name)
        val index = if (found == NOT_FOUND) add(name) else found
        lastName = name
        lastIndex = index
        return index
    }

    /** The name at [index]; null for [UNKNOWN] and [OTHER]. */
    fun nameAt(index: Int): String? = names[index]

    fun clear() {
        names.fill(null)
        lastName = null
        lastIndex = UNKNOWN
        size = 1
    }

    private fun find(name: String): Int {
        for (index in 1 until size) {
            if (names[index] == name) return index
        }
        return NOT_FOUND
    }

    private fun add(name: String): Int {
        if (size >= OTHER) {
            size = CAPACITY
            return OTHER
        }
        names[size] = name
        return size++
    }

    companion object {
        const val CAPACITY = 32
        const val UNKNOWN = 0
        const val OTHER = CAPACITY - 1
        private const val NOT_FOUND = -1
    }
}
