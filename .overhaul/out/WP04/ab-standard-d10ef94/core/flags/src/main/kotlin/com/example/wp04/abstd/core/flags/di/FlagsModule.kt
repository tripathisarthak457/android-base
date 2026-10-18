package com.example.wp04.abstd.core.flags.di

import com.example.wp04.abstd.core.flags.FeatureFlags
import com.example.wp04.abstd.core.flags.LocalFeatureFlags
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
