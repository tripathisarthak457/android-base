package com.example.wp04.base.core.navigation.di

import com.example.wp04.base.core.navigation.NavGraphEntry
import com.example.wp04.base.core.navigation.NavRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlinx.serialization.modules.SerializersModule
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {
    /** Assembled once from every feature's contribution. */
    @Provides
    @Singleton
    fun provideNavRegistry(graphs: Set<@JvmSuppressWildcards NavGraphEntry>): NavRegistry =
        NavRegistry(graphs)
}

/** Declares both multibound sets so the build still compiles with no features installed. */
@Module
@InstallIn(SingletonComponent::class)
interface NavigationMultibindings {
    @Multibinds
    fun navGraphs(): Set<NavGraphEntry>

    @Multibinds
    fun navKeySerializers(): Set<SerializersModule>
}
