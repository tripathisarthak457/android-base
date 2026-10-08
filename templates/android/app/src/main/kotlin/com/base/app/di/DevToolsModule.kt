package com.base.app.di

import com.base.app.BuildConfig
import com.base.app.core.devtools.DevEnvironment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** What this build is, for the inspector. Its own module because AppModule exists only with networking. */
@Module
@InstallIn(SingletonComponent::class)
object DevToolsModule {

    @Provides
    @Singleton
    fun provideDevEnvironment(): DevEnvironment = buildDevEnvironment()
}

internal fun buildDevEnvironment() = DevEnvironment(
    name = BuildConfig.ENVIRONMENT,
    versionName = BuildConfig.VERSION_NAME,
    applicationId = BuildConfig.APPLICATION_ID,
    apiBaseUrl = BuildConfig.API_BASE_URL,
    isDebugBuild = BuildConfig.DEBUG,
    isShippable = BuildConfig.IS_SHIPPABLE,
)
