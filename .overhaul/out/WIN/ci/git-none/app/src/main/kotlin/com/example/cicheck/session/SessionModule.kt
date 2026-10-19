package com.example.cicheck.session

import com.example.cicheck.core.common.session.SessionController
import com.example.cicheck.core.common.session.SessionScopedStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/** Binds the session implementation to the interface features see. */
@Module
@InstallIn(SingletonComponent::class)
interface SessionModule {

    @Binds
    fun bindSessionController(impl: SessionCoordinator): SessionController

    /** Declares the set so it can be injected even when nothing has contributed to it yet. */
    @Multibinds
    fun sessionScopedStores(): Set<SessionScopedStore>
}
