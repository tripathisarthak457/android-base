package com.base.app.di

import android.os.Build
import com.base.app.BuildConfig
import com.base.app.core.network.NetworkConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The one place environment configuration enters the object graph. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig(
        baseUrl = BuildConfig.API_BASE_URL,
        webSocketUrl = BuildConfig.WEB_SOCKET_URL,
        isDebug = BuildConfig.DEBUG,
        // Blank disables automatic 401 refresh. Set it once your backend issues refresh tokens.
        refreshTokenPath = "",
        userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} " +
            "(Android ${Build.VERSION.RELEASE}; ${Build.MANUFACTURER} ${Build.MODEL})",
        // <opt:devtools>
        // Same rule as the badge: debug builds, and dev and staging releases. Never
        // production, where the recorder is not installed and no body is ever held.
        recordExchanges = buildDevEnvironment().visible,
        // </opt:devtools>
    )
}
