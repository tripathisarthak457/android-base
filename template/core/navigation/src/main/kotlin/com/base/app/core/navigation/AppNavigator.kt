package com.base.app.core.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What a ViewModel can ask navigation to do.
 *
 * A command rather than a direct call on a back stack, because the back stack is Compose state
 * owned by the host and a ViewModel must not touch it: doing so ties the ViewModel's lifetime to
 * a composition, and makes it untestable without one.
 */
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
 * The seam between "a ViewModel decided to navigate" and "the back stack changed".
 *
 * A singleton, so any ViewModel can inject it without the screen above it having to thread a
 * callback down. The host collects [commands] and applies them to the stack it owns.
 *
 * The channel is buffered rather than conflated: two navigations issued in quick succession — a
 * pop followed by a push, say — must both arrive, in order. A conflated channel would drop the
 * first, and the user would land somewhere unexpected.
 */
@Singleton
class AppNavigator @Inject constructor() {

    private val _commands = Channel<NavCommand>(Channel.BUFFERED)
    val commands: Flow<NavCommand> = _commands.receiveAsFlow()

    /**
     * Not `suspend`, unlike the obvious design.
     *
     * A suspending navigate has to be called from a coroutine, which means every event handler
     * that navigates needs a scope, and a handler that is cancelled part-way — because the
     * screen it belongs to is leaving, which is exactly when navigation happens — silently drops
     * the navigation. `trySend` on a buffered channel cannot fail in practice and cannot be
     * cancelled.
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
