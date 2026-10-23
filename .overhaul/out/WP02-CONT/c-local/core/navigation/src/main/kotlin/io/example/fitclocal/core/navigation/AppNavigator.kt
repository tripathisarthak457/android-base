package io.example.fitclocal.core.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.example.fitclocal.core.common.navigation.AppNavKey
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job

/** What a ViewModel can ask navigation to do. */
sealed interface NavCommand {

    /**
     * @param popUpTo if set, entries above and — when [inclusive] — including the first match from
     *   the top are removed before [key] is pushed. This is how a login screen is replaced by home
     *   without leaving it behind the back gesture.
     * @param singleTop don't push a second copy of the destination already on top. Without it, a
     *   double-tapped list row pushes the detail screen twice and the first Back appears to do
     *   nothing.
     */
    data class Navigate(
        val key: AppNavKey,
        val popUpTo: AppNavKey? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = true,
    ) : NavCommand

    data object Up : NavCommand

    /**
     * Drops the whole stack and starts again at [key]. In the shell, like every command not addressed
     * to a tab, it acts on the presentation stack while anything is presented there.
     */
    data class ResetTo(val key: AppNavKey) : NavCommand

    /** Pops back to [key] if it is on the stack; does nothing if it is not. */
    data class PopTo(val key: AppNavKey, val inclusive: Boolean = false) : NavCommand
}

/**
 * The seam between "a ViewModel decided to navigate" and "the back stack changed". One per window
 * (activity), kept across its configuration changes: two windows of the app, side by side or in
 * desktop mode, each navigate on their own.
 *
 * Its methods do not suspend, so a click handler can call them directly. Commands queue, and the
 * host applies them one at a time, after any transition that is running has settled.
 */
@ActivityRetainedScoped
class AppNavigator @Inject constructor() {

    // A queue rather than a channel: a command a host has taken but not yet applied can be put
    // back, and a host that is cancelled while it waits for a command takes none.
    private val pending = MutableStateFlow<List<NavCommand>>(emptyList())

    fun navigate(
        key: AppNavKey,
        popUpTo: AppNavKey? = null,
        inclusive: Boolean = false,
        singleTop: Boolean = true,
    ) {
        send(NavCommand.Navigate(key, popUpTo, inclusive, singleTop))
    }

    fun navigateUp() {
        send(NavCommand.Up)
    }

    fun resetTo(key: AppNavKey) {
        send(NavCommand.ResetTo(key))
    }

    fun popTo(key: AppNavKey, inclusive: Boolean = false) {
        send(NavCommand.PopTo(key, inclusive))
    }

    private fun send(command: NavCommand) {
        pending.update { it + command }
    }

    /**
     * Hands each command to [apply] in the order sent, each to exactly one caller, until cancelled.
     * A command [apply] had not finished when it was cancelled goes back to the front of the queue
     * if [putBack] says so.
     */
    internal suspend fun applyEach(putBack: () -> Boolean, apply: suspend (NavCommand) -> Unit) {
        while (true) {
            pending.first { it.isNotEmpty() }
            val command = pending.getAndUpdate { it.drop(1) }.firstOrNull() ?: continue
            var applied = false
            try {
                apply(command)
                applied = true
            } finally {
                if (!applied && putBack()) pending.update { listOf(command) + it }
            }
        }
    }
}

/**
 * Hands the navigator's commands to [apply] while [lifecycle] is started, and keeps them waiting
 * otherwise, so a command sent while the window is in the background is applied when it returns
 * instead of moving a stack nobody sees.
 *
 * A command [apply] had not finished when the window stopped is applied when it returns. One it had
 * not finished when its caller was cancelled (the host left composition) was meant for that host,
 * and is dropped with it rather than applied to the host that replaces it.
 */
suspend fun AppNavigator.applyWhileStarted(lifecycle: Lifecycle, apply: suspend (NavCommand) -> Unit) {
    val host = currentCoroutineContext().job
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        applyEach(putBack = { host.isActive }, apply = apply)
    }
}
