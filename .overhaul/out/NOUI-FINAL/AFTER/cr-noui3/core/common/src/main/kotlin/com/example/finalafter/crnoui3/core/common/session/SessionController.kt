package com.example.finalafter.crnoui3.core.common.session

import kotlinx.coroutines.flow.StateFlow

/** The session, as seen by a feature: whether someone is signed in, and a way to end it. */
interface SessionController {

    val state: StateFlow<SessionState>

    fun signOut()
}

sealed interface SessionState {

    data object SignedOut : SessionState

    /**
     * [generation] changes with every new session, so anything keyed on it (the shell's kept tabs
     * and stacks) starts empty for the next person instead of showing the last one's screens.
     */
    data class SignedIn(val generation: Int) : SessionState
}
