package com.base.app.push

import com.base.app.core.common.util.AppLogger
import com.base.app.core.notification.PushTokenSink
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Where a push token is sent.
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
    fun providePushTokenSink(): PushTokenSink = PushTokenSink { token ->
        AppLogger.d("FCM token (not yet uploaded): $token", tag = "Push")
    }
}
