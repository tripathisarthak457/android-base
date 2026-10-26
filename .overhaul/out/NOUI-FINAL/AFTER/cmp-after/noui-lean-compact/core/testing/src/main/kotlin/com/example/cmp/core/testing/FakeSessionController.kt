package com.example.cmp.core.testing

import com.example.cmp.core.common.session.SessionController
import com.example.cmp.core.common.session.SessionState
import kotlinx.coroutines.flow.MutableStateFlow

/** A [SessionController] a test drives directly; it counts the sign-outs asked of it. */
class FakeSessionController(initial: SessionState = SessionState.SignedIn(generation = 1)) :
    SessionController {

    override val state = MutableStateFlow(initial)

    var signOuts = 0
        private set

    override fun signOut() {
        signOuts++
        state.value = SessionState.SignedOut
    }
}
