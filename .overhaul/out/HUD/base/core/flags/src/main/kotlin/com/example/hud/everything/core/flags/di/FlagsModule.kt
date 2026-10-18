package com.example.hud.everything.core.flags.di

import com.example.hud.everything.core.common.session.SessionScopedStore
import com.example.hud.everything.core.flags.FeatureFlags
import com.example.hud.everything.core.flags.FlagOverrides
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import com.example.hud.everything.core.flags.remoteconfig.RemoteConfigFeatureFlags

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
