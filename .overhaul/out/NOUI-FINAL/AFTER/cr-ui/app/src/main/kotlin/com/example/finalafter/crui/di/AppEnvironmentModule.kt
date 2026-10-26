package com.example.finalafter.crui.di

import com.example.finalafter.crui.BuildConfig
import com.example.finalafter.crui.core.common.environment.AppEnvironment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * What this build is, for the dev tooling that dev-flavour and debug builds add. Its own module
 * because AppModule exists only with networking.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppEnvironmentModule {

    @Provides
    @Singleton
    fun provideAppEnvironment(): AppEnvironment = AppEnvironment(
        name = BuildConfig.ENVIRONMENT,
        versionName = BuildConfig.VERSION_NAME,
        applicationId = BuildConfig.APPLICATION_ID,
        apiBaseUrl = BuildConfig.API_BASE_URL,
        isDebugBuild = BuildConfig.DEBUG,
    )
}
