package com.base.app.core.notification

import com.base.app.core.common.util.AppLogger
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Where this install's push address goes. */
fun interface PushTokenSink {
    suspend fun submit(installationId: String)
}

/** Registers this install with FCM and hands its installation ID to the [PushTokenSink]. */
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
