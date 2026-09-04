package com.base.app.core.notification

import com.base.app.core.common.util.AppLogger
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Where a new push token goes.
 *
 * An interface implemented by the app module rather than a call into a repository from here,
 * because `:core:notification` sits below every `:data:*` module and must not reach up into one.
 * The app binds an implementation that posts the token to whatever endpoint the backend uses.
 */
fun interface PushTokenSink {
    suspend fun submit(token: String)
}

/**
 * Fetches the FCM token and hands it to the [PushTokenSink].
 *
 * ## The token is re-sent on every launch, not only when it changes
 *
 * `onNewToken` fires when FCM rotates a token, and that is the only time it fires. If the call
 * that uploaded it failed — the user was offline, the server was down, the request 500'd — that
 * device silently stops receiving pushes forever, and nothing surfaces it. Re-submitting at
 * startup is a cheap idempotent write that closes the hole.
 */
@Singleton
class PushTokenRegistrar @Inject constructor(
    private val sink: PushTokenSink,
) {

    suspend fun refresh() {
        val token = currentToken() ?: return
        runCatching { sink.submit(token) }
            .onFailure { AppLogger.w("Could not submit push token", throwable = it, tag = TAG) }
    }

    suspend fun onNewToken(token: String) {
        runCatching { sink.submit(token) }
            .onFailure { AppLogger.w("Could not submit rotated push token", throwable = it, tag = TAG) }
    }

    /**
     * Bridges FCM's callback API to a coroutine.
     *
     * `suspendCancellableCoroutine` rather than `await()` so that a cancelled scope — the user
     * closing the app during startup — does not leave the continuation dangling.
     */
    private suspend fun currentToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    AppLogger.w("FCM token unavailable", throwable = task.exception, tag = TAG)
                    continuation.resume(null)
                }
            }
    }

    private companion object {
        const val TAG = "Push"
    }
}
