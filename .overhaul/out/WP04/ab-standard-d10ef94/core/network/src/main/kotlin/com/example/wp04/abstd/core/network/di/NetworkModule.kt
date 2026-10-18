package com.example.wp04.abstd.core.network.di

import android.content.Context
import com.example.wp04.abstd.core.common.network.NetworkMonitor
import com.example.wp04.abstd.core.network.ConnectivityNetworkMonitor
import com.example.wp04.abstd.core.network.KtorNetworkClient
import com.example.wp04.abstd.core.network.auth.KtorTokenRefresher
import com.example.wp04.abstd.core.network.auth.TokenRefresher
import com.example.wp04.abstd.core.network.NetworkClient
import com.example.wp04.abstd.core.network.PassthroughUnwrapper
import com.example.wp04.abstd.core.network.ResponseUnwrapper
// These live in ResponseCache.kt, which only exists with the room feature.
import androidx.room.Room
import com.example.wp04.abstd.core.common.session.SessionScopedStore
import com.example.wp04.abstd.core.network.CachedResponseDao
import com.example.wp04.abstd.core.network.NetworkDatabase
import com.example.wp04.abstd.core.network.QueuedRequestDao
import com.example.wp04.abstd.core.network.RequestQueue
import com.example.wp04.abstd.core.network.ResponseCache
import com.example.wp04.abstd.core.network.RoomRequestQueue
import com.example.wp04.abstd.core.network.RoomResponseCache
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
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

    @Binds
    fun bindResponseCache(impl: RoomResponseCache): ResponseCache

    @Binds
    fun bindRequestQueue(impl: RoomRequestQueue): RequestQueue

    @Binds
    @IntoSet
    fun bindResponseCacheAsSessionScoped(impl: RoomResponseCache): SessionScopedStore

    @Binds
    @IntoSet
    fun bindRequestQueueAsSessionScoped(impl: RoomRequestQueue): SessionScopedStore
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkProviders {

    @Provides
    @Singleton
    fun providePassthroughUnwrapper(): PassthroughUnwrapper = PassthroughUnwrapper()

    @Provides
    @Singleton
    fun provideNetworkDatabase(@ApplicationContext context: Context): NetworkDatabase =
        Room.databaseBuilder(context, NetworkDatabase::class.java, NetworkDatabase.NAME)
            // Everything here is a cache or a replayable request, so dropping it on a schema change
            // is safe.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideCachedResponseDao(database: NetworkDatabase): CachedResponseDao =
        database.cachedResponseDao()

    @Provides
    fun provideQueuedRequestDao(database: NetworkDatabase): QueuedRequestDao =
        database.queuedRequestDao()
}
