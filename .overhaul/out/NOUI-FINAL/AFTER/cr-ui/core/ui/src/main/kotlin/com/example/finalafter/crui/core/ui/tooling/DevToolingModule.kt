package com.example.finalafter.crui.core.ui.tooling

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface DevToolingModule {

    /** Bound by `:core:devtools` in the builds that depend on it, absent from every other. */
    @BindsOptionalOf
    fun devTooling(): DevTooling
}
