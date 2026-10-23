package com.example.videoalone.perf

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import com.example.videoalone.core.common.navigation.AppNavKey
import com.example.videoalone.core.navigation.ShellState
import com.example.videoalone.startup.ShellObserver
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/**
 * One line per change of the shell's state, which tools/perf reads back: `nav_stress` checks the
 * stack invariants on every line, and `restore` compares the state before and after process death.
 *
 *     perf.shell: generation=1 tab=0 stacks=[["HomeKey","DetailKey(id=3)"],["FeedKey"]] presented=[] roots=["HomeKey","FeedKey"] more=false chrome=Bar window=411x891dp class=CompactxExpanded
 *
 * `more` is whether the More list is in front of the tab, `chrome` the bar, rail or sidebar the
 * window shows, and `window` and `class` the window it was chosen for, which tools/adaptive checks
 * across resizes.
 */
class ShellLog @Inject constructor() : ShellObserver {

    override suspend fun observe(state: ShellState) {
        snapshotFlow { describe(state) }.collect { Log.i(TAG, it) }
    }

    private fun describe(state: ShellState): String =
        "generation=${state.generation} tab=${state.selectedIndex} " +
            "stacks=${JsonArray(state.stacks.map(::keys))} presented=${keys(state.presentedKeys)} roots=${keys(state.roots)} " +
            "more=${state.moreShown} chrome=${state.chrome}" +
            state.windowMetrics?.sizeDp?.let { " window=${it.width.value.roundToInt()}x${it.height.value.roundToInt()}dp" }.orEmpty() +
            state.window?.let { " class=${it.width}x${it.height}" }.orEmpty()

    private fun keys(stack: List<AppNavKey>): JsonArray = JsonArray(stack.map { JsonPrimitive(it.toString()) })

    private companion object {
        const val TAG = "perf.shell"
    }
}
