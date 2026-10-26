package com.example.cmp.core.network.websocket

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.cmp.core.common.coroutines.ApplicationScope
import com.example.cmp.core.datastore.session.AuthTokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps [AppWebSocket] open only while the app is in front. Leaving closes it after a grace period,
 * so a quick trip to another app does not cost a reconnect, and a socket held open in the
 * background does not drain the battery. A new account reopens it with that account's token.
 */
@Singleton
class RealtimeLifecycle @Inject constructor(
    private val socket: AppWebSocket,
    private val tokenStore: AuthTokenStore,
    private val scope: ApplicationScope,
) {
    private var closing: Job? = null

    /** Called once, from `Application.onCreate`. */
    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    closing?.cancel()
                    socket.resume()
                }

                override fun onStop(owner: LifecycleOwner) {
                    closing = scope.launch {
                        delay(BACKGROUND_GRACE)
                        socket.pause()
                    }
                }
            },
        )
        scope.launch {
            tokenStore.tokens.map {
                it?.owner
            }.distinctUntilChanged().drop(1).filterNotNull().collect {
                socket.reopen()
            }
        }
    }

    private companion object {
        val BACKGROUND_GRACE = 30.seconds
    }
}
