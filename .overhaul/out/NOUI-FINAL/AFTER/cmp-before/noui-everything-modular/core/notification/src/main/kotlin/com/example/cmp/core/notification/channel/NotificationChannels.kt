package com.example.cmp.core.notification.channel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import com.example.cmp.core.common.coroutines.AppDispatchers
import com.example.cmp.core.notification.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/** The channels this app posts to: what someone can mute, one by one, in the system's settings. */
enum class NotificationChannelSpec(
    val id: String,
    @param:StringRes val channelName: Int,
    @param:StringRes val description: Int,
    val importance: Int,
) {
    Default(
        id = "final_after_default",
        channelName = R.string.notification_channel_default,
        description = R.string.notification_channel_default_description,
        importance = NotificationManager.IMPORTANCE_DEFAULT,
    ),
    Urgent(
        id = "final_after_urgent",
        channelName = R.string.notification_channel_urgent,
        description = R.string.notification_channel_urgent_description,
        importance = NotificationManager.IMPORTANCE_HIGH,
    ),
    Promotions(
        id = "final_after_promotions",
        channelName = R.string.notification_channel_promotions,
        description = R.string.notification_channel_promotions_description,
        importance = NotificationManager.IMPORTANCE_LOW,
    ),
    Silent(
        id = "final_after_silent",
        channelName = R.string.notification_channel_silent,
        description = R.string.notification_channel_silent_description,
        importance = NotificationManager.IMPORTANCE_MIN,
    ),
}

@Singleton
class NotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) {
    /**
     * Creates or renames every channel. Run at each start, so a channel exists before the first
     * notification arrives and follows a change of language; off the main thread, because each
     * channel is a binder call.
     */
    suspend fun create() = withContext(dispatchers.io) {
        val manager = context.getSystemService<NotificationManager>() ?: return@withContext
        manager.createNotificationChannels(
            NotificationChannelSpec.entries.map { spec ->
                NotificationChannel(spec.id, context.getString(spec.channelName), spec.importance).apply {
                    description = context.getString(spec.description)
                }
            },
        )
    }
}
