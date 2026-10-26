package com.example.finalafter.crui.core.flags.di

import com.example.finalafter.crui.core.common.session.SessionScopedStore
import com.example.finalafter.crui.core.flags.FeatureFlags
import com.example.finalafter.crui.core.flags.FlagOverrides
import com.example.finalafter.crui.core.flags.remoteconfig.RemoteConfigFeatureFlags
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
