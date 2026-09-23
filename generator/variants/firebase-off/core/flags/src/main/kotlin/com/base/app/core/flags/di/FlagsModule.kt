package com.base.app.core.flags.di

import com.base.app.core.flags.FeatureFlags
import com.base.app.core.flags.LocalFeatureFlags
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Flags read as declared, because no remote source is wired up in this project. */
@Module
@InstallIn(SingletonComponent::class)
object FlagsModule {

    @Provides
    @Singleton
    fun provideFeatureFlags(flags: LocalFeatureFlags): FeatureFlags = flags
}
