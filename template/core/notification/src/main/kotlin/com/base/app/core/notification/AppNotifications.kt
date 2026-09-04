package com.base.app.core.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.base.app.core.common.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The notification channels this app posts to.
 *
 * Declared as an enum rather than created ad hoc, because a channel's importance, sound and
 * vibration are fixed at creation and **cannot be changed afterwards** — Android ignores every
 * later attempt, by design, so the user stays in control. Getting a channel wrong therefore means
 * shipping a new channel id and leaving the old one orphaned in the user's settings. Having them
 * all in one place makes that decision visible when it is made.
 *
 * The split matters to users: someone who wants order updates but not marketing has to be able to
 * turn one off without the other, and that is only possible if they are separate channels.
 */
enum class NotificationChannelSpec(
    val id: String,
    val channelName: String,
    val description: String,
    val importance: Int,
) {
    Default(
        id = "base_app_default",
        channelName = "General",
        description = "Account activity and important updates.",
        importance = NotificationManager.IMPORTANCE_DEFAULT,
    ),
    Urgent(
        id = "base_app_urgent",
        channelName = "Time-sensitive",
        description = "Things that need your attention right now.",
        importance = NotificationManager.IMPORTANCE_HIGH,
    ),
    Promotions(
        id = "base_app_promotions",
        channelName = "Offers",
        description = "Deals and product news.",
        importance = NotificationManager.IMPORTANCE_LOW,
    ),
    Silent(
        id = "base_app_silent",
        channelName = "Background",
        description = "Sync and progress updates.",
        importance = NotificationManager.IMPORTANCE_MIN,
    ),
}

/**
 * Creating channels and posting notifications.
 *
 * ## Channels are created at startup, not at first use
 *
 * They have to exist before the user can find them in system settings, and a user who wants to
 * mute one category should not have to receive a notification from it first in order to be able
 * to. `createNotificationChannels` is called from `Application.onCreate`.
 *
 * ## Posting checks the permission
 *
 * On API 33+ `POST_NOTIFICATIONS` is a runtime permission, and posting without it throws no
 * exception and shows nothing. Checking here means the failure is logged rather than silent,
 * which is the difference between "notifications are broken" being a five-minute answer and a
 * two-day investigation.
 */
@Singleton
class AppNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val systemManager = context.getSystemService<NotificationManager>() ?: return
        NotificationChannelSpec.entries.forEach { spec ->
            val channel = NotificationChannel(spec.id, spec.channelName, spec.importance).apply {
                description = spec.description
            }
            systemManager.createNotificationChannel(channel)
        }
    }

    val hasPermission: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Posts a notification.
     *
     * [contentIntent] is what happens on tap. It is a plain [Intent] rather than a
     * `PendingIntent`, so the caller does not have to remember `FLAG_IMMUTABLE` — which is
     * mandatory from API 31 and throws at runtime if omitted.
     */
    fun post(
        id: Int,
        title: String,
        body: String,
        channel: NotificationChannelSpec = NotificationChannelSpec.Default,
        smallIconRes: Int,
        contentIntent: Intent? = null,
        autoCancel: Boolean = true,
    ) {
        if (!hasPermission) {
            AppLogger.w("Notification suppressed: POST_NOTIFICATIONS not granted.", tag = TAG)
            return
        }

        val builder = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(body)
            // Without this a body longer than one line is truncated with no way to read the rest;
            // BigTextStyle is what makes it expandable.
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(autoCancel)
            .setPriority(channel.importance.toCompatPriority())

        contentIntent?.let { intent ->
            builder.setContentIntent(
                android.app.PendingIntent.getActivity(
                    context,
                    id,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                        android.app.PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }

        // The `hasPermission` guard above is the check, but it is a property rather than an
        // inline `checkSelfPermission`, so lint cannot follow it. The runCatching is the second
        // half of the answer: an OEM that revokes the grant between the check and the post
        // throws, and a crash there would be caused by the user tapping "don't allow".
        @SuppressLint("MissingPermission")
        val posted = runCatching { manager.notify(id, builder.build()) }
        posted.onFailure { AppLogger.e("Failed to post notification $id", it, TAG) }
    }

    fun cancel(id: Int) = manager.cancel(id)

    fun cancelAll() = manager.cancelAll()

    private companion object {
        const val TAG = "Notifications"
    }
}

/**
 * Pre-O devices have no channels; the importance has to be carried on the notification itself.
 * Without this, every notification on an old device arrives at default priority regardless of
 * which channel it nominally belongs to.
 */
private fun Int.toCompatPriority(): Int = when (this) {
    NotificationManager.IMPORTANCE_HIGH -> NotificationCompat.PRIORITY_HIGH
    NotificationManager.IMPORTANCE_LOW -> NotificationCompat.PRIORITY_LOW
    NotificationManager.IMPORTANCE_MIN -> NotificationCompat.PRIORITY_MIN
    else -> NotificationCompat.PRIORITY_DEFAULT
}
