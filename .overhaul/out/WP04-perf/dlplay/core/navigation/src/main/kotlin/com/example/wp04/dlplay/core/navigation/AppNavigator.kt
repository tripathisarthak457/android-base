package com.example.wp04.dlplay.core.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.wp04.dlplay.core.common.navigation.AppNavKey
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

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

    /** Drops the whole stack and starts again at [key]. Sign-out, and session expiry. */
    data class ResetTo(val key: AppNavKey) : NavCommand

    /** Pops back to [key] if it is on the stack; does nothing if it is not. */
    data class PopTo(val key: AppNavKey, val inclusive: Boolean = false) : NavCommand
}

/**
 * The seam between "a ViewModel decided to navigate" and "the back stack changed". One per window
 * (activity), kept across its configuration changes: two windows of the app, side by side or in
 * desktop mode, each navigate on their own.
 */
@ActivityRetainedScoped
class AppNavigator @Inject constructor() {

    private val _commands = Channel<NavCommand>(Channel.BUFFERED)
    val commands: Flow<NavCommand> = _commands.receiveAsFlow()

    /*
     * Not suspending, so a click handler can call these directly. The channel buffers, and the
     * shell applies commands one at a time, after any transition that is running has settled.
     */

    fun navigate(
        key: AppNavKey,
        popUpTo: AppNavKey? = null,
        inclusive: Boolean = false,
        singleTop: Boolean = true,
    ) {
        _commands.trySend(NavCommand.Navigate(key, popUpTo, inclusive, singleTop))
    }

    fun navigateUp() {
        _commands.trySend(NavCommand.Up)
    }

    fun resetTo(key: AppNavKey) {
        _commands.trySend(NavCommand.ResetTo(key))
    }

    fun popTo(key: AppNavKey, inclusive: Boolean = false) {
        _commands.trySend(NavCommand.PopTo(key, inclusive))
    }
}

/**
 * Hands the navigator's commands to [apply] while [lifecycle] is started, and keeps them waiting
 * otherwise, so a command sent while the window is in the background is applied when it returns
 * instead of moving a stack nobody sees.
 */
suspend fun AppNavigator.applyWhileStarted(lifecycle: Lifecycle, apply: suspend (NavCommand) -> Unit) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        commands.collect { apply(it) }
    }
}
