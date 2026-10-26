package com.example.finalafter.crui.core.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.finalafter.crui.core.common.log.Log
import com.example.finalafter.crui.core.notification.channel.NotificationChannelSpec
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Posts the app's notifications. */
@Singleton
class AppNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    val hasPermission: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Posts a notification. A tap opens [link] in this app when there is one, and the app as it was
     * left otherwise. On the lock screen it shows only the app's name and a neutral line, since a
     * message's text is nobody else's business.
     */
    fun post(
        id: Int,
        title: String,
        body: String,
        channel: NotificationChannelSpec = NotificationChannelSpec.Default,
        link: Uri? = null,
    ) {
        if (!hasPermission) {
            Log.w("Notification not shown: POST_NOTIFICATIONS is not granted.", tag = TAG)
            return
        }
        val tap = PendingIntent.getActivity(
            context,
            id,
            tapIntent(link),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val public = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.notification_small_icon)
            .setContentTitle(context.applicationInfo.loadLabel(context.packageManager))
            .setContentText(context.getString(R.string.notification_public_text))
            .build()
        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.notification_small_icon)
            .setContentTitle(title)
            .setContentText(body)
            // Without it a body longer than a line is cut off with no way to read the rest.
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .build()

        try {
            notifyChecked(id, notification)
        } catch (revoked: SecurityException) {
            // The permission can be withdrawn in the system's settings between the check and here.
            Log.w("Notification $id not shown", revoked, TAG)
        }
    }

    // hasPermission is the check; lint cannot follow it through a property.
    @SuppressLint("MissingPermission")
    private fun notifyChecked(id: Int, notification: Notification) = manager.notify(id, notification)

    fun cancel(id: Int) = manager.cancel(id)

    /**
     * Explicit, so no other app can receive it: the link opens in this app's own activity, which
     * checks it like any other link. Either way the task the app already has is brought forward, as
     * it was left, instead of a second copy being started.
     */
    private fun tapIntent(link: Uri?): Intent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().setPackage(context.packageName)
        if (link == null) return launch
        return Intent(Intent.ACTION_VIEW, link)
            .setPackage(context.packageName)
            .setComponent(launch.component)
            // The running activity takes the link in onNewIntent. Without CLEAR_TOP and SINGLE_TOP a
            // second instance would start on top of it, or on top of a browser tab the app opened.
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
    }

    private companion object {
        const val TAG = "Notifications"
    }
}
