package com.base.app.core.network.di

// <opt:room>
import android.content.Context
// </opt:room>
import com.base.app.core.network.KtorNetworkClient
import com.base.app.core.network.auth.KtorTokenRefresher
import com.base.app.core.network.auth.TokenRefresher
import com.base.app.core.network.NetworkClient
import com.base.app.core.network.PassthroughUnwrapper
import com.base.app.core.network.ResponseUnwrapper
// <opt:room>
// These live in ResponseCache.kt, which only exists with the room feature.
import androidx.room.Room
import com.base.app.core.common.session.SessionScopedStore
import com.base.app.core.network.CachedResponseDao
import com.base.app.core.network.NetworkDatabase
import com.base.app.core.network.QueuedRequestDao
import com.base.app.core.network.RequestQueue
import com.base.app.core.network.ResponseCache
import com.base.app.core.network.RoomRequestQueue
import com.base.app.core.network.RoomResponseCache
// </opt:room>
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
// <opt:room>
import dagger.hilt.android.qualifiers.ApplicationContext
// </opt:room>
import dagger.hilt.components.SingletonComponent
// <opt:room>
import dagger.multibindings.IntoSet
// </opt:room>
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
    fun bindTokenRefresher(impl: KtorTokenRefresher): TokenRefresher

    /** The default assumes the payload is the response body. */
    @Binds
    fun bindResponseUnwrapper(impl: PassthroughUnwrapper): ResponseUnwrapper

    // <opt:room>
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
    // </opt:room>
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkProviders {

    @Provides
    @Singleton
    fun providePassthroughUnwrapper(): PassthroughUnwrapper = PassthroughUnwrapper()

    // <opt:room>
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
    // </opt:room>
}
