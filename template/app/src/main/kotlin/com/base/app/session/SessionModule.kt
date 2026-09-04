package com.base.app.session

import com.base.app.core.common.session.SessionController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the session implementation to the interface features see.
 *
 * The one line that lets a settings screen in a feature module sign the user out without knowing
 * anything about stores, navigation, or the composition root.
 */
@Module
@InstallIn(SingletonComponent::class)
interface SessionModule {

    @Binds
    fun bindSessionController(impl: SessionCoordinator): SessionController
}
