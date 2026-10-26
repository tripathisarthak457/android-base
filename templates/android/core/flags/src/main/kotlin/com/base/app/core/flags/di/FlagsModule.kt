package com.base.app.core.flags.di

import com.base.app.core.common.session.SessionScopedStore
import com.base.app.core.flags.FeatureFlags
import com.base.app.core.flags.FlagOverrides
// <opt:!remoteconfig>import com.base.app.core.flags.LocalFeatureFlags
// <opt:remoteconfig>
import com.base.app.core.flags.remoteconfig.RemoteConfigFeatureFlags
// </opt:remoteconfig>
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Binds where flag values come from. Switching vendors replaces the one binding below. */
@Module
@InstallIn(SingletonComponent::class)
internal interface FlagsModule {
    // <opt:remoteconfig>

    @Binds
    fun bindFeatureFlags(flags: RemoteConfigFeatureFlags): FeatureFlags
    // </opt:remoteconfig>
    // <opt-else:remoteconfig>
    //~
    //~ @Binds
    //~ fun bindFeatureFlags(flags: LocalFeatureFlags): FeatureFlags
    // </opt-else:remoteconfig>

    @Binds
    @IntoSet
    fun bindFlagOverridesAsSessionScoped(overrides: FlagOverrides): SessionScopedStore
}
