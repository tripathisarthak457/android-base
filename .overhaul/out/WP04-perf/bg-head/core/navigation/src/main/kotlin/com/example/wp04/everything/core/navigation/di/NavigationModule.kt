package com.example.wp04.everything.core.navigation.di

import com.example.wp04.everything.core.navigation.DeepLinkRoute
import com.example.wp04.everything.core.navigation.NavGraphEntry
import com.example.wp04.everything.core.navigation.NavRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.multibindings.Multibinds
import kotlinx.serialization.modules.SerializersModule

/**
 * Navigation lives in the activity-retained component, as in the Navigation 3 Hilt recipe: each
 * window has its own navigator, and the destinations a feature registers capture that window's
 * navigator, not one shared by every window of the process. ViewModels still inject it, since
 * their component is a child of this one.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
object NavigationModule {

    /** Assembled once per window from every feature's contribution. */
    @Provides
    @ActivityRetainedScoped
    fun provideNavRegistry(graphs: Set<@JvmSuppressWildcards NavGraphEntry>): NavRegistry =
        NavRegistry(graphs)
}

/** Declares the multibound sets so the build still compiles with no features installed. */
@Module
@InstallIn(ActivityRetainedComponent::class)
interface NavigationMultibindings {

    @Multibinds
    fun navGraphs(): Set<NavGraphEntry>

    @Multibinds
    fun navKeySerializers(): Set<SerializersModule>

    @Multibinds
    fun deepLinkRoutes(): Set<DeepLinkRoute>
}
