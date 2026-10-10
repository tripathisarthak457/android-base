package com.base.app.core.network.di

import com.base.app.core.common.network.NetworkMonitor
import com.base.app.core.common.session.SessionScopedStore
import com.base.app.core.network.auth.KtorTokenRefresher
import com.base.app.core.network.auth.TokenRefresher
import com.base.app.core.network.client.InFlightRequests
import com.base.app.core.network.client.KtorNetworkClient
import com.base.app.core.network.client.NetworkClient
import com.base.app.core.network.client.NetworkConfig
import com.base.app.core.network.client.RetryPolicy
import com.base.app.core.network.monitor.ConnectivityNetworkMonitor
import com.base.app.core.network.recording.NetworkObserver
import com.base.app.core.network.response.PassthroughUnwrapper
import com.base.app.core.network.response.ResponseUnwrapper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import javax.inject.Singleton
// <opt:websocket>
import com.base.app.core.network.websocket.AppWebSocket
// </opt:websocket>
// <opt:appstatus>
import com.base.app.core.network.appstatus.AppStatusRules
// </opt:appstatus>
// <opt:offlinequeue>
import com.base.app.core.network.queue.OfflineQueue
import com.base.app.core.network.queue.RequestQueue
// </opt:offlinequeue>

@Module
@InstallIn(SingletonComponent::class)
internal interface NetworkModule {

    @Binds
    fun bindNetworkClient(impl: KtorNetworkClient): NetworkClient

    @Binds
    fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    @Binds
    fun bindTokenRefresher(impl: KtorTokenRefresher): TokenRefresher

    /** Empty unless a build installs an inspector; nothing is recorded then. */
    @Multibinds
    fun networkObservers(): Set<NetworkObserver>

    @Binds
    @IntoSet
    fun bindInFlightRequestsAsSessionScoped(impl: InFlightRequests): SessionScopedStore
    // <opt:websocket>

    @Binds
    @IntoSet
    fun bindWebSocketAsSessionScoped(impl: AppWebSocket): SessionScopedStore
    // </opt:websocket>
    // <opt:offlinequeue>

    @Binds
    fun bindRequestQueue(impl: OfflineQueue): RequestQueue
    // </opt:offlinequeue>

    companion object {

        /**
         * The shape of the API's bodies, for every call the app makes. Replace it with an
         * `EnvelopeUnwrapper`, or one of your own, when the backend wraps its payloads.
         */
        @Provides
        @Singleton
        fun provideResponseUnwrapper(): ResponseUnwrapper = PassthroughUnwrapper()

        @Provides
        @Singleton
        fun provideRetryPolicy(config: NetworkConfig): RetryPolicy = RetryPolicy(maxRetries = config.maxRetries)
        // <opt:appstatus>

        /** HTTP's own signals. A backend that says "update" or "maintenance" with a code in its body lists the code here. */
        @Provides
        @Singleton
        fun provideAppStatusRules(): AppStatusRules = AppStatusRules()
        // </opt:appstatus>
    }
}
