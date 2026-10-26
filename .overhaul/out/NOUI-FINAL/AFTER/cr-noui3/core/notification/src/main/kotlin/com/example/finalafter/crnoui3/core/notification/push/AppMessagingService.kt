package com.example.finalafter.crnoui3.core.notification.push

import android.net.Uri
import com.example.finalafter.crnoui3.core.common.coroutines.ApplicationScope
import com.example.finalafter.crnoui3.core.notification.AppNotifications
import com.example.finalafter.crnoui3.core.notification.channel.NotificationChannelSpec
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.launch

/** Receives pushes while the app runs; FCM shows a `notification` payload by itself when it does not. */
@AndroidEntryPoint
internal class AppMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notifications: AppNotifications

    @Inject
    lateinit var registration: PushRegistration

    @Inject
    lateinit var scope: ApplicationScope

    override fun onRegistered(installationId: String) {
        scope.launch { registration.onRegistered(installationId) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // The notification block first and the data map as a fallback: backends send either.
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val channel = when (message.data["channel"]) {
            "urgent" -> NotificationChannelSpec.Urgent
            "promotions" -> NotificationChannelSpec.Promotions
            "silent" -> NotificationChannelSpec.Silent
            else -> NotificationChannelSpec.Default
        }
        notifications.post(
            // Random unless the backend names one, so pushes do not replace each other.
            id = message.data["notification_id"]?.toIntOrNull() ?: Random.nextInt(),
            title = title,
            body = body,
            channel = channel,
            link = message.data["link"]?.let(Uri::parse),
        )
    }
}
