package com.base.app.push

import com.base.app.R
import com.base.app.core.common.util.AppLogger
import com.base.app.core.coroutines.ApplicationScope
import com.base.app.core.notification.AppNotifications
import com.base.app.core.notification.NotificationChannelSpec
import com.base.app.core.notification.PushTokenRegistrar
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * Receives pushes.
 *
 * ## Only data messages reach here while backgrounded
 *
 * A push containing a `notification` block is posted by the FCM SDK itself when the app is not in
 * the foreground — this service is never called, which is why the manifest also declares a
 * default channel and icon. A push containing only `data` always arrives here, in both states.
 * If you need consistent behaviour, send data-only messages and post the notification yourself.
 *
 * ## Work is launched in the application scope
 *
 * A `FirebaseMessagingService` is torn down as soon as `onMessageReceived` returns, so a
 * coroutine tied to the service would be cancelled before it finished. The token upload in
 * particular has to outlive the callback.
 */
@AndroidEntryPoint
class AppMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notifications: AppNotifications

    @Inject
    lateinit var tokenRegistrar: PushTokenRegistrar

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { tokenRegistrar.onNewToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // The notification block is preferred when present, with the data map as the fallback,
        // because a message can legitimately carry either — and a service that reads only one
        // shape drops half the pushes a backend sends.
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()

        val channel = when (message.data["channel"]) {
            "urgent" -> NotificationChannelSpec.Urgent
            "promotions" -> NotificationChannelSpec.Promotions
            "silent" -> NotificationChannelSpec.Silent
            else -> NotificationChannelSpec.Default
        }

        AppLogger.d("Push received on ${channel.id}", tag = "Push")

        notifications.post(
            // A random id so two pushes do not replace each other. Use a stable id derived from
            // the entity instead when an update genuinely should replace its predecessor — an
            // order status, say, where two notifications would be noise.
            id = message.data["notificationId"]?.toIntOrNull() ?: Random.nextInt(),
            title = title,
            body = body,
            channel = channel,
            smallIconRes = R.drawable.ic_notification,
            // <opt:deeplink>
            contentIntent = message.data["link"]?.let(::deepLinkIntent),
            // </opt:deeplink>
        )
    }

    // <opt:deeplink>
    private fun deepLinkIntent(link: String) =
        android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(link),
        ).setPackage(packageName)
    // </opt:deeplink>
}
