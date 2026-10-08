package com.base.app.core.datastore.di

import com.base.app.core.common.session.SessionScopedStore
import com.base.app.core.datastore.AuthTokenStore
import com.base.app.core.datastore.DataStoreAuthTokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface StoreBindings {

    @Binds
    fun bindAuthTokenStore(impl: DataStoreAuthTokenStore): AuthTokenStore

    /**
     * Registered next to the binding it clears, so adding a session-scoped store and forgetting to
     * wipe it on sign-out is not possible.
     */
    @Binds
    @IntoSet
    fun bindAuthTokenStoreAsSessionScoped(impl: DataStoreAuthTokenStore): SessionScopedStore
}
