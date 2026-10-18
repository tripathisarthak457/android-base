package com.base.app.core.devtools.di

import com.base.app.core.devtools.page.DevToolsPage
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
internal interface DevToolsPagesModule {

    /** Empty in a build whose dependencies contribute no page. */
    @Multibinds
    fun pages(): Set<DevToolsPage>
}
