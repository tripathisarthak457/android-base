package com.example.wp04.base.session

import com.example.wp04.base.core.common.session.SessionController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the session implementation to the interface features see. */
@Module
@InstallIn(SingletonComponent::class)
interface SessionModule {
    @Binds
    fun bindSessionController(impl: SessionCoordinator): SessionController
}
