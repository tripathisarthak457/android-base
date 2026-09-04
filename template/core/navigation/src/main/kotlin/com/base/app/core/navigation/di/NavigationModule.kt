package com.base.app.core.navigation.di

import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.NavRegistry
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

    /**
     * Assembled once from every feature's contribution.
     *
     * A `@Singleton` because building it validates the whole graph — duplicate keys fail here —
     * and because the map is read on every navigation.
     */
    @Provides
    @Singleton
    fun provideNavRegistry(graphs: Set<@JvmSuppressWildcards NavGraphEntry>): NavRegistry =
        NavRegistry(graphs)
}

/**
 * Declares both multibound sets so the build still compiles with no features installed.
 *
 * Without these, an app that has not yet added a feature fails at the injection site rather than
 * receiving an empty set — which would make the very first feature a strangely large change, and
 * make deleting the last one impossible.
 */
@Module
@InstallIn(SingletonComponent::class)
interface NavigationMultibindings {

    @Multibinds
    fun navGraphs(): Set<NavGraphEntry>

    @Multibinds
    fun navKeySerializers(): Set<SerializersModule>
}
