package com.example.wp04.dlplay.push

import com.example.wp04.dlplay.core.common.log.Log
import com.example.wp04.dlplay.core.notification.PushTokenSink
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Where this install's push address is sent: its Firebase Installation ID, which the server targets
 * with `Message.fid`.
 *
 * The default only logs it. Replace the body with a call into the `:data:` module that owns device
 * registration.
 */
@Module
@InstallIn(SingletonComponent::class)
object PushTokenModule {

    @Provides
    @Singleton
    fun providePushTokenSink(): PushTokenSink = PushTokenSink { installationId ->
        Log.d("FCM installation ID (not yet uploaded): $installationId", tag = "Push")
    }
}
