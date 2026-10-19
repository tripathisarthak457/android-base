package com.base.app.core.analytics.jank

import androidx.metrics.performance.StateInfo

/**
 * The analytics key of a frame's states, built again only when they change. The states of
 * consecutive frames are the same [StateInfo] instances until something calls `putState`, so a
 * screen that keeps a state for its whole life costs one string, not one per frame.
 *
 * Called on JankStats' thread only.
 */
internal class StateKeys {
    private val last = arrayOfNulls<StateInfo>(MAX_REMEMBERED)
    private var lastCount = -1
    private var lastKey = IDLE_STATE

    fun of(states: List<StateInfo>): String {
        if (states.isEmpty()) return IDLE_STATE
        if (states.size == lastCount && sameAsLast(states)) return lastKey

        lastKey = states.joinToString(separator = ",") { it.key + "=" + it.value }
            .take(MAX_STATE_LENGTH)
        // More states than are remembered never match, so their key is built every frame: correct,
        // and no screen sets that many.
        lastCount = if (states.size <= MAX_REMEMBERED) states.size else -1
        for (index in 0 until minOf(states.size, MAX_REMEMBERED)) last[index] = states[index]
        return lastKey
    }

    private fun sameAsLast(states: List<StateInfo>): Boolean {
        for (index in states.indices) {
            if (states[index] !== last[index]) return false
        }
        return true
    }

    companion object {
        /** The key of a frame drawn without states. */
        const val IDLE_STATE = "idle"

        /** Analytics vendors cap a parameter value at about a hundred characters. */
        const val MAX_STATE_LENGTH = 100

        private const val MAX_REMEMBERED = 8
    }
}
