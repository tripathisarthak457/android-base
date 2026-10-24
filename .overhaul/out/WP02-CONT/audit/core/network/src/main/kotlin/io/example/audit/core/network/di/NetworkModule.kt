package io.example.audit.core.network.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import io.example.audit.core.common.network.NetworkMonitor
import io.example.audit.core.common.session.SessionScopedStore
import io.example.audit.core.network.auth.KtorTokenRefresher
import io.example.audit.core.network.auth.TokenRefresher
import io.example.audit.core.network.client.InFlightRequests
import io.example.audit.core.network.client.KtorNetworkClient
import io.example.audit.core.network.client.NetworkClient
import io.example.audit.core.network.client.NetworkConfig
import io.example.audit.core.network.client.RetryPolicy
import io.example.audit.core.network.monitor.ConnectivityNetworkMonitor
import io.example.audit.core.network.recording.NetworkObserver
import io.example.audit.core.network.response.PassthroughUnwrapper
import io.example.audit.core.network.response.ResponseUnwrapper
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
