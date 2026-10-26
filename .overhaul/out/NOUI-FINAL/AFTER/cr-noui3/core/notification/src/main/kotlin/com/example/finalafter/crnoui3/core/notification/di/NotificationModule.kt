package com.example.finalafter.crnoui3.core.notification.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.finalafter.crnoui3.core.common.log.Log
import com.example.finalafter.crnoui3.core.common.session.SessionScopedStore
import com.example.finalafter.crnoui3.core.datastore.DataStoreFiles
import com.example.finalafter.crnoui3.core.notification.push.PushRegistration
import com.example.finalafter.crnoui3.core.notification.push.PushStore
import com.example.finalafter.crnoui3.core.notification.push.PushTokenSink
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NotificationModule {

    @Binds
    @IntoSet
    fun bindPushRegistrationAsSessionScoped(impl: PushRegistration): SessionScopedStore

    companion object {

        @Provides
        @Singleton
        @PushStore
        fun providePushStore(files: DataStoreFiles): DataStore<Preferences> = files.preferences("push")

        /**
         * Where the installation id goes. This one only logs it: replace the body with a call into
         * the `:data:` module that owns device registration.
         */
        @Provides
        @Singleton
        fun providePushTokenSink(): PushTokenSink = PushTokenSink { installationId ->
            Log.d("FCM installation id (not uploaded): $installationId", tag = "Push")
        }
    }
}
