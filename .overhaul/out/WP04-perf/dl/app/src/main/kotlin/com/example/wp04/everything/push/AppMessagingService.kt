package com.example.wp04.everything.push

import com.example.wp04.everything.R
import com.example.wp04.everything.core.common.coroutines.ApplicationScope
import com.example.wp04.everything.core.common.log.Log
import com.example.wp04.everything.core.notification.AppNotifications
import com.example.wp04.everything.core.notification.NotificationChannelSpec
import com.example.wp04.everything.core.notification.PushTokenRegistrar
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/** Receives pushes. */
@AndroidEntryPoint
class AppMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notifications: AppNotifications

    @Inject
    lateinit var tokenRegistrar: PushTokenRegistrar

    @Inject
    lateinit var scope: ApplicationScope

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        scope.launch { tokenRegistrar.onRegistered(installationId) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Read the notification block first and the data map as a fallback; backends send either.
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()

        val channel = when (message.data["channel"]) {
            "urgent" -> NotificationChannelSpec.Urgent
            "promotions" -> NotificationChannelSpec.Promotions
            "silent" -> NotificationChannelSpec.Silent
            else -> NotificationChannelSpec.Default
        }

        Log.d("Push received on ${channel.id}", tag = "Push")

        notifications.post(
            // A random id so pushes do not replace each other. Use a stable id when one should
            // replace the last.
            id = message.data["notificationId"]?.toIntOrNull() ?: Random.nextInt(),
            title = title,
            body = body,
            channel = channel,
            smallIconRes = R.drawable.ic_notification,
            contentIntent = message.data["link"]?.let(::deepLinkIntent),
        )
    }

    private fun deepLinkIntent(link: String) =
        android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(link),
        ).setPackage(packageName)
}
