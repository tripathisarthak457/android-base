package com.base.app.core.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue

/**
 * Back presses that arrive while a host's screens are moving.
 *
 * Applied at once, a second press shrinks the stack again in the middle of a pop: the transition
 * retargets to a screen that is not composed yet, the screen that was leaving snaps, and a burst of
 * presses can walk straight out of the app. Replayed one by one, three quick presses become three
 * animations in a row. So presses are counted while the screens move and, once the running
 * transition has all but arrived ([SETTLED_ENOUGH]), popped together: Navigation 3 animates that
 * as one pop from the screen on display to the final one. Presses that would go past the root are
 * dropped, so a burst never leaves the app by accident.
 *
 * One gate per host (each kept tab, the presentation host, a single-stack host). It holds no
 * reference to Compose UI, so its rules are tested without a screen.
 */
@Stable
class BackGate internal constructor(private val maxCoalesced: Int = MAX_COALESCED) {

    private val reporters = mutableStateSetOf<Any>()

    /*
     * Between a stack change and the first frame of its transition nothing is running yet, but a
     * press in that gap must still be counted, not applied: it would retarget the transition before
     * it starts.
     */
    private var awaitingMotion by mutableStateOf(false)

    /** Changes with every stack change the host makes, so the host can watch for its motion. */
    internal var changes by mutableIntStateOf(0)
        private set

    /** True from a stack change until its transition has all but arrived ([SETTLED_ENOUGH]). */
    val moving: Boolean
        get() = awaitingMotion || reporters.isNotEmpty()

    internal var counted = 0
        private set

    internal var lastChange: StackChange? by mutableStateOf(null)
        private set

    /** A Back press: pops one entry now, or counts it while the screens move. */
    internal fun onBack(pop: (Int) -> Int) {
        if (moving) {
            counted = (counted + 1).coerceAtMost(maxCoalesced)
        } else if (pop(1) > 0) {
            changed(StackChange.Pop)
        }
    }

    /** The host changed its stack; the transition for it starts on the next frame, if at all. */
    internal fun changed(change: StackChange) {
        lastChange = change
        awaitingMotion = true
        changes++
    }

    /** A screen of this host reports whether its transition is still short of settled. */
    internal fun report(reporter: Any, isMoving: Boolean) {
        if (isMoving) {
            reporters += reporter
            awaitingMotion = false
        } else {
            reporters -= reporter
        }
    }

    /** No transition followed the last change: reduce motion, or a change nothing animates. */
    internal fun noMotion() {
        awaitingMotion = false
    }

    /** The screens have stopped: the presses counted meanwhile become one pop. */
    internal fun settle(pop: (Int) -> Int) {
        if (counted == 0) return
        val presses = counted
        counted = 0
        if (pop(presses) > 0) changed(StackChange.Pop)
    }

    internal companion object {
        /**
         * The share of its travel after which a transition no longer holds Back: the arriving
         * screen is within a few pixels of its place, about 70 % of the way through the settle.
         */
        const val SETTLED_ENOUGH = 0.99f
        const val MAX_COALESCED = 8
    }
}

internal enum class StackChange { Push, Pop, Replace }
