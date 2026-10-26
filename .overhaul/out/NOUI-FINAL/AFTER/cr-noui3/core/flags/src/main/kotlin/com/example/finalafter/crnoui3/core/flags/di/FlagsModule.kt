package com.example.finalafter.crnoui3.core.flags.di

import com.example.finalafter.crnoui3.core.common.session.SessionScopedStore
import com.example.finalafter.crnoui3.core.flags.FeatureFlags
import com.example.finalafter.crnoui3.core.flags.FlagOverrides
import com.example.finalafter.crnoui3.core.flags.remoteconfig.RemoteConfigFeatureFlags
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Binds where flag values come from. Switching vendors replaces the one binding below. */
@Module
@InstallIn(SingletonComponent::class)
internal interface FlagsModule {
    @Binds
    fun bindFeatureFlags(flags: RemoteConfigFeatureFlags): FeatureFlags

    @Binds
    @IntoSet
    fun bindFlagOverridesAsSessionScoped(overrides: FlagOverrides): SessionScopedStore
}
