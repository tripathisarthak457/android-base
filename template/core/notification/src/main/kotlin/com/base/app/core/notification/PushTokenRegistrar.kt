package com.base.app.core.notification

import com.base.app.core.common.util.AppLogger
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where this install's push address goes.
 *
 * An interface implemented by the app module rather than a call into a repository from here,
 * because `:core:notification` sits below every `:data:*` module and must not reach up into one.
 * The app binds an implementation that posts the value to whatever endpoint the backend uses.
 *
 * The value is the Firebase Installation ID. FCM now addresses an app instance by its FID —
 * `Message.fid` on the server — and the registration token it replaces is deprecated.
 */
fun interface PushTokenSink {
    suspend fun submit(installationId: String)
}

/**
 * Registers this install with FCM and hands its installation ID to the [PushTokenSink].
 *
 * ## It runs on every launch, not only when the ID changes
 *
 * `onRegistered` is how FCM reports a new ID, and nothing guarantees it fires again for an ID it
 * has already reported. If the upload after that first report failed — offline, a 500 — the
 * device would silently never receive a push. So each launch registers, reads the ID directly and
 * submits it again: a cheap idempotent write that closes the hole.
 */
@Singleton
class PushTokenRegistrar @Inject constructor(
    private val sink: PushTokenSink,
) {

    suspend fun registerOnLaunch() {
        runCatching {
            FirebaseMessaging.getInstance().register().await()
            FirebaseInstallations.getInstance().id.await()
        }.onSuccess { submit(it) }
            .onFailure { AppLogger.w("FCM registration unavailable", throwable = it, tag = TAG) }
    }

    suspend fun onRegistered(installationId: String) = submit(installationId)

    private suspend fun submit(installationId: String) {
        runCatching { sink.submit(installationId) }
            .onFailure { AppLogger.w("Could not submit the push installation ID", throwable = it, tag = TAG) }
    }

    private companion object {
        const val TAG = "Push"
    }
}
