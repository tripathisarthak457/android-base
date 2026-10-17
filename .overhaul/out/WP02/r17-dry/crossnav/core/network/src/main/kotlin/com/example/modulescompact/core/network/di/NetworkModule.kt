package com.example.modulescompact.core.network.di

import com.example.modulescompact.core.common.network.NetworkMonitor
import com.example.modulescompact.core.network.ConnectivityNetworkMonitor
import com.example.modulescompact.core.network.KtorNetworkClient
import com.example.modulescompact.core.network.NetworkClient
import com.example.modulescompact.core.network.PassthroughUnwrapper
import com.example.modulescompact.core.network.ResponseUnwrapper
import com.example.modulescompact.core.network.auth.KtorTokenRefresher
import com.example.modulescompact.core.network.auth.TokenRefresher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/** Attaches the bearer token and refreshes it transparently on a 401. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

/**
 * No auth plugin at all. Used only by the token refresh call itself, which would otherwise recurse:
 * a 401 on the refresh endpoint would trigger a refresh, which would 401.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlainClient

@Module
@InstallIn(SingletonComponent::class)
interface NetworkBindings {

    @Binds
    fun bindNetworkClient(impl: KtorNetworkClient): NetworkClient

    @Binds
    fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    @Binds
    fun bindTokenRefresher(impl: KtorTokenRefresher): TokenRefresher

    /** The default assumes the payload is the response body. */
    @Binds
    fun bindResponseUnwrapper(impl: PassthroughUnwrapper): ResponseUnwrapper
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkProviders {

    @Provides
    @Singleton
    fun providePassthroughUnwrapper(): PassthroughUnwrapper = PassthroughUnwrapper()
}
