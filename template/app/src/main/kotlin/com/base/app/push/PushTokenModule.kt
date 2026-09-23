package com.base.app.push

import com.base.app.core.common.util.AppLogger
import com.base.app.core.notification.PushTokenSink
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Where this install's push address is sent: its Firebase Installation ID, which the server
 * targets with `Message.fid`.
 *
 * The default logs it and stops there, because a template cannot know your endpoint. Replace the
 * body with a call into whichever `:data:*` module owns device registration — that module is a
 * legitimate dependency of `:app`, and this is the seam that keeps `:core:notification` from
 * having to reach up into it.
 */
@Module
@InstallIn(SingletonComponent::class)
object PushTokenModule {

    @Provides
    @Singleton
    fun providePushTokenSink(): PushTokenSink = PushTokenSink { installationId ->
        AppLogger.d("FCM installation ID (not yet uploaded): $installationId", tag = "Push")
    }
}
