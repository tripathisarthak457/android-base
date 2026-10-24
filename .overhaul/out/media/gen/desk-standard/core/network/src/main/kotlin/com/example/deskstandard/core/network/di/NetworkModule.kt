package com.example.deskstandard.core.network.di

import com.example.deskstandard.core.common.network.NetworkMonitor
import com.example.deskstandard.core.common.session.SessionScopedStore
import com.example.deskstandard.core.network.auth.KtorTokenRefresher
import com.example.deskstandard.core.network.auth.TokenRefresher
import com.example.deskstandard.core.network.client.InFlightRequests
import com.example.deskstandard.core.network.client.KtorNetworkClient
import com.example.deskstandard.core.network.client.NetworkClient
import com.example.deskstandard.core.network.client.NetworkConfig
import com.example.deskstandard.core.network.client.RetryPolicy
import com.example.deskstandard.core.network.monitor.ConnectivityNetworkMonitor
import com.example.deskstandard.core.network.recording.NetworkObserver
import com.example.deskstandard.core.network.response.PassthroughUnwrapper
import com.example.deskstandard.core.network.response.ResponseUnwrapper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import javax.inject.Singleton

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
    }
}
