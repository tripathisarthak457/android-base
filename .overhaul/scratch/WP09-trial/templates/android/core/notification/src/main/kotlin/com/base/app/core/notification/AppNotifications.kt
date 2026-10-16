package com.base.app.core.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.base.app.core.common.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The notification channels this app posts to. */
enum class NotificationChannelSpec(
    val id: String,
    @param:StringRes val channelName: Int,
    @param:StringRes val description: Int,
    val importance: Int,
) {
    Default(
        id = "base_app_default",
        channelName = R.string.notification_channel_default,
        description = R.string.notification_channel_default_description,
        importance = NotificationManager.IMPORTANCE_DEFAULT,
    ),
    Urgent(
        id = "base_app_urgent",
        channelName = R.string.notification_channel_urgent,
        description = R.string.notification_channel_urgent_description,
        importance = NotificationManager.IMPORTANCE_HIGH,
    ),
    Promotions(
        id = "base_app_promotions",
        channelName = R.string.notification_channel_promotions,
        description = R.string.notification_channel_promotions_description,
        importance = NotificationManager.IMPORTANCE_LOW,
    ),
    Silent(
        id = "base_app_silent",
        channelName = R.string.notification_channel_silent,
        description = R.string.notification_channel_silent_description,
        importance = NotificationManager.IMPORTANCE_MIN,
    ),
}

/** Creating channels and posting notifications. */
@Singleton
class AppNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val systemManager = context.getSystemService<NotificationManager>() ?: return
        NotificationChannelSpec.entries.forEach { spec ->
            // Created again on every launch, so a language change renames the channels too.
            val channel = NotificationChannel(spec.id, context.getString(spec.channelName), spec.importance).apply {
                description = context.getString(spec.description)
            }
            systemManager.createNotificationChannel(channel)
        }
    }

    val hasPermission: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Posts a notification. [contentIntent] is what happens on tap. */
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

        // The `hasPermission` guard above is the check, but it is a property rather than an inline
        // `checkSelfPermission`, so lint cannot follow it.
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
 * Without this, every notification on an old device arrives at default priority regardless of which
 * channel it nominally belongs to.
 */
private fun Int.toCompatPriority(): Int = when (this) {
    NotificationManager.IMPORTANCE_HIGH -> NotificationCompat.PRIORITY_HIGH
    NotificationManager.IMPORTANCE_LOW -> NotificationCompat.PRIORITY_LOW
    NotificationManager.IMPORTANCE_MIN -> NotificationCompat.PRIORITY_MIN
    else -> NotificationCompat.PRIORITY_DEFAULT
}
