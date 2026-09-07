package com.base.app.core.flags.di

import com.base.app.core.flags.FeatureFlags
import com.base.app.core.flags.RemoteConfigFeatureFlags
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds where flag values come from.
 *
 * This is the whole of the app's coupling to Remote Config. Moving to LaunchDarkly, Statsig or a
 * column in your own database replaces this file and touches nothing else.
 */
@Module
@InstallIn(SingletonComponent::class)
object FlagsModule {

    @Provides
    @Singleton
    fun provideFeatureFlags(flags: RemoteConfigFeatureFlags): FeatureFlags = flags
}
