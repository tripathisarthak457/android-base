package com.base.app.core.flags.di

import com.base.app.core.flags.FeatureFlags
import com.base.app.core.flags.RemoteConfigFeatureFlags
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds where flag values come from. Switching vendors replaces this file and nothing else. */
@Module
@InstallIn(SingletonComponent::class)
object FlagsModule {

    @Provides
    @Singleton
    fun provideFeatureFlags(flags: RemoteConfigFeatureFlags): FeatureFlags = flags
}
