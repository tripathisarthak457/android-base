package com.example.finalafter.crnoui.di

import android.os.Build
import com.example.finalafter.crnoui.BuildConfig
import com.example.finalafter.crnoui.core.network.client.NetworkConfig
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
        appVersionCode = BuildConfig.VERSION_CODE.toLong(),
    )
}
