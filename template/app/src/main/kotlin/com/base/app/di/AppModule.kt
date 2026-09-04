package com.base.app.di

import com.base.app.BuildConfig
import com.base.app.core.network.NetworkConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The one place environment configuration enters the object graph.
 *
 * `BuildConfig` exists only in this module — library modules deliberately have none, so that a
 * library cannot behave differently depending on which variant compiled it. The values come from
 * the product flavour (see `AppFlavor` in build-logic), which means switching environment is a
 * variant switch and nothing else: no code change, no rebuild of any library module.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig(
        baseUrl = BuildConfig.API_BASE_URL,
        webSocketUrl = BuildConfig.WEB_SOCKET_URL,
        isDebug = BuildConfig.DEBUG,
        // Blank until your backend has one. See NetworkConfig.refreshTokenPath: leaving it blank
        // disables the automatic 401-refresh flow entirely, which is correct for an API that does
        // not issue refresh tokens — the alternative is every expiry hitting an endpoint that
        // does not exist.
        refreshTokenPath = "",
    )
}
