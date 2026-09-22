package com.base.app.core.flags.di

import com.base.app.core.flags.FeatureFlags
import com.base.app.core.flags.LocalFeatureFlags
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Flags read as declared, because no remote source is wired up in this project.
 *
 * Every `flags[Flag.Something]` call in feature code still compiles and still returns a sensible
 * answer. That is the point: the two sides of a flag can be built and reviewed before anybody has
 * decided which vendor will be turning it on, and adding one later is a change to this one file.
 */
@Module
@InstallIn(SingletonComponent::class)
object FlagsModule {

    @Provides
    @Singleton
    fun provideFeatureFlags(flags: LocalFeatureFlags): FeatureFlags = flags
}
