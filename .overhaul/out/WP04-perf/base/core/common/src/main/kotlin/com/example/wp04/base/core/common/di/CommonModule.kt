package com.example.wp04.base.core.common.di

import com.example.wp04.base.core.common.network.ConnectivityNetworkMonitor
import com.example.wp04.base.core.common.network.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import com.example.wp04.base.core.common.session.SessionScopedStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CommonModule {
    @Binds
    @Singleton
    fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    /** Declares the set so it can be injected even when nothing has contributed to it yet. */
    @Multibinds
    fun sessionScopedStores(): Set<SessionScopedStore>
}
