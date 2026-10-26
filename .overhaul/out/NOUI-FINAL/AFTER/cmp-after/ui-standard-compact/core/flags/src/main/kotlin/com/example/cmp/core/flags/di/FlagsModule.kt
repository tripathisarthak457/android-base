package com.example.cmp.core.flags.di

import com.example.cmp.core.common.session.SessionScopedStore
import com.example.cmp.core.flags.FeatureFlags
import com.example.cmp.core.flags.FlagOverrides
import com.example.cmp.core.flags.LocalFeatureFlags
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
    fun bindFeatureFlags(flags: LocalFeatureFlags): FeatureFlags

    @Binds
    @IntoSet
    fun bindFlagOverridesAsSessionScoped(overrides: FlagOverrides): SessionScopedStore
}
