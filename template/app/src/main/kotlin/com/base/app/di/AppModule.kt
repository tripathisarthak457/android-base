package com.base.app.di

import com.base.app.BuildConfig
// <opt:devtools>
import com.base.app.core.devtools.DevEnvironment
// </opt:devtools>
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
        // <opt:devtools>
        // Same rule as the badge: debug builds, and dev and staging releases. Never
        // production, where the recorder is not installed and no body is ever held.
        recordExchanges = devEnvironment().visible,
        // </opt:devtools>
    )

    // <opt:devtools>
    /**
     * What this build is, for the inspector.
     *
     * Read from `BuildConfig` here rather than in `:core:devtools`, for the same reason
     * `NetworkConfig` is: a library module reading its own `BuildConfig` reports whichever
     * variant compiled it, not the app.
     */
    @Provides
    @Singleton
    fun provideDevEnvironment(): DevEnvironment = devEnvironment()

    private fun devEnvironment() = DevEnvironment(
        name = BuildConfig.ENVIRONMENT,
        versionName = BuildConfig.VERSION_NAME,
        applicationId = BuildConfig.APPLICATION_ID,
        apiBaseUrl = BuildConfig.API_BASE_URL,
        isDebugBuild = BuildConfig.DEBUG,
        isShippable = BuildConfig.IS_SHIPPABLE,
    )
    // </opt:devtools>
}
