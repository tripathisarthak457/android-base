package com.base.app.perf

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import com.base.app.core.navigation.AppNavKey
import com.base.app.core.navigation.ShellState
import com.base.app.ui.ShellObserver
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

/**
 * One line per change of the shell's state, which tools/perf reads back: `nav_stress` checks the
 * stack invariants on every line, and `restore` compares the state before and after process death.
 *
 *     perf.shell: generation=1 tab=0 stacks=[["HomeKey","DetailKey(id=3)"],["FeedKey"]] presented=[] roots=["HomeKey","FeedKey"]
 */
class ShellLog @Inject constructor() : ShellObserver {

    override suspend fun observe(state: ShellState) {
        snapshotFlow { describe(state) }.collect { Log.i(TAG, it) }
    }

    private fun describe(state: ShellState): String =
        "generation=${state.generation} tab=${state.selectedIndex} " +
            "stacks=${JsonArray(state.stacks.map(::keys))} presented=${keys(state.presentedKeys)} roots=${keys(state.roots)}"

    private fun keys(stack: List<AppNavKey>): JsonArray = JsonArray(stack.map { JsonPrimitive(it.toString()) })

    private companion object {
        const val TAG = "perf.shell"
    }
}
